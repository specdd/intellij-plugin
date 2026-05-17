package ai.specdd.idea.validation

import ai.specdd.idea.SpecDDFileType
import com.intellij.lang.annotation.AnnotationBuilder
import com.intellij.lang.annotation.AnnotationHolder
import com.intellij.lang.annotation.HighlightSeverity
import com.intellij.openapi.fileTypes.FileType
import com.intellij.openapi.fileTypes.PlainTextFileType
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import java.lang.reflect.InvocationHandler
import java.lang.reflect.Proxy

class SpecDDAnnotatorBehaviorSpec : BehaviorSpec({
    given("a SpecDD annotator") {
        `when`("plain text is annotated directly") {
            then("it creates error annotations for validation issues") {
                val annotationHolder = RecordingAnnotationHolder()

                SpecDDAnnotator().annotateText("Spec: Example\nPorpose:", annotationHolder.proxy)

                annotationHolder.errors.shouldContainExactly(
                    RecordedAnnotation(
                        range = TextRange(14, 21),
                        message = "Unknown SpecDD section 'Porpose'. Did you mean 'Purpose'?",
                    ),
                )
            }
        }

        `when`("a non-file PSI element is annotated") {
            then("it does not validate it") {
                val annotationHolder = RecordingAnnotationHolder()

                SpecDDAnnotator().annotate(psiElement(), annotationHolder.proxy)

                annotationHolder.errors shouldBe emptyList()
            }
        }

        `when`("a non-SpecDD PSI file is annotated") {
            then("it does not validate it") {
                val annotationHolder = RecordingAnnotationHolder()

                SpecDDAnnotator().annotate(psiFile("Porpose:", PlainTextFileType.INSTANCE), annotationHolder.proxy)

                annotationHolder.errors shouldBe emptyList()
            }
        }

        `when`("a SpecDD PSI file is annotated") {
            then("it validates the file text") {
                val annotationHolder = RecordingAnnotationHolder()

                SpecDDAnnotator().annotate(psiFile("Purpose:", SpecDDFileType()), annotationHolder.proxy)

                annotationHolder.errors.shouldContainExactly(
                    RecordedAnnotation(
                        range = TextRange(0, 7),
                        message = "SpecDD files should start with the Spec section. Did you mean 'Spec'?",
                    ),
                )
            }
        }
    }
})

private data class RecordedAnnotation(
    val range: TextRange,
    val message: String,
)

private class RecordingAnnotationHolder {
    val errors = mutableListOf<RecordedAnnotation>()
    private var currentSeverity: HighlightSeverity? = null
    private var currentMessage: String? = null

    val proxy: AnnotationHolder = Proxy.newProxyInstance(
        AnnotationHolder::class.java.classLoader,
        arrayOf(AnnotationHolder::class.java),
        InvocationHandler { _, method, args ->
            if ("newAnnotation" == method.name && args?.size == 2) {
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
                        if (HighlightSeverity.ERROR == currentSeverity && args?.firstOrNull() is TextRange) {
                            errors.add(RecordedAnnotation(args.first() as TextRange, currentMessage ?: ""))
                        }
                        proxy
                    }

                    "create" -> null
                    else -> proxy
                }
            },
        ) as AnnotationBuilder
}

private fun psiElement(): PsiElement =
    Proxy.newProxyInstance(
        PsiElement::class.java.classLoader,
        arrayOf(PsiElement::class.java),
        InvocationHandler { _, method, _ ->
            error("Unexpected PsiElement.${method.name} call")
        },
    ) as PsiElement

private fun psiFile(text: String, fileType: FileType): PsiFile =
    Proxy.newProxyInstance(
        PsiFile::class.java.classLoader,
        arrayOf(PsiFile::class.java),
        InvocationHandler { _, method, _ ->
            when (method.name) {
                "getFileType" -> fileType
                "getText" -> text
                else -> error("Unexpected PsiFile.${method.name} call")
            }
        },
    ) as PsiFile
