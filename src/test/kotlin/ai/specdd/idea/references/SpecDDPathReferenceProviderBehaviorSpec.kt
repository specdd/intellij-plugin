package ai.specdd.idea.references

import ai.specdd.idea.SpecDDFileType
import ai.specdd.idea.directory
import ai.specdd.idea.file
import ai.specdd.idea.testVirtualRoot
import com.intellij.openapi.fileTypes.FileType
import com.intellij.openapi.fileTypes.PlainTextFileType
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.TextRange
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.util.ProcessingContext
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import java.lang.reflect.InvocationHandler
import java.lang.reflect.Proxy

class SpecDDPathReferenceProviderBehaviorSpec : BehaviorSpec({
    given("a SpecDD path reference provider") {
        val provider = SpecDDPathReferenceProvider()

        `when`("references are requested for a SpecDD element") {
            then("it returns path references within the element range") {
                val root = testVirtualRoot()
                val text = "References:\n  ./main.sdd\n  ./src/*"
                val file = psiFile(text, SpecDDFileType(), root.file("app.sdd"))
                val element = psiElement(text, file, TextRange(0, text.length))

                val references = provider.getReferencesByElement(element, ProcessingContext())

                references.map { reference -> reference.canonicalText }.shouldContainExactly("./main.sdd", "./src/*")
                references.map { reference -> reference.rangeInElement }
                    .shouldContainExactly(TextRange(14, 24), TextRange(27, 34))
            }
        }

        `when`("references are requested for backticked paths and explicit symbols") {
            then("it returns references for inner path text and explicit symbol text") {
                val root = testVirtualRoot()
                val text = "References:\n  `./main.sdd` and `load(\"./src/app.kt\")` and `@FetchClient`"
                val file = psiFile(text, SpecDDFileType(), root.file("app.sdd"))
                val element = psiElement(text, file, TextRange(0, text.length))

                val references = provider.getReferencesByElement(element, ProcessingContext())

                references.map { reference -> reference.canonicalText }
                    .shouldContainExactly("./main.sdd", "./src/app.kt", "FetchClient")
                references.map { reference -> reference.rangeInElement }
                    .shouldContainExactly(
                        TextRange(text.indexOf("./main.sdd"), text.indexOf("./main.sdd") + "./main.sdd".length),
                        TextRange(text.indexOf("./src/app.kt"), text.indexOf("./src/app.kt") + "./src/app.kt".length),
                        TextRange(text.indexOf("@FetchClient"), text.indexOf("@FetchClient") + "@FetchClient".length),
                    )
            }
        }

        `when`("references are requested for backticked non-path code") {
            then("it does not return a symbol reference") {
                val root = testVirtualRoot()
                val text = "References:\n  `FetchClient`"
                val file = psiFile(text, SpecDDFileType(), root.file("app.sdd"))
                val element = psiElement(text, file, TextRange(0, text.length))

                provider.getReferencesByElement(element, ProcessingContext()).shouldContainExactly()
            }
        }

        `when`("references are requested for a non-SpecDD file") {
            then("it returns no references") {
                val text = "References:\n  main.sdd"
                val file = psiFile(text, PlainTextFileType.INSTANCE, null)
                val element = psiElement(text, file, TextRange(0, text.length))

                provider.getReferencesByElement(element, ProcessingContext()) shouldBe emptyArray()
            }
        }

        `when`("references are outside the current element range") {
            then("it skips them") {
                val root = testVirtualRoot()
                val text = "References:\n  ./main.sdd\n  ./src/*"
                val file = psiFile(text, SpecDDFileType(), root.file("app.sdd"))
                val element = psiElement(text, file, TextRange(0, 15))

                provider.getReferencesByElement(element, ProcessingContext())
                    .map { reference -> reference.canonicalText }
                    .shouldContainExactly()
            }
        }

        `when`("no project root is available") {
            then("it returns no references") {
                val text = "References:\n  main.sdd"
                val file = psiFile(text, SpecDDFileType(), null)
                val element = psiElement(text, file, TextRange(0, text.length))

                provider.getReferencesByElement(element, ProcessingContext()) shouldBe emptyArray()
            }
        }

        `when`("a reference is requested at an offset inside a path candidate") {
            then("it returns that path reference") {
                val root = testVirtualRoot()
                val text = "References:\n  ./main.sdd\n  ./src/*"
                val file = psiFile(text, SpecDDFileType(), root.file("app.sdd"))

                val reference = provider.getReferenceAt(file, text.indexOf("./main.sdd") + 2)

                reference?.canonicalText shouldBe "./main.sdd"
                reference?.rangeInElement shouldBe TextRange(14, 24)
            }
        }

        `when`("a reference is requested at an offset inside an explicit symbol") {
            then("it returns that symbol reference") {
                val root = testVirtualRoot()
                val text = "References:\n  @FetchClient"
                val file = psiFile(text, SpecDDFileType(), root.file("app.sdd"))

                val reference = provider.getReferenceAt(file, text.indexOf("FetchClient") + 2)

                reference?.canonicalText shouldBe "FetchClient"
                reference?.rangeInElement shouldBe
                        TextRange(text.indexOf("@FetchClient"), text.indexOf("@FetchClient") + "@FetchClient".length)
            }
        }

        `when`("a reference is requested at an offset inside a backticked path") {
            then("it returns that path reference") {
                val root = testVirtualRoot()
                val text = "References:\n  `./main.sdd`"
                val file = psiFile(text, SpecDDFileType(), root.file("app.sdd"))

                val reference = provider.getReferenceAt(file, text.indexOf("./main.sdd") + 2)

                reference?.canonicalText shouldBe "./main.sdd"
                reference?.rangeInElement shouldBe TextRange(15, 25)
            }
        }

        `when`("the project root comes from an IntelliJ project metadata file") {
            then("absolute project paths resolve from that project root") {
                val workspace = testVirtualRoot("workspace")
                val projectRoot = workspace.directory("repo")
                val specFile = projectRoot.directory("specs").file("app.sdd")
                val projectFile = projectRoot.directory(".idea").file("workspace.xml")
                projectRoot.file("target.sdd")
                val text = "References:\n  /target.sdd"
                val file = psiFile(
                    text = text,
                    fileType = SpecDDFileType(),
                    virtualFile = specFile,
                    project = psiProject(projectFile = projectFile),
                )

                val reference = provider.getReferenceAt(file, text.indexOf("/target.sdd") + 1)

                reference?.canonicalText shouldBe "/target.sdd"
            }
        }

        `when`("the project root comes from alternate IntelliJ project metadata shapes") {
            then("it accepts directory and ipr-style project metadata roots") {
                val workspace = testVirtualRoot("workspace")
                val directoryProjectRoot = workspace.directory("directory-project")
                val iprProjectRoot = workspace.directory("ipr-project")
                val text = "References:\n  /target.sdd"

                listOf(
                    directoryProjectRoot to directoryProjectRoot,
                    iprProjectRoot to iprProjectRoot.file("project.ipr"),
                ).forEach { (projectRoot, projectFile) ->
                    val specFile = projectRoot.directory("specs").file("app.sdd")
                    projectRoot.file("target.sdd")
                    val file = psiFile(
                        text = text,
                        fileType = SpecDDFileType(),
                        virtualFile = specFile,
                        project = psiProject(projectFile = projectFile),
                    )

                    provider.getReferenceAt(file, text.indexOf("/target.sdd") + 1)?.canonicalText shouldBe
                            "/target.sdd"
                }
            }
        }

        `when`("a reference is requested at an offset outside path candidates") {
            then("it returns no reference") {
                val root = testVirtualRoot()
                val text = "References:\n  main.sdd and FetchClient"
                val file = psiFile(text, SpecDDFileType(), root.file("app.sdd"))

                provider.getReferenceAt(file, text.indexOf("References")).shouldBe(null)
                provider.getReferenceAt(file, text.indexOf("FetchClient")).shouldBe(null)
            }
        }
    }
})

private fun psiFile(
    text: String,
    fileType: FileType,
    virtualFile: VirtualFile?,
    project: Project = project(virtualFile?.path),
): PsiFile =
    Proxy.newProxyInstance(
        PsiFile::class.java.classLoader,
        arrayOf(PsiFile::class.java),
        InvocationHandler { proxy, method, _ ->
            when (method.name) {
                "getText" -> text
                "getTextRange" -> TextRange(0, text.length)
                "getFileType" -> fileType
                "getProject" -> project
                "getContainingFile" -> proxy
                "getVirtualFile" -> virtualFile
                else -> null
            }
        },
    ) as PsiFile

private fun psiElement(text: String, file: PsiFile, textRange: TextRange): PsiElement =
    Proxy.newProxyInstance(
        PsiElement::class.java.classLoader,
        arrayOf(PsiElement::class.java),
        InvocationHandler { _, method, _ ->
            when (method.name) {
                "getText" -> text
                "getTextRange" -> textRange
                "getContainingFile" -> file
                "getProject" -> file.project
                "isValid" -> true
                else -> null
            }
        },
    ) as PsiElement

private fun psiProject(projectFile: VirtualFile? = null, workspaceFile: VirtualFile? = null): Project =
    Proxy.newProxyInstance(
        Project::class.java.classLoader,
        arrayOf(Project::class.java),
        InvocationHandler { _, method, _ ->
            when (method.name) {
                "getBasePath" -> null
                "getProjectFile" -> projectFile
                "getWorkspaceFile" -> workspaceFile
                "isDisposed" -> false
                else -> null
            }
        },
    ) as Project
