package ai.specdd.idea.references

import ai.specdd.idea.SpecDDFileType
import com.intellij.lang.annotation.AnnotationHolder
import com.intellij.lang.annotation.Annotator
import com.intellij.lang.annotation.HighlightSeverity
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile

class SpecDDPathAnnotator(
    private val extractor: SpecDDPathReferenceExtractor = SpecDDPathReferenceExtractor(),
    private val resolver: SpecDDPathResolver = SpecDDPathResolver(),
) : Annotator {
    override fun annotate(element: PsiElement, holder: AnnotationHolder) {
        if (element !is PsiFile) return
        if (element.fileType !is SpecDDFileType) return

        val resolutionContext = element.pathResolutionContext() ?: return
        annotateText(element.text, resolutionContext, holder)
    }

    fun annotateText(text: String, resolutionContext: SpecDDPathResolutionContext, holder: AnnotationHolder) {
        for (candidate in extractor.extract(text)) {
            val resolution = resolver.resolve(candidate, resolutionContext)
            if (SpecDDPathResolutionStatus.UNRESOLVED != resolution.status) continue

            val builder = holder
                .newAnnotation(HighlightSeverity.WARNING, "SpecDD path '${candidate.text}' does not resolve.")
                .range(candidate.range)

            val quickFix = createFileQuickFix(candidate, resolutionContext)
            if (null != quickFix) {
                builder.withFix(quickFix)
            }

            builder
                .create()
        }
    }
}
