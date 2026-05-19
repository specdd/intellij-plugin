package ai.specdd.idea.references

import com.intellij.openapi.util.TextRange

class SpecDDCodeReferenceExtractor {
    fun extract(text: CharSequence): List<SpecDDCodeReferenceCandidate> {
        val candidates = mutableListOf<SpecDDCodeReferenceCandidate>()
        var lineStart = 0
        while (lineStart < text.length) {
            val lineEnd = findLineEnd(text, lineStart)
            extractLine(text, lineStart, lineEnd, candidates)
            lineStart = nextLineStart(text, lineEnd)
        }
        return candidates
    }

    private fun extractLine(
        text: CharSequence,
        lineStart: Int,
        lineEnd: Int,
        candidates: MutableList<SpecDDCodeReferenceCandidate>,
    ) {
        var offset = lineStart
        while (offset < lineEnd) {
            if ('`' != text[offset]) {
                offset += 1
                continue
            }

            val closingOffset = indexOf(text, '`', offset + 1, lineEnd)
            if (NO_OFFSET == closingOffset) return

            val contentStart = offset + 1
            val contentEnd = closingOffset
            if (contentStart < contentEnd) {
                candidates.add(
                    SpecDDCodeReferenceCandidate(
                        text = text.subSequence(contentStart, contentEnd).toString(),
                        contentRange = TextRange(contentStart, contentEnd),
                        fullRange = TextRange(offset, closingOffset + 1),
                    ),
                )
            }

            offset = closingOffset + 1
        }
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

    private fun indexOf(text: CharSequence, target: Char, start: Int, end: Int): Int {
        var offset = start
        while (offset < end) {
            if (target == text[offset]) return offset
            offset += 1
        }
        return NO_OFFSET
    }

    private companion object {
        const val NO_OFFSET = -1
    }
}

data class SpecDDCodeReferenceCandidate(
    val text: String,
    val contentRange: TextRange,
    val fullRange: TextRange,
)
