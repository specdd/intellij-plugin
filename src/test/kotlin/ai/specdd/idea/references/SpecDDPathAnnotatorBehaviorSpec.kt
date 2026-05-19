package ai.specdd.idea.references

import ai.specdd.idea.directory
import ai.specdd.idea.testVirtualRoot
import ai.specdd.idea.SpecDDFileType
import com.intellij.codeInsight.intention.IntentionAction
import com.intellij.lang.annotation.AnnotationBuilder
import com.intellij.lang.annotation.AnnotationHolder
import com.intellij.lang.annotation.HighlightSeverity
import com.intellij.openapi.fileTypes.PlainTextFileType
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.TextRange
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiFile
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import java.lang.reflect.InvocationHandler
import java.lang.reflect.Proxy
import kotlin.io.path.createTempDirectory

class SpecDDPathAnnotatorBehaviorSpec : BehaviorSpec({
    given("a SpecDD path annotator") {
        `when`("path references are annotated directly") {
            then("it reports warning annotations only for unresolved path-like candidates") {
                val root = testVirtualRoot()
                root.directory("src")
                val holder = RecordingPathAnnotationHolder()
                val text = """
                    |Structure:
                    |  src: Existing directory
                    |  ./missing/file.sdd
                    |  Missing prose
                    """.trimMargin()

                SpecDDPathAnnotator().annotateText(text, SpecDDPathResolutionContext(root, root), holder.proxy)

                holder.warnings.shouldContainExactly(
                    RecordedPathAnnotation(
                        range = TextRange(text.indexOf("./missing/file.sdd"), text.indexOf("./missing/file.sdd") + 18),
                        message = "SpecDD path './missing/file.sdd' does not resolve.",
                    ),
                )
            }
        }

        `when`("path sections contain prose and URLs") {
            then("it does not report unresolved file warnings for them") {
                val root = testVirtualRoot()
                val holder = RecordingPathAnnotationHolder()
                val text = """
                    |Owns:
                    |  Plugin metadata and icons
                    |References:
                    |  https://github.com/specdd/intellij-plugin
                    |Purpose:
                    |  See docs/readme.md
                    """.trimMargin()

                SpecDDPathAnnotator().annotateText(text, SpecDDPathResolutionContext(root, root), holder.proxy)

                holder.warnings shouldBe emptyList()
                holder.fixTexts shouldBe emptyList()
            }
        }

        `when`("a non-file PSI element is annotated") {
            then("it does nothing") {
                val holder = RecordingPathAnnotationHolder()

                SpecDDPathAnnotator().annotate(psiElement("src/main"), holder.proxy)

                holder.warnings shouldBe emptyList()
            }
        }

        `when`("a non-SpecDD PSI file is annotated") {
            then("it does nothing") {
                val holder = RecordingPathAnnotationHolder()

                SpecDDPathAnnotator().annotate(psiFile("References:\n  missing.sdd", false), holder.proxy)

                holder.warnings shouldBe emptyList()
            }
        }

        `when`("a SpecDD PSI file has no path resolution context") {
            then("it does nothing") {
                val holder = RecordingPathAnnotationHolder()

                SpecDDPathAnnotator().annotate(psiFile("References:\n  missing.sdd", true), holder.proxy)

                holder.warnings shouldBe emptyList()
            }
        }

        `when`("a SpecDD PSI file has a path resolution context") {
            then("it annotates unresolved path candidates with create-path fixes") {
                val root = testVirtualRoot()
                val holder = RecordingPathAnnotationHolder()
                val text = "References:\n  ./missing.sdd"

                SpecDDPathAnnotator().annotate(psiFile(text, true, root), holder.proxy)

                holder.warnings.shouldContainExactly(
                    RecordedPathAnnotation(
                        range = TextRange(text.indexOf("./missing.sdd"), text.length),
                        message = "SpecDD path './missing.sdd' does not resolve.",
                    ),
                )
                holder.fixTexts.shouldContainExactly("Create file './missing.sdd'")
            }
        }
    }
})

private data class RecordedPathAnnotation(
    val range: TextRange,
    val message: String,
)

private class RecordingPathAnnotationHolder {
    val warnings = mutableListOf<RecordedPathAnnotation>()
    val fixTexts = mutableListOf<String>()
    private var currentSeverity: HighlightSeverity? = null
    private var currentMessage: String? = null

    val proxy: AnnotationHolder = Proxy.newProxyInstance(
        AnnotationHolder::class.java.classLoader,
        arrayOf(AnnotationHolder::class.java),
        InvocationHandler { _, method, args ->
            if ("newAnnotation" == method.name && 2 == args?.size) {
                currentSeverity = args[0] as HighlightSeverity
                currentMessage = args[1] as String
                return@InvocationHandler annotationBuilder()
            }
            null
        },
    ) as AnnotationHolder

    private fun annotationBuilder(): AnnotationBuilder =
        Proxy.newProxyInstance(
            AnnotationBuilder::class.java.classLoader,
            arrayOf(AnnotationBuilder::class.java),
            InvocationHandler { proxy, method, args ->
                when (method.name) {
                    "range" -> {
                        if (HighlightSeverity.WARNING == currentSeverity && args?.firstOrNull() is TextRange) {
                            warnings.add(RecordedPathAnnotation(args.first() as TextRange, currentMessage ?: ""))
                        }
                        proxy
                    }

                    "withFix" -> {
                        val action = args?.firstOrNull() as? IntentionAction
                        if (null != action) {
                            fixTexts.add(action.text)
                        }
                        proxy
                    }

                    "create" -> null
                    else -> proxy
                }
            },
        ) as AnnotationBuilder
}

private fun psiFile(text: String, specDD: Boolean, virtualFile: VirtualFile? = null): PsiFile =
    Proxy.newProxyInstance(
        PsiFile::class.java.classLoader,
        arrayOf(PsiFile::class.java),
        InvocationHandler { _, method, _ ->
            when (method.name) {
                "getFileType" -> if (specDD) SpecDDFileType() else PlainTextFileType.INSTANCE
                "getText" -> text
                "getProject" -> project(virtualFile?.path)
                "getVirtualFile" -> virtualFile
                "getContainingFile" -> null
                else -> null
            }
        },
    ) as PsiFile

internal fun project(basePath: String?): Project =
    Proxy.newProxyInstance(
        Project::class.java.classLoader,
        arrayOf(Project::class.java),
        InvocationHandler { _, method, _ ->
            when (method.name) {
                "getBasePath" -> basePath
                "isDisposed" -> false
                else -> null
            }
        },
    ) as Project
