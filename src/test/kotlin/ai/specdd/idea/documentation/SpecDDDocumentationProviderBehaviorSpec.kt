package ai.specdd.idea.documentation

import ai.specdd.idea.SpecDDFileType
import ai.specdd.idea.SpecDDLanguage
import com.intellij.lang.Language
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.fileTypes.FileType
import com.intellij.openapi.util.TextRange
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.FileViewProvider
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiNamedElement
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import java.lang.reflect.InvocationHandler
import java.lang.reflect.Proxy

class SpecDDDocumentationProviderBehaviorSpec : BehaviorSpec({
    given("a SpecDD documentation provider") {
        val provider = SpecDDDocumentationProvider()

        `when`("a target offset is inside a section label") {
            then("it resolves the documented section") {
                val text = "Spec: Example\nPurpose:\n  Describe it."

                provider.sectionAt(text, 0) shouldBe SpecDDDocumentedSection("Spec", TextRange(0, 4))
                provider.sectionAt(text, text.indexOf("Purpose") + 3) shouldBe
                        SpecDDDocumentedSection("Purpose", TextRange(14, 21))
            }
        }

        `when`("a target offset is outside a section label") {
            then("it returns no documented section") {
                val text = "Spec: Example\nPurpose:\n  Describe it."

                provider.sectionAt(text, text.indexOf("Example")).shouldBeNull()
                provider.sectionAt(text, text.indexOf("Describe")).shouldBeNull()
                provider.sectionAt(text, -1).shouldBeNull()
                provider.sectionAt(text, text.length + 1).shouldBeNull()
            }
        }

        `when`("a custom documentation element is requested for a SpecDD file") {
            then("it creates an element that generates hover documentation") {
                val text = "Spec: Example\nCan modify:\n  src/*"
                val element = provider.getCustomDocumentationElement(
                    editor = editor(),
                    file = psiFile("main.sdd", text, SpecDDFileType()),
                    contextElement = null,
                    targetOffset = text.indexOf("Can modify") + 1,
                )

                element.shouldNotBeNull()
                element.text shouldBe "Can modify"
                (element as PsiNamedElement).name shouldBe "Can modify"
                (element.parent === element.containingFile) shouldBe true
                element.textOffset shouldBe 14
                element.textRange shouldBe TextRange(14, 24)
                provider.generateDoc(element, null) shouldContain "Use this section as write authority."
                provider.generateHoverDoc(element, null) shouldContain "Use this section as write authority."
            }
        }

        `when`("a custom documentation element is requested away from a label") {
            then("it returns no documentation element") {
                val text = "Spec: Example\nPurpose:\n  Body."

                provider.getCustomDocumentationElement(
                    editor = editor(),
                    file = psiFile("main.sdd", text, SpecDDFileType()),
                    contextElement = null,
                    targetOffset = text.indexOf("Example"),
                ).shouldBeNull()
            }
        }

        `when`("a custom documentation element is requested for another file type") {
            then("it returns no documentation element") {
                provider.getCustomDocumentationElement(
                    editor = editor(),
                    file = psiFile("plain.txt", "Spec: Example", mockFileType()),
                    contextElement = null,
                    targetOffset = 0,
                ).shouldBeNull()
            }
        }

        `when`("generateDoc receives a normal PSI element") {
            then("it returns no documentation") {
                provider.generateDoc(psiElement(), null).shouldBeNull()
            }
        }
    }
})

private fun editor(): Editor =
    Proxy.newProxyInstance(
        Editor::class.java.classLoader,
        arrayOf(Editor::class.java),
        InvocationHandler { _, method, args -> defaultValue(method.returnType, args) },
    ) as Editor

private fun psiFile(name: String, text: String, fileType: FileType): PsiFile =
    Proxy.newProxyInstance(
        PsiFile::class.java.classLoader,
        arrayOf(PsiFile::class.java),
        InvocationHandler { _, method, args ->
            when (method.name) {
                "getName" -> name
                "getText" -> text
                "getFileType" -> fileType
                "getLanguage" -> SpecDDLanguage
                "getViewProvider" -> fileViewProvider(text)
                else -> defaultValue(method.returnType, args)
            }
        },
    ) as PsiFile

private fun psiElement(): PsiElement =
    Proxy.newProxyInstance(
        PsiElement::class.java.classLoader,
        arrayOf(PsiElement::class.java),
        InvocationHandler { _, method, args -> defaultValue(method.returnType, args) },
    ) as PsiElement

private fun fileViewProvider(text: String): FileViewProvider =
    Proxy.newProxyInstance(
        FileViewProvider::class.java.classLoader,
        arrayOf(FileViewProvider::class.java),
        InvocationHandler { _, method, args ->
            when (method.name) {
                "getBaseLanguage" -> SpecDDLanguage
                "getLanguages" -> setOf(SpecDDLanguage)
                "getContents" -> text
                "isPhysical" -> false
                else -> defaultValue(method.returnType, args)
            }
        },
    ) as FileViewProvider

private fun mockFileType(): FileType =
    Proxy.newProxyInstance(
        FileType::class.java.classLoader,
        arrayOf(FileType::class.java),
        InvocationHandler { _, method, args ->
            when (method.name) {
                "getName" -> "Plain"
                "getDescription" -> "Plain text"
                "getDefaultExtension" -> "txt"
                "isBinary" -> false
                "isReadOnly" -> false
                else -> defaultValue(method.returnType, args)
            }
        },
    ) as FileType

private fun defaultValue(returnType: Class<*>, args: Array<Any?>?): Any? = when {
    Void.TYPE == returnType -> null
    java.lang.Boolean.TYPE == returnType -> false
    Integer.TYPE == returnType -> 0
    java.lang.Long.TYPE == returnType -> 0L
    PsiElement::class.java.isAssignableFrom(returnType) -> null
    PsiFile::class.java.isAssignableFrom(returnType) -> null
    VirtualFile::class.java.isAssignableFrom(returnType) -> null
    Language::class.java.isAssignableFrom(returnType) -> SpecDDLanguage
    else -> args?.firstOrNull()
}
