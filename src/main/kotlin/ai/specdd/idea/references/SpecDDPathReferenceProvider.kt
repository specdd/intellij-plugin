package ai.specdd.idea.references

import ai.specdd.idea.SpecDDFileType
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiReference
import com.intellij.psi.PsiReferenceProvider
import com.intellij.util.ProcessingContext

class SpecDDPathReferenceProvider(
    private val extractor: SpecDDPathReferenceExtractor = SpecDDPathReferenceExtractor(),
    private val symbolExtractor: SpecDDSymbolReferenceExtractor = SpecDDSymbolReferenceExtractor(),
    private val symbolResolver: SpecDDSymbolResolver = SpecDDSymbolResolver(),
) : PsiReferenceProvider() {
    override fun getReferencesByElement(element: PsiElement, context: ProcessingContext): Array<PsiReference> {
        val containingFile = (element as? PsiFile) ?: element.containingFile ?: return PsiReference.EMPTY_ARRAY
        if (containingFile.fileType !is SpecDDFileType) return PsiReference.EMPTY_ARRAY

        val resolutionContext = containingFile.pathResolutionContext() ?: return PsiReference.EMPTY_ARRAY
        val elementRange = element.textRange ?: return PsiReference.EMPTY_ARRAY

        val pathReferences = extractor
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

        val symbolReferences = symbolExtractor
            .extract(containingFile.text)
            .filter { candidate ->
                elementRange.startOffset <= candidate.range.startOffset &&
                        candidate.range.endOffset <= elementRange.endOffset
            }
            .map { candidate ->
                SpecDDSymbolReference(
                    element = element,
                    rangeInElement = candidate.range.shiftLeft(elementRange.startOffset),
                    symbolText = candidate.text,
                    resolver = symbolResolver,
                )
            }

        return (pathReferences + symbolReferences).toTypedArray()
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

        if (null != candidate) {
            return SpecDDPathReference(
                element = element,
                rangeInElement = candidate.range.shiftLeft(elementRange.startOffset),
                candidate = candidate,
                context = resolutionContext,
            )
        }

        return symbolExtractor
            .extract(containingFile.text)
            .firstOrNull { symbolCandidate ->
                symbolCandidate.range.startOffset <= offsetInFile &&
                        offsetInFile < symbolCandidate.range.endOffset &&
                        elementRange.startOffset <= symbolCandidate.range.startOffset &&
                        symbolCandidate.range.endOffset <= elementRange.endOffset
            }
            ?.let { symbolCandidate ->
                SpecDDSymbolReference(
                    element = element,
                    rangeInElement = symbolCandidate.range.shiftLeft(elementRange.startOffset),
                    symbolText = symbolCandidate.text,
                    resolver = symbolResolver,
                )
            }
    }
}

internal fun PsiElement.pathResolutionContext(): SpecDDPathResolutionContext? {
    val file = (this as? PsiFile) ?: containingFile
    val virtualFile = file?.virtualFile
    val projectRoot = projectRootFor(virtualFile) ?: return null
    val specDirectory = virtualFile?.parent ?: projectRoot

    return SpecDDPathResolutionContext(
        projectRoot = projectRoot,
        specDirectory = specDirectory,
        isInProject = { target -> isInRoot(target, projectRoot) },
    )
}

private fun PsiElement.projectRootFor(virtualFile: VirtualFile?): VirtualFile? =
    project.projectDirectory()
        ?: project.projectFileRoot()
        ?: project.workspaceFileRoot()
        ?: virtualFile?.topmostParent()

private fun Project.projectDirectory(): VirtualFile? =
    runCatching {
        basePath?.let { path -> LocalFileSystem.getInstance().findFileByPath(path) }
    }.getOrNull()

private fun Project.projectFileRoot(): VirtualFile? =
    runCatching { projectFile?.projectRootFromProjectMetadataFile() }.getOrNull()

private fun Project.workspaceFileRoot(): VirtualFile? =
    runCatching { workspaceFile?.projectRootFromProjectMetadataFile() }.getOrNull()

private fun VirtualFile.projectRootFromProjectMetadataFile(): VirtualFile? {
    if (isDirectory) {
        return this
    }

    val metadataDirectory = parent ?: return null
    if (".idea" == metadataDirectory.name) {
        return metadataDirectory.parent
    }

    return metadataDirectory
}

private fun VirtualFile.topmostParent(): VirtualFile {
    var current = this
    while (null != current.parent) {
        current = current.parent
    }
    return current
}
