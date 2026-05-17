package ai.specdd.idea.references

import com.intellij.openapi.util.TextRange
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.*
import java.nio.file.Path

class SpecDDPathReference(
    element: PsiElement,
    rangeInElement: TextRange,
    private val candidate: SpecDDPathCandidate,
    private val context: SpecDDPathResolutionContext?,
    private val resolver: SpecDDPathResolver = SpecDDPathResolver(),
    private val targetMapper: (Path, PsiElement) -> PsiElement? = { path, sourceElement ->
        pathToPsiElement(path, sourceElement)
    },
) : PsiPolyVariantReferenceBase<PsiElement>(element, rangeInElement, true) {
    override fun multiResolve(incompleteCode: Boolean): Array<ResolveResult> {
        val resolutionContext = context ?: return ResolveResult.EMPTY_ARRAY
        val resolution = resolver.resolve(candidate, resolutionContext)
        if (SpecDDPathResolutionStatus.RESOLVED != resolution.status) return ResolveResult.EMPTY_ARRAY

        return resolution.targets
            .mapNotNull { path -> targetMapper(path, element) }
            .map { target -> PsiElementResolveResult(target) }
            .toTypedArray()
    }

    override fun getCanonicalText(): String = candidate.text
}

internal fun pathToPsiElement(
    path: Path,
    element: PsiElement,
    findVirtualFile: (Path) -> VirtualFile? = { targetPath ->
        LocalFileSystem.getInstance().refreshAndFindFileByNioFile(targetPath)
    },
): PsiElement? =
    findVirtualFile(path)?.let { virtualFile ->
        val psiManager = PsiManager.getInstance(element.project)
        if (virtualFile.isDirectory) {
            psiManager.findDirectory(virtualFile)
        } else {
            psiManager.findFile(virtualFile)
        }
    }
