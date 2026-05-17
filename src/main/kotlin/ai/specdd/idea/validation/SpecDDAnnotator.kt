package ai.specdd.idea.validation

import ai.specdd.idea.SpecDDFileType
import com.intellij.lang.annotation.AnnotationHolder
import com.intellij.lang.annotation.Annotator
import com.intellij.lang.annotation.HighlightSeverity
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile

class SpecDDAnnotator(
    private val validator: SpecDDStructureValidator = SpecDDStructureValidator(),
) : Annotator {
    override fun annotate(element: PsiElement, holder: AnnotationHolder) {
        if (element !is PsiFile) return
        if (element.fileType !is SpecDDFileType) return

        annotateText(element.text, holder)
    }

    fun annotateText(text: String, holder: AnnotationHolder) {
        for (issue in validator.validate(text).issues) {
            holder
                .newAnnotation(HighlightSeverity.ERROR, issue.displayMessage)
                .range(issue.range)
                .create()
        }
    }
}
