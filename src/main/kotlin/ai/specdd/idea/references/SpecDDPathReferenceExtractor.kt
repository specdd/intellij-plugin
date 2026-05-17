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
            if (currentSectionLabel in PATH_SECTIONS && null != keyValue) {
                addCandidate(text, keyRange(keyValue), candidates, forcePathSyntax = true)
            }

            if (classification.kind in INLINE_PATH_KINDS) {
                addInlinePathCandidates(text, TextRange(classification.contentStart, lineEnd), candidates)
            }
        }

        return candidates
    }

    private fun addCandidate(
        text: CharSequence,
        range: TextRange,
        candidates: MutableList<SpecDDPathCandidate>,
        forcePathSyntax: Boolean = false,
    ) {
        if (range.isEmpty || candidates.any { candidate -> candidate.range.intersects(range) }) return

        val value = text.subSequence(range.startOffset, range.endOffset).toString()
        candidates.add(
            SpecDDPathCandidate(
                text = value,
                range = range,
                hasPathSyntax = forcePathSyntax || hasPathSyntax(value),
            ),
        )
    }

    private fun addInlinePathCandidates(
        text: CharSequence,
        range: TextRange,
        candidates: MutableList<SpecDDPathCandidate>,
    ) {
        val line = text.subSequence(range.startOffset, range.endOffset).toString()
        for (match in PATH_PATTERN.findAll(line)) {
            addCandidate(
                text = text,
                range = TextRange(range.startOffset + match.range.first, range.startOffset + match.range.last + 1),
                candidates = candidates,
            )
        }
    }

    private fun keyRange(keyValue: SpecDDKeyValue): TextRange {
        return TextRange(keyValue.keyStart, keyValue.keyEnd)
    }

    private fun hasPathSyntax(text: String): Boolean =
        text.startsWith("~") || text.any { character -> character in PATH_SYNTAX_CHARS }

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
    SpecDDLineKind.TEXT,
)

private val PATH_PATTERN = Regex(
    """(?:\.{1,2}/)?[A-Za-z0-9_*.-]+(?:/[A-Za-z0-9_*.-]+)+|[A-Za-z0-9_.-]+\.(?:sdd|js|ts|tsx|jsx|py|go|rs|java|cs|rb|php|md|json|ya?ml|toml|css|html)\b""",
)
private val PATH_SYNTAX_CHARS = setOf('/', '.', '*', '?', '[', ']', '{', '}')
