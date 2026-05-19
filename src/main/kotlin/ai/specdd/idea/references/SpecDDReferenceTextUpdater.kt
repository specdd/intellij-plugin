package ai.specdd.idea.references

import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile

internal fun updateReferenceText(
    sourceElement: PsiElement,
    rangeInElement: TextRange,
    replacementText: String,
): PsiElement {
    val containingFile = (sourceElement as? PsiFile) ?: sourceElement.containingFile ?: return sourceElement
    val elementRange = sourceElement.textRange ?: return sourceElement
    val documentManager = PsiDocumentManager.getInstance(sourceElement.project)
    val document = documentManager.getDocument(containingFile) ?: return sourceElement
    val replacementRange = rangeInElement.shiftRight(elementRange.startOffset)

    document.replaceString(replacementRange.startOffset, replacementRange.endOffset, replacementText)
    documentManager.commitDocument(document)

    return sourceElement
}
