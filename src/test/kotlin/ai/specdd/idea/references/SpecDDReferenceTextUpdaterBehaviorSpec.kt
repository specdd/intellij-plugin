package ai.specdd.idea.references

import com.intellij.codeInsight.multiverse.CodeInsightContext
import com.intellij.openapi.Disposable
import com.intellij.openapi.application.ModalityState
import com.intellij.openapi.editor.Document
import com.intellij.openapi.editor.RangeMarker
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Computable
import com.intellij.openapi.util.Key
import com.intellij.openapi.util.TextRange
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import java.lang.reflect.InvocationHandler
import java.lang.reflect.Proxy

class SpecDDReferenceTextUpdaterBehaviorSpec : BehaviorSpec({
    given("a SpecDD reference text updater") {
        `when`("a containing file document exists") {
            then("it replaces the reference text at the element-relative range and commits the document") {
                val document = MutableDocument("prefix @invoice_demo.models.Invoice suffix")
                val documentManager = TestPsiDocumentManager(document)
                val project = projectWithDocumentManager(documentManager)
                val file = updaterPsiFile(project)
                val element = updaterPsiElement(
                    text = "@invoice_demo.models.Invoice",
                    project = project,
                    file = file,
                    textRange = TextRange(7, 35),
                )

                val updated = updateReferenceText(
                    element,
                    TextRange(0, "@invoice_demo.models.Invoice".length),
                    "@invoice_demo.models.Receipt",
                )

                (updated === element) shouldBe true
                document.currentText shouldBe "prefix @invoice_demo.models.Receipt suffix"
                documentManager.committedDocument shouldBe document
            }
        }

        `when`("a symbol reference uses its default updater") {
            then("it rewrites through the containing document") {
                val document = MutableDocument("Uses @invoice_demo.models.Invoice here")
                val documentManager = TestPsiDocumentManager(document)
                val project = projectWithDocumentManager(documentManager)
                val file = updaterPsiFile(project)
                val element = updaterPsiElement(
                    text = "@invoice_demo.models.Invoice",
                    project = project,
                    file = file,
                    textRange = TextRange(5, 33),
                )
                val reference = SpecDDSymbolReference(
                    element = element,
                    rangeInElement = TextRange(0, "@invoice_demo.models.Invoice".length),
                    symbolText = "invoice_demo.models.Invoice",
                )

                reference.handleElementRename("Receipt")

                document.currentText shouldBe "Uses @invoice_demo.models.Receipt here"
            }
        }

        `when`("no containing document exists") {
            then("it leaves the source element unchanged") {
                val documentManager = TestPsiDocumentManager(null)
                val project = projectWithDocumentManager(documentManager)
                val file = updaterPsiFile(project)
                val element = updaterPsiElement(
                    text = "@Invoice",
                    project = project,
                    file = file,
                    textRange = TextRange(0, "@Invoice".length),
                )

                val updated = updateReferenceText(element, TextRange(0, "@Invoice".length), "@Receipt")

                (updated === element) shouldBe true
                documentManager.committedDocument shouldBe null
            }
        }
    }
})

private fun projectWithDocumentManager(documentManager: PsiDocumentManager): Project =
    Proxy.newProxyInstance(
        Project::class.java.classLoader,
        arrayOf(Project::class.java),
        InvocationHandler { _, method, args ->
            when (method.name) {
                "getService" -> if (PsiDocumentManager::class.java == args?.firstOrNull()) documentManager else null
                "isDisposed" -> false
                else -> defaultValue(method.returnType)
            }
        },
    ) as Project

private fun updaterPsiFile(project: Project): PsiFile =
    Proxy.newProxyInstance(
        PsiFile::class.java.classLoader,
        arrayOf(PsiFile::class.java),
        InvocationHandler { proxy, method, _ ->
            when (method.name) {
                "getProject" -> project
                "getContainingFile" -> proxy
                "getTextRange" -> TextRange(0, 0)
                "isValid" -> true
                else -> defaultValue(method.returnType)
            }
        },
    ) as PsiFile

private fun updaterPsiElement(
    text: String,
    project: Project,
    file: PsiFile?,
    textRange: TextRange?,
): PsiElement =
    Proxy.newProxyInstance(
        PsiElement::class.java.classLoader,
        arrayOf(PsiElement::class.java),
        InvocationHandler { _, method, _ ->
            when (method.name) {
                "getText" -> text
                "getProject" -> project
                "getContainingFile" -> file
                "getTextRange" -> textRange
                "isValid" -> true
                else -> defaultValue(method.returnType)
            }
        },
    ) as PsiElement

private class TestPsiDocumentManager(
    private val document: Document?,
) : PsiDocumentManager() {
    var committedDocument: Document? = null

    override fun isCommitted(document: Document): Boolean = committedDocument == document

    override fun getPsiFile(document: Document): PsiFile? = null

    override fun getPsiFile(document: Document, context: CodeInsightContext): PsiFile? = null

    override fun getCachedPsiFile(document: Document): PsiFile? = null

    override fun getCachedPsiFile(document: Document, context: CodeInsightContext): PsiFile? = null

    override fun getDocument(file: PsiFile): Document? = document

    override fun getCachedDocument(file: PsiFile): Document? = document

    override fun commitAllDocuments() = Unit

    override fun commitAllDocumentsUnderProgress(): Boolean = true

    override fun performForCommittedDocument(document: Document, action: Runnable) = action.run()

    override fun commitDocument(document: Document) {
        committedDocument = document
    }

    override fun getLastCommittedText(document: Document): CharSequence = document.charsSequence

    override fun getLastCommittedStamp(document: Document): Long = document.modificationStamp

    override fun getLastCommittedDocument(file: PsiFile): Document? = document

    override fun getUncommittedDocuments(): Array<Document> = emptyArray()

    override fun isUncommited(document: Document): Boolean = false

    override fun hasUncommitedDocuments(): Boolean = false

    override fun commitAndRunReadAction(runnable: Runnable) = runnable.run()

    override fun <T : Any?> commitAndRunReadAction(computable: Computable<T>): T = computable.compute()

    override fun reparseFiles(files: Collection<VirtualFile>, includeOpenFiles: Boolean) = Unit

    override fun isDocumentBlockedByPsi(document: Document): Boolean = false

    override fun doPostponedOperationsAndUnblockDocument(document: Document) = Unit

    override fun performWhenAllCommitted(runnable: Runnable): Boolean {
        runnable.run()
        return true
    }

    override fun performLaterWhenAllCommitted(runnable: Runnable) = runnable.run()

    override fun performLaterWhenAllCommitted(state: ModalityState, runnable: Runnable) = runnable.run()
}

private class MutableDocument(initialText: String) : Document {
    private val content = StringBuilder(initialText)
    private val userData = mutableMapOf<Key<*>, Any?>()
    private var stamp = 0L

    val currentText: String
        get() = content.toString()

    override fun getImmutableCharSequence(): CharSequence = content.toString()

    override fun getLineCount(): Int = 1

    override fun getLineNumber(offset: Int): Int = 0

    override fun getLineStartOffset(line: Int): Int = 0

    override fun getLineEndOffset(line: Int): Int = content.length

    override fun insertString(offset: Int, s: CharSequence) {
        content.insert(offset, s)
        stamp += 1
    }

    override fun deleteString(startOffset: Int, endOffset: Int) {
        content.delete(startOffset, endOffset)
        stamp += 1
    }

    override fun replaceString(startOffset: Int, endOffset: Int, s: CharSequence) {
        content.replace(startOffset, endOffset, s.toString())
        stamp += 1
    }

    override fun isWritable(): Boolean = true

    override fun getModificationStamp(): Long = stamp

    override fun createRangeMarker(startOffset: Int, endOffset: Int, surviveOnExternalChange: Boolean): RangeMarker =
        rangeMarker()

    override fun createGuardedBlock(startOffset: Int, endOffset: Int): RangeMarker = rangeMarker()

    override fun setText(text: CharSequence) {
        content.clear()
        content.append(text)
        stamp += 1
    }

    @Suppress("UNCHECKED_CAST")
    override fun <T : Any?> getUserData(key: Key<T>): T? = userData[key] as? T

    override fun <T : Any?> putUserData(key: Key<T>, value: T?) {
        userData[key] = value
    }
}

private fun rangeMarker(): RangeMarker =
    Proxy.newProxyInstance(
        RangeMarker::class.java.classLoader,
        arrayOf(RangeMarker::class.java),
        InvocationHandler { _, method, _ ->
            when (method.name) {
                "isValid" -> true
                else -> defaultValue(method.returnType)
            }
        },
    ) as RangeMarker

private fun defaultValue(returnType: Class<*>): Any? =
    when (returnType) {
        Boolean::class.javaPrimitiveType -> false
        Int::class.javaPrimitiveType -> 0
        Long::class.javaPrimitiveType -> 0L
        Unit::class.javaPrimitiveType -> Unit
        else -> null
    }
