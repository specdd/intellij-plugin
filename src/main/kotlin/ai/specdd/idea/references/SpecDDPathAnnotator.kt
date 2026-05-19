package ai.specdd.idea.references

import ai.specdd.idea.SpecDDFileType
import com.intellij.lang.annotation.AnnotationHolder
import com.intellij.lang.annotation.Annotator
import com.intellij.lang.annotation.HighlightSeverity
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile

class SpecDDPathAnnotator(
    private val extractor: SpecDDPathReferenceExtractor = SpecDDPathReferenceExtractor(),
    private val resolver: SpecDDPathResolver = SpecDDPathResolver(),
    private val symbolExtractor: SpecDDSymbolReferenceExtractor = SpecDDSymbolReferenceExtractor(),
    private val symbolResolver: SpecDDSymbolResolver = SpecDDSymbolResolver(),
) : Annotator {
    override fun annotate(element: PsiElement, holder: AnnotationHolder) {
        if (element !is PsiFile) return
        if (element.fileType !is SpecDDFileType) return

        val resolutionContext = element.pathResolutionContext() ?: return
        annotateText(element.text, resolutionContext, holder, element.project)
    }

    fun annotateText(
        text: String,
        resolutionContext: SpecDDPathResolutionContext,
        holder: AnnotationHolder,
        project: Project? = null,
    ) {
        for (candidate in extractor.extract(text)) {
            if (!candidate.warnIfUnresolved) continue

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

        if (null == project) return

        for (candidate in symbolExtractor.extract(text)) {
            if (symbolResolver.resolve(candidate.text, project).isNotEmpty()) continue

            holder
                .newAnnotation(HighlightSeverity.WARNING, "SpecDD symbol '@${candidate.text}' does not resolve.")
                .range(candidate.range)
                .create()
        }
    }
}
