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
    private val referenceTextUpdater: (PsiElement, TextRange, String) -> PsiElement = ::updateReferenceText,
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

    override fun handleElementRename(newElementName: String): PsiElement {
        if (candidate.isGlob) return element

        val renamedText = candidate.text.renameLastPathSegment(newElementName)
        return referenceTextUpdater(element, rangeInElement, renamedText)
    }

    override fun bindToElement(targetElement: PsiElement): PsiElement {
        if (candidate.isGlob) return element

        val resolutionContext = context ?: return element
        val target = targetElement.targetVirtualFile() ?: return element
        if (!isInRoot(target, resolutionContext.projectRoot)) return element
        if (!resolutionContext.isInProject(target)) return element

        return referenceTextUpdater(
            element,
            rangeInElement,
            candidate.text.rebindPath(target, resolutionContext),
        )
    }

    internal fun participatesInAutomaticRename(): Boolean = !candidate.isGlob
}

internal fun virtualFileToPsiElement(
    virtualFile: VirtualFile,
    element: PsiElement,
): PsiElement? =
    PsiManager.getInstance(element.project).let { psiManager ->
        if (virtualFile.isDirectory) psiManager.findDirectory(virtualFile) else psiManager.findFile(virtualFile)
    }

private fun PsiElement.targetVirtualFile(): VirtualFile? =
    when (this) {
        is PsiDirectory -> virtualFile
        is PsiFile -> virtualFile
        else -> runCatching { navigationElement }
            .getOrNull()
            ?.takeIf { navigation -> navigation !== this }
            ?.targetVirtualFile()
    }

private fun String.renameLastPathSegment(newElementName: String): String {
    val normalized = replace('\\', '/')
    val separator = normalized.lastIndexOf('/')
    if (-1 == separator) return newElementName
    return substring(0, separator + 1) + newElementName
}

private fun String.rebindPath(target: VirtualFile, context: SpecDDPathResolutionContext): String =
    when {
        startsWith("/") -> "/" + relativePath(context.projectRoot, target)
        startsWith("./") || startsWith("../") -> relativePath(context.specDirectory, target).withExplicitRelativePrefix()
        else -> relativePath(context.specDirectory, target)
    }

private fun String.withExplicitRelativePrefix(): String =
    if (startsWith("../")) this else "./$this"
