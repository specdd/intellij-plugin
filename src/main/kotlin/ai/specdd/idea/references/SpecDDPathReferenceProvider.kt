package ai.specdd.idea.references

import ai.specdd.idea.SpecDDFileType
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiReference
import com.intellij.psi.PsiReferenceProvider
import com.intellij.util.ProcessingContext
import java.nio.file.Path

class SpecDDPathReferenceProvider(
    private val extractor: SpecDDPathReferenceExtractor = SpecDDPathReferenceExtractor(),
) : PsiReferenceProvider() {
    override fun getReferencesByElement(element: PsiElement, context: ProcessingContext): Array<PsiReference> {
        val containingFile = (element as? PsiFile) ?: element.containingFile ?: return PsiReference.EMPTY_ARRAY
        if (containingFile.fileType !is SpecDDFileType) return PsiReference.EMPTY_ARRAY

        val resolutionContext = containingFile.pathResolutionContext() ?: return PsiReference.EMPTY_ARRAY
        val elementRange = element.textRange ?: return PsiReference.EMPTY_ARRAY

        return extractor
            .extract(containingFile.text)
            .filter { candidate ->
                elementRange.startOffset <= candidate.range.startOffset &&
                        candidate.range.endOffset <= elementRange.endOffset
            }
            .map { candidate ->
                SpecDDPathReference(
                    element = element,
                    rangeInElement = candidate.range.shiftLeft(elementRange.startOffset),
                    candidate = candidate,
                    context = resolutionContext,
                )
            }
            .toTypedArray()
    }

    fun getReferenceAt(element: PsiElement, offsetInFile: Int): PsiReference? {
        val containingFile = (element as? PsiFile) ?: element.containingFile ?: return null
        if (containingFile.fileType !is SpecDDFileType) return null

        val resolutionContext = containingFile.pathResolutionContext() ?: return null
        val elementRange = element.textRange ?: return null
        val candidate = extractor
            .extract(containingFile.text)
            .firstOrNull { candidate ->
                candidate.range.startOffset <= offsetInFile &&
                        offsetInFile < candidate.range.endOffset &&
                        elementRange.startOffset <= candidate.range.startOffset &&
                        candidate.range.endOffset <= elementRange.endOffset
            }
            ?: return null

        return SpecDDPathReference(
            element = element,
            rangeInElement = candidate.range.shiftLeft(elementRange.startOffset),
            candidate = candidate,
            context = resolutionContext,
        )
    }
}

internal fun PsiElement.pathResolutionContext(): SpecDDPathResolutionContext? {
    val projectRoot = project.basePath?.let { basePath -> Path.of(basePath).normalize() } ?: return null
    val specDirectory = containingFile
        ?.virtualFile
        ?.parent
        ?.path
        ?.let { path -> Path.of(path).normalize() }
        ?: projectRoot
    return SpecDDPathResolutionContext(projectRoot = projectRoot, specDirectory = specDirectory)
}
