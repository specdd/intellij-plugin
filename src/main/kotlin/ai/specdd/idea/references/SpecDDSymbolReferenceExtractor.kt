package ai.specdd.idea.references

import ai.specdd.idea.parser.SpecDDLineClassifier
import ai.specdd.idea.parser.SpecDDLineKind
import com.intellij.openapi.util.TextRange

class SpecDDSymbolReferenceExtractor(
    private val lineClassifier: SpecDDLineClassifier = SpecDDLineClassifier(),
) {
    fun extract(text: CharSequence): List<SpecDDSymbolReferenceCandidate> {
        val candidates = mutableListOf<SpecDDSymbolReferenceCandidate>()
        var currentSectionLabel: String? = null
        var lineStart = 0
        while (lineStart < text.length) {
            val lineEnd = findLineEnd(text, lineStart)
            val classification = lineClassifier.classify(text, lineStart, lineEnd, currentSectionLabel)
            if (SpecDDLineKind.SECTION == classification.kind) {
                currentSectionLabel = classification.sectionHeader?.label
            } else if (null != currentSectionLabel && classification.kind in SYMBOL_LINE_KINDS) {
                extractLine(text, classification.contentStart, lineEnd, candidates)
            }
            lineStart = nextLineStart(text, lineEnd)
        }
        return candidates
    }

    private fun extractLine(
        text: CharSequence,
        lineStart: Int,
        lineEnd: Int,
        candidates: MutableList<SpecDDSymbolReferenceCandidate>,
    ) {
        var offset = lineStart
        while (offset < lineEnd) {
            if ('@' != text[offset] || !canStartSymbol(text, lineStart, offset, lineEnd)) {
                offset += 1
                continue
            }

            val symbolStart = offset + 1
            var symbolEnd = symbolStart + 1
            while (symbolEnd < lineEnd && isSymbolPart(text[symbolEnd])) {
                symbolEnd += 1
            }

            val trimmedEnd = trimSentencePeriod(text, symbolStart, symbolEnd, lineEnd)
            if (symbolStart < trimmedEnd) {
                candidates.add(
                    SpecDDSymbolReferenceCandidate(
                        text = text.subSequence(symbolStart, trimmedEnd).toString(),
                        range = TextRange(offset, trimmedEnd),
                    ),
                )
            }
            offset = symbolEnd
        }
    }

    private fun canStartSymbol(text: CharSequence, lineStart: Int, atOffset: Int, lineEnd: Int): Boolean {
        if (atOffset + 1 >= lineEnd || !isSymbolStart(text[atOffset + 1])) return false
        if (lineStart == atOffset) return true

        val previous = text[atOffset - 1]
        if ('\\' == previous) return false

        return previous.isWhitespace() || previous in OPENING_PUNCTUATION || '`' == previous
    }

    private fun trimSentencePeriod(
        text: CharSequence,
        symbolStart: Int,
        symbolEnd: Int,
        lineEnd: Int,
    ): Int {
        if (symbolStart >= symbolEnd || '.' != text[symbolEnd - 1]) return symbolEnd
        if (symbolEnd >= lineEnd) return symbolEnd - 1

        val next = text[symbolEnd]
        if (next.isWhitespace() || next in CLOSING_PUNCTUATION) return symbolEnd - 1
        return symbolEnd
    }

    private fun findLineEnd(text: CharSequence, lineStart: Int): Int {
        var offset = lineStart
        while (offset < text.length && '\n' != text[offset] && '\r' != text[offset]) {
            offset += 1
        }
        return offset
    }

    private fun nextLineStart(text: CharSequence, lineEnd: Int): Int {
        if (lineEnd >= text.length) return text.length
        if ('\r' != text[lineEnd]) return lineEnd + 1
        if (lineEnd + 1 < text.length && '\n' == text[lineEnd + 1]) return lineEnd + 2
        return lineEnd + 1
    }
}

data class SpecDDSymbolReferenceCandidate(
    val text: String,
    val range: TextRange,
)

private fun isSymbolStart(character: Char): Boolean =
    character in 'A'..'Z' || character in 'a'..'z' || '_' == character

private fun isSymbolPart(character: Char): Boolean =
    isSymbolStart(character) ||
            character in '0'..'9' ||
            '.' == character ||
            ':' == character ||
            '#' == character ||
            '\\' == character ||
            '/' == character ||
            '?' == character ||
            '!' == character

private val OPENING_PUNCTUATION = setOf('(', '[', '{', '<', '"', '\'')
private val CLOSING_PUNCTUATION = setOf(')', ']', '}', '>', '"', '\'')
private val SYMBOL_LINE_KINDS = setOf(
    SpecDDLineKind.TASK,
    SpecDDLineKind.SCENARIO_STEP,
    SpecDDLineKind.KEY_VALUE,
    SpecDDLineKind.CONTINUATION,
    SpecDDLineKind.TEXT,
)
