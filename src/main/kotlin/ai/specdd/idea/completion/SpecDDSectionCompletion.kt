package ai.specdd.idea.completion

import ai.specdd.idea.parser.SpecDDLanguageFacts
import com.intellij.codeInsight.lookup.LookupElement
import com.intellij.codeInsight.lookup.LookupElementBuilder

object SpecDDSectionCompletion {
    fun complete(text: CharSequence, offset: Int): SpecDDSectionCompletionResult? {
        val prefix = prefixAt(text, offset) ?: return null
        val variants = SpecDDLanguageFacts.sectionLabels
            .filter { label -> label.startsWith(prefix, ignoreCase = true) }
            .map { label -> SpecDDSectionCompletionVariant(label) }

        if (variants.isEmpty()) return null
        return SpecDDSectionCompletionResult(prefix, variants)
    }

    internal fun prefixAt(text: CharSequence, offset: Int): String? {
        if (offset < 0 || offset > text.length) return null

        val lineStart = findLineStart(text, offset)
        if (lineStart < offset && text[lineStart].isWhitespace()) return null

        val prefix = text.subSequence(lineStart, offset).toString()
        if (prefix.any { character -> !character.isLetter() && !character.isWhitespace() }) return null

        return prefix
    }

    private fun findLineStart(text: CharSequence, offset: Int): Int {
        var lineStart = offset
        while (lineStart > 0 && '\n' != text[lineStart - 1] && '\r' != text[lineStart - 1]) {
            lineStart -= 1
        }
        return lineStart
    }
}

data class SpecDDSectionCompletionResult(
    val prefix: String,
    val variants: List<SpecDDSectionCompletionVariant>,
)

data class SpecDDSectionCompletionVariant(
    val label: String,
) {
    val lookupString: String = "$label: "

    fun toLookupElement(): LookupElement =
        LookupElementBuilder
            .create(lookupString)
            .withPresentableText(label)
}
