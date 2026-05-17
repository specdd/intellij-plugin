package ai.specdd.idea.documentation

import ai.specdd.idea.SpecDDFileType
import ai.specdd.idea.parser.SpecDDLineClassifier
import ai.specdd.idea.parser.SpecDDLineKind
import com.intellij.lang.documentation.DocumentationProvider
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.impl.FakePsiElement

class SpecDDDocumentationProvider(
    private val lineClassifier: SpecDDLineClassifier = SpecDDLineClassifier(),
) : DocumentationProvider {
    override fun getCustomDocumentationElement(
        editor: Editor,
        file: PsiFile,
        contextElement: PsiElement?,
        targetOffset: Int,
    ): PsiElement? {
        if (file.fileType !is SpecDDFileType) return null

        val section = sectionAt(file.text, targetOffset) ?: return null
        val html =
            SpecDDSectionDocumentation.htmlFor(section.label) ?: error("Missing documentation for ${section.label}")
        return SpecDDDocumentationElement(file, section.label, section.range, html)
    }

    override fun generateDoc(element: PsiElement, originalElement: PsiElement?): String? =
        (element as? SpecDDDocumentationElement)?.html

    override fun generateHoverDoc(element: PsiElement, originalElement: PsiElement?): String? =
        generateDoc(element, originalElement)

    fun sectionAt(text: CharSequence, targetOffset: Int): SpecDDDocumentedSection? {
        if (targetOffset < 0 || targetOffset > text.length) return null

        val lineStart = findLineStart(text, targetOffset)
        val lineEnd = findLineEnd(text, lineStart)
        val classification = lineClassifier.classify(text, lineStart, lineEnd)
        if (SpecDDLineKind.SECTION != classification.kind) return null

        val sectionHeader = classification.sectionHeader ?: return null
        if (targetOffset < sectionHeader.labelStart || targetOffset >= sectionHeader.labelEnd) return null

        return SpecDDDocumentedSection(
            label = sectionHeader.label,
            range = TextRange(sectionHeader.labelStart, sectionHeader.labelEnd),
        )
    }

    private fun findLineStart(text: CharSequence, offset: Int): Int {
        var lineStart = offset.coerceAtMost(text.length)
        while (lineStart > 0 && '\n' != text[lineStart - 1] && '\r' != text[lineStart - 1]) {
            lineStart -= 1
        }
        return lineStart
    }

    private fun findLineEnd(text: CharSequence, lineStart: Int): Int {
        var offset = lineStart
        while (offset < text.length && '\n' != text[offset] && '\r' != text[offset]) {
            offset += 1
        }
        return offset
    }
}

data class SpecDDDocumentedSection(
    val label: String,
    val range: TextRange,
)

private class SpecDDDocumentationElement(
    private val file: PsiFile,
    private val label: String,
    private val range: TextRange,
    val html: String,
) : FakePsiElement() {
    override fun getParent(): PsiElement = file

    override fun getContainingFile(): PsiFile = file

    override fun getTextRange(): TextRange = range

    override fun getTextOffset(): Int = range.startOffset

    override fun getText(): String = label

    override fun getName(): String = label
}
