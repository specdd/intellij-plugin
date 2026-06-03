package ai.specdd.idea.references

import ai.specdd.idea.parser.SpecDDKeyValue
import ai.specdd.idea.parser.SpecDDLineClassifier
import ai.specdd.idea.parser.SpecDDLineKind
import com.intellij.openapi.util.TextRange

class SpecDDPathReferenceExtractor(
    private val lineClassifier: SpecDDLineClassifier = SpecDDLineClassifier(),
) {
    fun extract(text: CharSequence): List<SpecDDPathCandidate> {
        val candidates = mutableListOf<SpecDDPathCandidate>()
        var currentSectionLabel: String? = null

        forEachLine(text) { lineStart, lineEnd ->
            val classification = lineClassifier.classify(text, lineStart, lineEnd)

            if (SpecDDLineKind.SECTION == classification.kind) {
                currentSectionLabel = classification.sectionHeader?.label
                return@forEachLine
            }

            val keyValue = classification.keyValue
            if (null != keyValue && isPathSectionKeyCandidate(text, currentSectionLabel, keyValue)) {
                addCandidate(text, keyRange(keyValue), candidates, forcePathSyntax = true, warnIfUnresolved = true)
            }

            if (classification.kind in INLINE_PATH_KINDS) {
                if (currentSectionLabel in PATH_SECTIONS && SpecDDLineKind.TEXT == classification.kind) {
                    addPathSectionEntry(
                        text = text,
                        range = TextRange(classification.contentStart, lineEnd),
                        candidates = candidates,
                    )
                }
                addInlinePathCandidates(
                    text = text,
                    range = TextRange(classification.contentStart, lineEnd),
                    candidates = candidates,
                    forcePathSyntax = currentSectionLabel in PATH_SECTIONS,
                    warnIfUnresolved = true,
                )
            }
        }

        return candidates
    }

    private fun addCandidate(
        text: CharSequence,
        range: TextRange,
        candidates: MutableList<SpecDDPathCandidate>,
        forcePathSyntax: Boolean,
        warnIfUnresolved: Boolean,
    ) {
        if (range.isEmpty || candidates.any { candidate -> candidate.range.intersects(range) }) return

        val value = text.subSequence(range.startOffset, range.endOffset).toString()
        candidates.add(
            SpecDDPathCandidate(
                text = value,
                range = range,
                hasPathSyntax = forcePathSyntax || hasPathSyntax(value),
                warnIfUnresolved = warnIfUnresolved,
            ),
        )
    }

    private fun addInlinePathCandidates(
        text: CharSequence,
        range: TextRange,
        candidates: MutableList<SpecDDPathCandidate>,
        forcePathSyntax: Boolean,
        warnIfUnresolved: Boolean,
    ) {
        val line = text.subSequence(range.startOffset, range.endOffset).toString()
        val urlRanges = URL_PATTERN
            .findAll(line)
            .map { match -> match.range.first..match.range.last }
            .toList()
        val codeSpanRanges = codeSpanRanges(line)

        for (match in PATH_PATTERN.findAll(line)) {
            if (!hasAllowedInlinePathBoundary(line, match.range.first, codeSpanRanges)) {
                continue
            }
            val matchStart = match.range.first
            val matchEnd = trimmedInlinePathEnd(line, match.range.last + 1)
            if (matchEnd <= matchStart) continue

            if (urlRanges.any { urlRange -> matchStart <= urlRange.last && urlRange.first < matchEnd }) {
                continue
            }
            addCandidate(
                text = text,
                range = TextRange(range.startOffset + matchStart, range.startOffset + matchEnd),
                candidates = candidates,
                forcePathSyntax = forcePathSyntax,
                warnIfUnresolved = warnIfUnresolved,
            )
        }
    }

    private fun hasAllowedInlinePathBoundary(line: String, matchStart: Int, codeSpanRanges: List<IntRange>): Boolean {
        if (0 == matchStart) return true

        val previous = line[matchStart - 1]
        if ('`' == previous) {
            return codeSpanRanges.any { codeRange -> codeRange.first == matchStart - 1 }
        }
        return previous.isWhitespace() || previous in OPENING_PUNCTUATION
    }

    private fun addPathSectionEntry(
        text: CharSequence,
        range: TextRange,
        candidates: MutableList<SpecDDPathCandidate>,
    ) {
        val line = text.subSequence(range.startOffset, range.endOffset).toString()
        val trimmedStartInLine = line.indexOfFirst { character -> !character.isWhitespace() }
        if (-1 == trimmedStartInLine) return
        if (!hasExplicitPathPrefix(line.substring(trimmedStartInLine))) return

        val match = PATH_PATTERN.find(line, trimmedStartInLine) ?: return
        if (trimmedStartInLine != match.range.first) return

        val matchEnd = trimmedInlinePathEnd(line, match.range.last + 1)
        if (matchEnd <= match.range.first) return

        addCandidate(
            text = text,
            range = TextRange(range.startOffset + match.range.first, range.startOffset + matchEnd),
            candidates = candidates,
            forcePathSyntax = true,
            warnIfUnresolved = true,
        )
    }

    private fun isPathSectionKeyCandidate(
        text: CharSequence,
        currentSectionLabel: String?,
        keyValue: SpecDDKeyValue,
    ): Boolean {
        if (currentSectionLabel !in PATH_SECTIONS) return false
        return hasExplicitPathPrefix(text.subSequence(keyValue.keyStart, keyValue.keyEnd).toString())
    }

    private fun keyRange(keyValue: SpecDDKeyValue): TextRange {
        return TextRange(keyValue.keyStart, keyValue.keyEnd)
    }

    private fun hasPathSyntax(text: String): Boolean =
        hasExplicitPathPrefix(text)

    private fun codeSpanRanges(line: String): List<IntRange> {
        val ranges = mutableListOf<IntRange>()
        var offset = 0
        while (offset < line.length) {
            if ('`' != line[offset]) {
                offset += 1
                continue
            }

            val closingOffset = line.indexOf('`', startIndex = offset + 1)
            if (-1 == closingOffset) return ranges

            ranges.add(offset..closingOffset)
            offset = closingOffset + 1
        }
        return ranges
    }

    private fun trimmedInlinePathEnd(line: String, endExclusive: Int): Int {
        var end = endExclusive
        while (end > 0 && line[end - 1] in TRAILING_PATH_PUNCTUATION) {
            end -= 1
        }
        return end
    }

    private fun forEachLine(text: CharSequence, block: (Int, Int) -> Unit) {
        var lineStart = 0
        while (lineStart < text.length) {
            val lineEnd = findLineEnd(text, lineStart)
            block(lineStart, lineEnd)
            lineStart = nextLineStart(text, lineEnd)
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
}

data class SpecDDPathCandidate(
    val text: String,
    val range: TextRange,
    val hasPathSyntax: Boolean,
    val warnIfUnresolved: Boolean = hasPathSyntax,
)

private val PATH_SECTIONS = setOf(
    "Structure",
    "Owns",
    "Can modify",
    "Can read",
    "References",
    "Depends on",
    "Forbids",
    "Exposes",
)

private val INLINE_PATH_KINDS = setOf(
    SpecDDLineKind.TASK,
    SpecDDLineKind.SCENARIO_STEP,
    SpecDDLineKind.KEY_VALUE,
    SpecDDLineKind.CONTINUATION,
    SpecDDLineKind.TEXT,
)

private val PATH_PATTERN = Regex(
    """(?:\./|\.\./|/)[A-Za-z0-9_*?.,{}\[\]-]+(?:/[A-Za-z0-9_*?.,{}\[\]-]+)*""",
)
private val URL_PATTERN = Regex("""\b[A-Za-z][A-Za-z0-9+.-]*://\S+""")
private val OPENING_PUNCTUATION = setOf('(', '[', '{', '<', '"', '\'')
private val TRAILING_PATH_PUNCTUATION = setOf('.', ',')

private fun hasExplicitPathPrefix(text: String): Boolean =
    text.startsWith("./") || text.startsWith("../") || text.startsWith("/")
