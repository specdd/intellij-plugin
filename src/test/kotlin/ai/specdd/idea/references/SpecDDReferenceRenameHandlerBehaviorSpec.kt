package ai.specdd.idea.references

import ai.specdd.idea.SpecDDLanguage
import com.intellij.lang.Language
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.actionSystem.DataContext
import com.intellij.openapi.editor.CaretModel
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiReference
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import java.lang.reflect.InvocationHandler
import java.lang.reflect.Proxy

class SpecDDReferenceRenameHandlerBehaviorSpec : BehaviorSpec({
    given("a SpecDD reference rename request") {
        val project = project(null)
        val target = psiElement("InvoiceService", project)
        val sourceElement = psiElement("@invoice_demo.service.InvoiceService", project)
        val reference = SpecDDSymbolReference(
            element = sourceElement,
            rangeInElement = TextRange(0, sourceElement.text.length),
            symbolText = "invoice_demo.service.InvoiceService",
            resolver = SpecDDSymbolResolver {
                listOf(FakeRenameContributor(target))
            },
        )

        `when`("the caret is inside a resolved explicit symbol reference in a SpecDD file") {
            then("it uses the resolved target and SpecDD reference element") {
                val request = specDDReferenceRenameRequest(
                    project = project,
                    editor = renameEditor(5),
                    file = renamePsiFile(SpecDDLanguage) { reference },
                )

                (request?.project === project) shouldBe true
                request?.editor?.caretModel?.offset shouldBe 5
                (request?.contextElement === sourceElement) shouldBe true
                (request?.target === target) shouldBe true
            }
        }

        `when`("the caret is at the end of a resolved explicit symbol reference") {
            then("it checks the previous offset") {
                val request = specDDReferenceRenameRequest(
                    project = project,
                    editor = renameEditor(34),
                    file = renamePsiFile(SpecDDLanguage) { offset ->
                        if (33 == offset) reference else null
                    },
                )

                (request?.target === target) shouldBe true
            }
        }

        `when`("the file is not SpecDD, the reference is not a symbol, or the symbol is unresolved") {
            then("it declines the rename request") {
                specDDReferenceRenameRequest(
                    project = project,
                    editor = renameEditor(1),
                    file = renamePsiFile(Language.ANY) { reference },
                ) shouldBe null

                specDDReferenceRenameRequest(
                    project = project,
                    editor = renameEditor(1),
                    file = renamePsiFile(SpecDDLanguage) { nonSymbolReference() },
                ) shouldBe null

                specDDReferenceRenameRequest(
                    project = project,
                    editor = renameEditor(1),
                    file = renamePsiFile(SpecDDLanguage) {
                        SpecDDSymbolReference(
                            element = sourceElement,
                            rangeInElement = TextRange(0, sourceElement.text.length),
                            symbolText = "MissingSymbol",
                        )
                    },
                ) shouldBe null
            }
        }

        `when`("checking references directly at a caret offset") {
            then("it returns only SpecDD symbol references") {
                (specDDReferenceAtCaret(renamePsiFile(SpecDDLanguage) { reference }, 0) === reference) shouldBe true
                specDDReferenceAtCaret(renamePsiFile(SpecDDLanguage) { nonSymbolReference() }, 0) shouldBe null
                specDDReferenceAtCaret(renamePsiFile(SpecDDLanguage) { null }, 0) shouldBe null
            }
        }
    }

    given("a SpecDD rename handler") {
        val project = project(null)
        val editor = renameEditor(3)
        val file = renamePsiFile(SpecDDLanguage) { null }
        val contextElement = psiElement("@invoice_demo.service.InvoiceService", project)
        val target = psiElement("InvoiceService", project)
        val request = SpecDDReferenceRenameRequest(project, editor, contextElement, target)

        `when`("a request exists in the action context") {
            then("it is available and delegates the rename invocation") {
                var invokedRequest: SpecDDReferenceRenameRequest? = null
                val handler = SpecDDReferenceRenameHandler(
                    requestProvider = { requestProject, requestEditor, requestFile ->
                        (requestProject === project) shouldBe true
                        (requestEditor === editor) shouldBe true
                        (requestFile === file) shouldBe true
                        request
                    },
                    renameInvoker = { invokedRequest = it },
                )
                val dataContext = renameDataContext(project, editor, file)

                handler.isAvailableOnDataContext(dataContext) shouldBe true
                handler.invoke(project, editor, file, dataContext)

                (invokedRequest === request) shouldBe true
            }
        }

        `when`("rename is invoked from an element array context") {
            then("it still delegates through the caret request") {
                var invokedRequest: SpecDDReferenceRenameRequest? = null
                val handler = SpecDDReferenceRenameHandler(
                    requestProvider = { _, _, _ -> request },
                    renameInvoker = { invokedRequest = it },
                )

                handler.invoke(project, arrayOf(target), renameDataContext(project, editor, file))

                (invokedRequest === request) shouldBe true
            }
        }

        `when`("the default handler invokes IntelliJ rename support") {
            then("it passes the resolved target and SpecDD context element") {
                val sourceElement = psiElement("@invoice_demo.service.InvoiceService", project)
                val symbolReference = SpecDDSymbolReference(
                    element = sourceElement,
                    rangeInElement = TextRange(0, sourceElement.text.length),
                    symbolText = "invoice_demo.service.InvoiceService",
                    resolver = SpecDDSymbolResolver {
                        listOf(FakeRenameContributor(target))
                    },
                )
                val specDDFile = renamePsiFile(SpecDDLanguage) { symbolReference }
                val previousInvoker = specDDPsiRenameInvoker
                var renamedTarget: PsiElement? = null
                var renameProject: Project? = null
                var renameContext: PsiElement? = null
                var renameEditor: Editor? = null

                try {
                    specDDPsiRenameInvoker = { psiTarget, psiProject, psiContext, psiEditor ->
                        renamedTarget = psiTarget
                        renameProject = psiProject
                        renameContext = psiContext
                        renameEditor = psiEditor
                    }

                    SpecDDReferenceRenameHandler()
                        .invoke(project, editor, specDDFile, renameDataContext(project, editor, specDDFile))
                } finally {
                    specDDPsiRenameInvoker = previousInvoker
                }

                (renamedTarget === target) shouldBe true
                (renameProject === project) shouldBe true
                (renameContext === sourceElement) shouldBe true
                (renameEditor === editor) shouldBe true
            }
        }

        `when`("required action data or a request is missing") {
            then("it is unavailable and does not invoke rename") {
                var invoked = false
                val handler = SpecDDReferenceRenameHandler(
                    requestProvider = { _, _, _ -> null },
                    renameInvoker = { invoked = true },
                )

                handler.isAvailableOnDataContext(renameDataContext(null, editor, file)) shouldBe false
                handler.isAvailableOnDataContext(renameDataContext(project, null, file)) shouldBe false
                handler.isAvailableOnDataContext(renameDataContext(project, editor, null)) shouldBe false
                handler.isAvailableOnDataContext(renameDataContext(project, editor, file)) shouldBe false
                handler.invoke(project, editor, file, renameDataContext(project, editor, file))
                handler.invoke(project, emptyArray(), renameDataContext(project, null, file))
                handler.invoke(project, emptyArray(), renameDataContext(project, editor, null))

                invoked shouldBe false
            }
        }
    }
})

private class FakeRenameContributor(
    private val target: PsiElement,
) {
    @Suppress("UNUSED_PARAMETER")
    fun getItemsByName(
        name: String,
        pattern: String,
        project: Project,
        includeNonProjectItems: Boolean,
    ): Array<Any> = arrayOf(target)
}

private fun renameDataContext(
    project: Project?,
    editor: Editor?,
    file: PsiFile?,
): DataContext =
    DataContext { dataId ->
        when (dataId) {
            CommonDataKeys.PROJECT.name -> project
            CommonDataKeys.EDITOR.name -> editor
            CommonDataKeys.PSI_FILE.name -> file
            else -> null
        }
    }

private fun renameEditor(offset: Int): Editor {
    val caretModel = Proxy.newProxyInstance(
        CaretModel::class.java.classLoader,
        arrayOf(CaretModel::class.java),
        InvocationHandler { _, method, _ ->
            when (method.name) {
                "getOffset" -> offset
                else -> renameDefaultReturn(method.returnType)
            }
        },
    ) as CaretModel

    return Proxy.newProxyInstance(
        Editor::class.java.classLoader,
        arrayOf(Editor::class.java),
        InvocationHandler { _, method, _ ->
            when (method.name) {
                "getCaretModel" -> caretModel
                else -> renameDefaultReturn(method.returnType)
            }
        },
    ) as Editor
}

private fun renamePsiFile(
    language: Language,
    referenceAt: (Int) -> PsiReference?,
): PsiFile =
    Proxy.newProxyInstance(
        PsiFile::class.java.classLoader,
        arrayOf(PsiFile::class.java),
        InvocationHandler { _, method, args ->
            when (method.name) {
                "getLanguage" -> language
                "findReferenceAt" -> referenceAt(args?.firstOrNull() as Int)
                "isValid" -> true
                else -> renameDefaultReturn(method.returnType)
            }
        },
    ) as PsiFile

private fun nonSymbolReference(): PsiReference =
    Proxy.newProxyInstance(
        PsiReference::class.java.classLoader,
        arrayOf(PsiReference::class.java),
        InvocationHandler { _, method, _ ->
            when (method.name) {
                "resolve" -> null
                else -> renameDefaultReturn(method.returnType)
            }
        },
    ) as PsiReference

private fun renameDefaultReturn(returnType: Class<*>): Any? =
    when (returnType) {
        Boolean::class.javaPrimitiveType -> false
        Int::class.javaPrimitiveType -> 0
        Long::class.javaPrimitiveType -> 0L
        Unit::class.javaPrimitiveType -> Unit
        else -> null
    }
