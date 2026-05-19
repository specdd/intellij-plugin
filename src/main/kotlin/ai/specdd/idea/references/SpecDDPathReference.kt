package ai.specdd.idea.references

import com.intellij.openapi.util.TextRange
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.*

class SpecDDPathReference(
    element: PsiElement,
    rangeInElement: TextRange,
    private val candidate: SpecDDPathCandidate,
    private val context: SpecDDPathResolutionContext?,
    private val resolver: SpecDDPathResolver = SpecDDPathResolver(),
    private val targetMapper: (VirtualFile, PsiElement) -> PsiElement? = { virtualFile, sourceElement ->
        virtualFileToPsiElement(virtualFile, sourceElement)
    },
) : PsiPolyVariantReferenceBase<PsiElement>(element, rangeInElement, true) {
    override fun multiResolve(incompleteCode: Boolean): Array<ResolveResult> {
        val resolutionContext = context ?: return ResolveResult.EMPTY_ARRAY
        val resolution = resolver.resolve(candidate, resolutionContext)
        if (SpecDDPathResolutionStatus.RESOLVED != resolution.status) return ResolveResult.EMPTY_ARRAY

        return resolution.targets
            .mapNotNull { virtualFile -> targetMapper(virtualFile, element) }
            .map { target -> PsiElementResolveResult(target) }
            .toTypedArray()
    }

    override fun getCanonicalText(): String = candidate.text
}

internal fun virtualFileToPsiElement(
    virtualFile: VirtualFile,
    element: PsiElement,
): PsiElement? =
    PsiManager.getInstance(element.project).let { psiManager ->
        if (virtualFile.isDirectory) psiManager.findDirectory(virtualFile) else psiManager.findFile(virtualFile)
    }
