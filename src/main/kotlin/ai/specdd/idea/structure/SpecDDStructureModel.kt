package ai.specdd.idea.structure

import ai.specdd.idea.parser.SpecDDLineClassifier
import ai.specdd.idea.parser.SpecDDLineKind

data class SpecDDStructureSection(
    val label: String,
    val inlineValue: String,
    val sectionStart: Int,
    val labelStart: Int,
    val lineEnd: Int,
) {
    val displayName: String = if (inlineValue.isBlank()) label else "$label: $inlineValue"
}

class SpecDDStructureModel(
    private val lineClassifier: SpecDDLineClassifier = SpecDDLineClassifier(),
) {
    fun sections(text: CharSequence): List<SpecDDStructureSection> {
        val sections = mutableListOf<SpecDDStructureSection>()
        forEachLine(text) { lineStart, lineEnd ->
            val classification = lineClassifier.classify(text, lineStart, lineEnd)
            if (SpecDDLineKind.SECTION != classification.kind) return@forEachLine

            val sectionHeader = classification.sectionHeader ?: return@forEachLine
            sections.add(
                SpecDDStructureSection(
                    label = sectionHeader.label,
                    inlineValue = text.subSequence(sectionHeader.valueStart, sectionHeader.valueEnd).toString().trim(),
                    sectionStart = sectionHeader.labelStart,
                    labelStart = sectionHeader.labelStart,
                    lineEnd = lineEnd,
                ),
            )
        }

        return sections
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
