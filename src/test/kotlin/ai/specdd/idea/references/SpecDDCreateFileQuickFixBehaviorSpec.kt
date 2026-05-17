package ai.specdd.idea.references

import com.intellij.openapi.application.Application
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.util.TextRange
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import java.lang.reflect.InvocationHandler
import java.lang.reflect.Proxy
import java.nio.file.Files
import kotlin.io.path.*

class SpecDDCreateFileQuickFixBehaviorSpec : BehaviorSpec({
    given("a SpecDD create file quick fix") {
        `when`("an unresolved exact file path is inside the project") {
            then("it creates the missing file and parent directories") {
                val root = createTempDirectory()
                val fix = createFileQuickFix(candidate("nested/main.sdd"), context(root, root)).shouldNotBeNull()

                fix.text shouldBe "Create file 'nested/main.sdd'"
                fix.familyName shouldBe "Create SpecDD referenced path"
                fix.startInWriteAction() shouldBe false
                fix.isAvailable(project(root.toString()), null, null) shouldBe true

                fix.invoke(project(root.toString()), null, null)

                root.resolve("nested").isDirectory() shouldBe true
                root.resolve("nested/main.sdd").isRegularFile() shouldBe true
                fix.isAvailable(project(root.toString()), null, null) shouldBe false
            }
        }

        `when`("an unresolved exact directory path is inside the project") {
            then("it creates the missing directory") {
                val root = createTempDirectory()
                val fix = createFileQuickFix(candidate("generated"), context(root, root)).shouldNotBeNull()

                fix.text shouldBe "Create directory 'generated'"

                fix.invoke(project(root.toString()), null, null)

                root.resolve("generated").isDirectory() shouldBe true
            }
        }

        `when`("an unresolved relative path stays inside the project") {
            then("it creates the resolved project file") {
                val root = createTempDirectory()
                val specDirectory = root.resolve("specs")
                val fix =
                    createFileQuickFix(candidate("../sibling.sdd"), context(root, specDirectory)).shouldNotBeNull()

                fix.invoke(project(root.toString()), null, null)

                root.resolve("sibling.sdd").isRegularFile() shouldBe true
            }
        }

        `when`("the application runs write actions") {
            then("it creates the path from the write action") {
                val root = createTempDirectory()
                val previousApplication = ApplicationManager.getApplication()
                ApplicationManager.setApplication(applicationThatRunsWriteActions())
                val fix = createFileQuickFix(candidate("write-action.sdd"), context(root, root)).shouldNotBeNull()

                try {
                    fix.invoke(project(root.toString()), null, null)
                } finally {
                    ApplicationManager.setApplication(previousApplication)
                }

                root.resolve("write-action.sdd").isRegularFile() shouldBe true
            }
        }

        `when`("a target already exists") {
            then("it does not create a quick fix") {
                val root = createTempDirectory()
                root.resolve("main.sdd").createFile()

                createFileQuickFix(candidate("main.sdd"), context(root, root)).shouldBeNull()
            }
        }

        `when`("a candidate is a glob") {
            then("it does not create a quick fix") {
                val root = createTempDirectory()

                createFileQuickFix(candidate("fixtures/*.sdd"), context(root, root)).shouldBeNull()
            }
        }

        `when`("a relative path escapes the project") {
            then("it does not create a quick fix") {
                val root = createTempDirectory()

                createFileQuickFix(candidate("../outside.sdd"), context(root, root)).shouldBeNull()
            }
        }

        `when`("the spec directory is outside the project") {
            then("it does not create a quick fix") {
                val root = createTempDirectory()
                val outside = createTempDirectory()

                createFileQuickFix(candidate("main.sdd"), context(root, outside)).shouldBeNull()
            }
        }

        `when`("invoke is called for a no longer available target") {
            then("it leaves the existing file untouched") {
                val root = createTempDirectory()
                val target = root.resolve("main.sdd")
                val fix = createFileQuickFix(candidate("main.sdd"), context(root, root)).shouldNotBeNull()
                target.createFile()

                fix.invoke(project(root.toString()), null, null)

                Files.size(target) shouldBe 0L
                target.exists() shouldBe true
            }
        }
    }
})

private fun candidate(text: String): SpecDDPathCandidate =
    SpecDDPathCandidate(text, TextRange(0, text.length), true)

private fun context(root: java.nio.file.Path, specDirectory: java.nio.file.Path): SpecDDPathResolutionContext =
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
