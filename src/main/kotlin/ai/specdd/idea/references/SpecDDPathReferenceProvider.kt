package ai.specdd.idea.references

import ai.specdd.idea.SpecDDFileType
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.ProjectFileIndex
import com.intellij.openapi.roots.ProjectRootManager
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiReference
import com.intellij.psi.PsiReferenceProvider
import com.intellij.util.ProcessingContext

class SpecDDPathReferenceProvider(
    private val extractor: SpecDDPathReferenceExtractor = SpecDDPathReferenceExtractor(),
    private val codeExtractor: SpecDDCodeReferenceExtractor = SpecDDCodeReferenceExtractor(),
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

        val codeReferences = codeExtractor
            .extract(containingFile.text)
            .filter { candidate ->
                elementRange.startOffset <= candidate.contentRange.startOffset &&
                        candidate.contentRange.endOffset <= elementRange.endOffset
            }
            .map { candidate ->
                if (candidate.text.hasExplicitPathPrefix()) {
                    SpecDDPathReference(
                        element = element,
                        rangeInElement = candidate.contentRange.shiftLeft(elementRange.startOffset),
                        candidate = SpecDDPathCandidate(
                            text = candidate.text,
                            range = candidate.contentRange,
                            hasPathSyntax = true,
                            warnIfUnresolved = false,
                        ),
                        context = resolutionContext,
                    )
                } else {
                    SpecDDSymbolReference(
                        element = element,
                        rangeInElement = candidate.contentRange.shiftLeft(elementRange.startOffset),
                        symbolText = candidate.text,
                        resolver = symbolResolver,
                    )
                }
            }

        return (pathReferences + codeReferences).toTypedArray()
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

        return codeExtractor
            .extract(containingFile.text)
            .firstOrNull { codeCandidate ->
                codeCandidate.contentRange.startOffset <= offsetInFile &&
                        offsetInFile < codeCandidate.contentRange.endOffset &&
                        elementRange.startOffset <= codeCandidate.contentRange.startOffset &&
                        codeCandidate.contentRange.endOffset <= elementRange.endOffset
            }
            ?.let { codeCandidate ->
                if (codeCandidate.text.hasExplicitPathPrefix()) {
                    SpecDDPathReference(
                        element = element,
                        rangeInElement = codeCandidate.contentRange.shiftLeft(elementRange.startOffset),
                        candidate = SpecDDPathCandidate(
                            text = codeCandidate.text,
                            range = codeCandidate.contentRange,
                            hasPathSyntax = true,
                            warnIfUnresolved = false,
                        ),
                        context = resolutionContext,
                    )
                } else {
                    SpecDDSymbolReference(
                        element = element,
                        rangeInElement = codeCandidate.contentRange.shiftLeft(elementRange.startOffset),
                        symbolText = codeCandidate.text,
                        resolver = symbolResolver,
                    )
                }
            }
    }
}

private fun String.hasExplicitPathPrefix(): Boolean =
    startsWith("./") || startsWith("../") || startsWith("/")

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
        ?: runCatching { @Suppress("DEPRECATION") project.baseDir }.getOrNull()
        ?: runCatching { ProjectRootManager.getInstance(project).contentRoots.firstOrNull() }.getOrNull()
        ?: virtualFile?.contentRoot(project)
        ?: virtualFile?.topmostParent()

private fun Project.projectDirectory(): VirtualFile? =
    runCatching {
        basePath?.let { path -> LocalFileSystem.getInstance().findFileByPath(path) }
    }.getOrNull()

private fun VirtualFile.contentRoot(project: Project): VirtualFile? =
    runCatching { ProjectFileIndex.getInstance(project).getContentRootForFile(this) }.getOrNull()

private fun VirtualFile.topmostParent(): VirtualFile {
    var current = this
    while (null != current.parent) {
        current = current.parent
    }
    return current
}
