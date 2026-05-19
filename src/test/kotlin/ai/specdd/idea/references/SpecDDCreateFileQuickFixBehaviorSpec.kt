package ai.specdd.idea.references

import ai.specdd.idea.directory
import ai.specdd.idea.exists
import ai.specdd.idea.file
import ai.specdd.idea.resolve
import ai.specdd.idea.testVirtualRoot
import com.intellij.openapi.application.Application
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.util.TextRange
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import java.lang.reflect.InvocationHandler
import java.lang.reflect.Proxy

class SpecDDCreateFileQuickFixBehaviorSpec : BehaviorSpec({
    given("a SpecDD create file quick fix") {
        `when`("an unresolved exact file path is inside the project") {
            then("it creates the missing file and parent directories") {
                val root = testVirtualRoot()
                val fix = createFileQuickFix(candidate("nested/main.sdd"), context(root, root)).shouldNotBeNull()

                fix.text shouldBe "Create file 'nested/main.sdd'"
                fix.familyName shouldBe "Create SpecDD referenced path"
                fix.startInWriteAction() shouldBe false
                fix.isAvailable(project(root.path), null, null) shouldBe true

                fix.invoke(project(root.path), null, null)

                root.resolve("nested")?.isDirectory shouldBe true
                root.resolve("nested/main.sdd")?.isDirectory shouldBe false
                fix.isAvailable(project(root.path), null, null) shouldBe false
            }
        }

        `when`("an unresolved exact directory path is inside the project") {
            then("it creates the missing directory") {
                val root = testVirtualRoot()
                val fix = createFileQuickFix(candidate("generated"), context(root, root)).shouldNotBeNull()

                fix.text shouldBe "Create directory 'generated'"

                fix.invoke(project(root.path), null, null)

                root.resolve("generated")?.isDirectory shouldBe true
            }
        }

        `when`("an unresolved relative path stays inside the project") {
            then("it creates the resolved project file") {
                val root = testVirtualRoot()
                val specDirectory = root.directory("specs")
                val fix =
                    createFileQuickFix(candidate("../sibling.sdd"), context(root, specDirectory)).shouldNotBeNull()

                fix.invoke(project(root.path), null, null)

                root.resolve("sibling.sdd")?.isDirectory shouldBe false
            }
        }

        `when`("an unresolved exact path has existing parent directories") {
            then("it reuses them while creating the missing file") {
                val root = testVirtualRoot()
                root.directory("nested")
                val fix = createFileQuickFix(candidate("nested/main.sdd"), context(root, root)).shouldNotBeNull()

                fix.invoke(project(root.path), null, null)

                root.resolve("nested/main.sdd")?.isDirectory shouldBe false
            }
        }

        `when`("the application runs write actions") {
            then("it creates the path from the write action") {
                val root = testVirtualRoot()
                val previousApplication = ApplicationManager.getApplication()
                ApplicationManager.setApplication(applicationThatRunsWriteActions())
                val fix = createFileQuickFix(candidate("write-action.sdd"), context(root, root)).shouldNotBeNull()

                try {
                    fix.invoke(project(root.path), null, null)
                } finally {
                    ApplicationManager.setApplication(previousApplication)
                }

                root.resolve("write-action.sdd")?.isDirectory shouldBe false
            }
        }

        `when`("a target already exists") {
            then("it does not create a quick fix") {
                val root = testVirtualRoot()
                root.file("main.sdd")

                createFileQuickFix(candidate("main.sdd"), context(root, root)).shouldBeNull()
            }
        }

        `when`("an intermediate path segment is an existing file") {
            then("it does not create a quick fix") {
                val root = testVirtualRoot()
                root.file("nested")

                createFileQuickFix(candidate("nested/main.sdd"), context(root, root)).shouldBeNull()
            }
        }

        `when`("a candidate is a glob") {
            then("it does not create a quick fix") {
                val root = testVirtualRoot()

                createFileQuickFix(candidate("fixtures/*.sdd"), context(root, root)).shouldBeNull()
            }
        }

        `when`("a relative path escapes the project") {
            then("it does not create a quick fix") {
                val root = testVirtualRoot()

                createFileQuickFix(candidate("../outside.sdd"), context(root, root)).shouldBeNull()
            }
        }

        `when`("the spec directory is outside the project") {
            then("it does not create a quick fix") {
                val root = testVirtualRoot()
                val outside = testVirtualRoot("outside")

                createFileQuickFix(candidate("main.sdd"), context(root, outside)).shouldBeNull()
            }
        }

        `when`("invoke is called for a no longer available target") {
            then("it leaves the existing file untouched") {
                val root = testVirtualRoot()
                val fix = createFileQuickFix(candidate("main.sdd"), context(root, root)).shouldNotBeNull()
                root.file("main.sdd")

                fix.invoke(project(root.path), null, null)

                root.resolve("main.sdd")?.length shouldBe 0L
                root.exists("main.sdd") shouldBe true
            }
        }
    }
})

private fun candidate(text: String): SpecDDPathCandidate =
    SpecDDPathCandidate(text, TextRange(0, text.length), true)

private fun context(root: com.intellij.openapi.vfs.VirtualFile, specDirectory: com.intellij.openapi.vfs.VirtualFile): SpecDDPathResolutionContext =
    SpecDDPathResolutionContext(root, specDirectory)

private fun applicationThatRunsWriteActions(): Application =
    Proxy.newProxyInstance(
        Application::class.java.classLoader,
        arrayOf(Application::class.java),
        InvocationHandler { _, method, args ->
            when (method.name) {
                "runWriteAction" -> {
                    (args?.firstOrNull() as? Runnable)?.run()
                    null
                }

                "isUnitTestMode" -> true
                "isHeadlessEnvironment" -> true
                else -> {
                    if (Boolean::class.javaPrimitiveType == method.returnType) false else null
                }
            }
        },
    ) as Application
