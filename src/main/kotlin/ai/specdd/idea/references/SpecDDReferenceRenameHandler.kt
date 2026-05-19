package ai.specdd.idea.references

import ai.specdd.idea.SpecDDLanguage
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.actionSystem.DataContext
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiReference
import com.intellij.refactoring.rename.PsiElementRenameHandler
import com.intellij.refactoring.rename.RenameHandler

internal var specDDPsiRenameInvoker: (PsiElement, Project, PsiElement, Editor) -> Unit =
    PsiElementRenameHandler::rename

class SpecDDReferenceRenameHandler : RenameHandler {
    private val requestProvider: (Project, Editor, PsiFile) -> SpecDDReferenceRenameRequest?
    private val renameInvoker: (SpecDDReferenceRenameRequest) -> Unit

    constructor() : this(::specDDReferenceRenameRequest, ::invokeSpecDDReferenceRename)

    internal constructor(
        requestProvider: (Project, Editor, PsiFile) -> SpecDDReferenceRenameRequest?,
        renameInvoker: (SpecDDReferenceRenameRequest) -> Unit,
    ) {
        this.requestProvider = requestProvider
        this.renameInvoker = renameInvoker
    }

    override fun isAvailableOnDataContext(dataContext: DataContext): Boolean {
        val project = CommonDataKeys.PROJECT.getData(dataContext) ?: return false
        val editor = CommonDataKeys.EDITOR.getData(dataContext) ?: return false
        val file = CommonDataKeys.PSI_FILE.getData(dataContext) ?: return false

        return null != requestProvider(project, editor, file)
    }

    override fun invoke(project: Project, editor: Editor, file: PsiFile, dataContext: DataContext) {
        val request = requestProvider(project, editor, file) ?: return
        LOG.debug("Invoking SpecDD reference rename for ${request.target}")
        renameInvoker(request)
    }

    override fun invoke(project: Project, elements: Array<out PsiElement>, dataContext: DataContext) {
        val editor = CommonDataKeys.EDITOR.getData(dataContext) ?: return
        val file = CommonDataKeys.PSI_FILE.getData(dataContext) ?: return

        invoke(project, editor, file, dataContext)
    }

    companion object {
        private val LOG = Logger.getInstance(SpecDDReferenceRenameHandler::class.java)
    }
}

internal data class SpecDDReferenceRenameRequest(
    val project: Project,
    val editor: Editor,
    val contextElement: PsiElement,
    val target: PsiElement,
)

internal fun specDDReferenceRenameRequest(
    project: Project,
    editor: Editor,
    file: PsiFile,
): SpecDDReferenceRenameRequest? {
    if (SpecDDLanguage != file.language) return null

    val reference = specDDReferenceAtCaret(file, editor.caretModel.offset) ?: return null
    val target = reference.resolve()?.takeIf { element -> element.isValid } ?: return null

    return SpecDDReferenceRenameRequest(
        project = project,
        editor = editor,
        contextElement = reference.element,
        target = target,
    )
}

internal fun specDDReferenceAtCaret(file: PsiFile, offset: Int): SpecDDSymbolReference? {
    val reference = file.findReferenceAt(offset)
    if (reference is SpecDDSymbolReference) return reference
    if (0 == offset) return null

    val previousReference = file.findReferenceAt(offset - 1)
    if (previousReference is SpecDDSymbolReference) return previousReference
    return null
}

internal fun invokeSpecDDReferenceRename(request: SpecDDReferenceRenameRequest) {
    specDDPsiRenameInvoker(
        request.target,
        request.project,
        request.contextElement,
        request.editor,
    )
}
