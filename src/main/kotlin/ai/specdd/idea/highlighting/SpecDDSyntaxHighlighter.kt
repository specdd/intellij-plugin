package ai.specdd.idea.highlighting

import ai.specdd.idea.SpecDDLanguage
import ai.specdd.idea.parser.SpecDDLineClassifier
import ai.specdd.idea.parser.SpecDDLineKind
import ai.specdd.idea.parser.SpecDDTaskStatus
import com.intellij.lexer.Lexer
import com.intellij.lexer.LexerBase
import com.intellij.openapi.editor.colors.TextAttributesKey
import com.intellij.openapi.fileTypes.SyntaxHighlighterBase
import com.intellij.psi.TokenType
import com.intellij.psi.tree.IElementType

class SpecDDSyntaxHighlighter : SyntaxHighlighterBase() {
    override fun getHighlightingLexer(): Lexer = SpecDDHighlightingLexer()

    override fun getTokenHighlights(tokenType: IElementType): Array<TextAttributesKey> = when (tokenType) {
        SpecDDHighlightingTokenTypes.COMMENT -> COMMENT_KEYS
        SpecDDHighlightingTokenTypes.INDENT -> INDENT_KEYS
        SpecDDHighlightingTokenTypes.CONTINUATION_TEXT -> CONTINUATION_TEXT_KEYS
        SpecDDHighlightingTokenTypes.SECTION_LABEL -> SECTION_LABEL_KEYS
        SpecDDHighlightingTokenTypes.KEY_VALUE_KEY -> KEY_VALUE_KEY_KEYS
        SpecDDHighlightingTokenTypes.SECTION_META -> SECTION_META_KEYS
        SpecDDHighlightingTokenTypes.SECTION_POSITIVE -> SECTION_POSITIVE_KEYS
        SpecDDHighlightingTokenTypes.SECTION_NEGATIVE -> SECTION_NEGATIVE_KEYS
        SpecDDHighlightingTokenTypes.SECTION_REQUIRED -> SECTION_REQUIRED_KEYS
        SpecDDHighlightingTokenTypes.SECTION_COLON -> SECTION_COLON_KEYS
        SpecDDHighlightingTokenTypes.SECTION_VALUE -> SECTION_VALUE_KEYS
        SpecDDHighlightingTokenTypes.TASK_DONE -> TASK_DONE_KEYS
        SpecDDHighlightingTokenTypes.TASK_OPEN -> TASK_OPEN_KEYS
        SpecDDHighlightingTokenTypes.TASK_BLOCKED -> TASK_BLOCKED_KEYS
        SpecDDHighlightingTokenTypes.TASK_QUESTION -> TASK_QUESTION_KEYS
        SpecDDHighlightingTokenTypes.TASK_SKIPPED -> TASK_SKIPPED_KEYS
        SpecDDHighlightingTokenTypes.TASK_INVALID -> TASK_INVALID_KEYS
        SpecDDHighlightingTokenTypes.TASK_ID -> TASK_ID_KEYS
        SpecDDHighlightingTokenTypes.SCENARIO_STEP -> SCENARIO_STEP_KEYS
        SpecDDHighlightingTokenTypes.CODE_SPAN -> CODE_SPAN_KEYS
        SpecDDHighlightingTokenTypes.CODE_SPAN_DELIMITER -> CODE_SPAN_DELIMITER_KEYS
        SpecDDHighlightingTokenTypes.PATH -> PATH_KEYS
        SpecDDHighlightingTokenTypes.SYMBOL -> SYMBOL_KEYS
        TokenType.BAD_CHARACTER -> BAD_CHARACTER_KEYS
        else -> EMPTY_KEYS
    }
}

private val EMPTY_KEYS = emptyArray<TextAttributesKey>()
private val COMMENT_KEYS = arrayOf(SpecDDHighlightingColors.COMMENT)
private val INDENT_KEYS = arrayOf(SpecDDHighlightingColors.INDENT)
private val CONTINUATION_TEXT_KEYS = arrayOf(SpecDDHighlightingColors.CONTINUATION_TEXT)
private val SECTION_LABEL_KEYS = arrayOf(SpecDDHighlightingColors.SECTION_LABEL)
private val KEY_VALUE_KEY_KEYS = arrayOf(SpecDDHighlightingColors.KEY_VALUE_KEY)
private val SECTION_META_KEYS = arrayOf(SpecDDHighlightingColors.SECTION_META)
private val SECTION_POSITIVE_KEYS = arrayOf(SpecDDHighlightingColors.SECTION_POSITIVE)
private val SECTION_NEGATIVE_KEYS = arrayOf(SpecDDHighlightingColors.SECTION_NEGATIVE)
private val SECTION_REQUIRED_KEYS = arrayOf(SpecDDHighlightingColors.SECTION_REQUIRED)
private val SECTION_COLON_KEYS = arrayOf(SpecDDHighlightingColors.SECTION_COLON)
private val SECTION_VALUE_KEYS = arrayOf(SpecDDHighlightingColors.SECTION_VALUE)
private val TASK_DONE_KEYS = arrayOf(SpecDDHighlightingColors.TASK_DONE)
private val TASK_OPEN_KEYS = arrayOf(SpecDDHighlightingColors.TASK_OPEN)
private val TASK_BLOCKED_KEYS = arrayOf(SpecDDHighlightingColors.TASK_BLOCKED)
private val TASK_QUESTION_KEYS = arrayOf(SpecDDHighlightingColors.TASK_QUESTION)
private val TASK_SKIPPED_KEYS = arrayOf(SpecDDHighlightingColors.TASK_SKIPPED)
private val TASK_INVALID_KEYS = arrayOf(SpecDDHighlightingColors.TASK_INVALID)
private val TASK_ID_KEYS = arrayOf(SpecDDHighlightingColors.TASK_ID)
private val SCENARIO_STEP_KEYS = arrayOf(SpecDDHighlightingColors.SCENARIO_STEP)
private val CODE_SPAN_KEYS = arrayOf(SpecDDHighlightingColors.CODE_SPAN)
private val CODE_SPAN_DELIMITER_KEYS = arrayOf(SpecDDHighlightingColors.CODE_SPAN_DELIMITER)
private val PATH_KEYS = arrayOf(SpecDDHighlightingColors.PATH)
private val SYMBOL_KEYS = arrayOf(SpecDDHighlightingColors.SYMBOL)
private val BAD_CHARACTER_KEYS = arrayOf(SpecDDHighlightingColors.BAD_CHARACTER)

private class SpecDDHighlightingLexer : LexerBase() {
    private var buffer: CharSequence = ""
    private var bufferEnd: Int = 0
    private var tokenIndex: Int = 0
    private var tokens: List<SpecDDToken> = emptyList()

    override fun start(buffer: CharSequence, startOffset: Int, endOffset: Int, initialState: Int) {
        this.buffer = buffer
        this.bufferEnd = endOffset
        tokenIndex = 0
        tokens = SpecDDTokenBuilder(buffer, startOffset, endOffset).build()
    }

    override fun getState(): Int = 0

    override fun getTokenType(): IElementType? = tokens.getOrNull(tokenIndex)?.type

    override fun getTokenStart(): Int = tokens.getOrNull(tokenIndex)?.start ?: bufferEnd

    override fun getTokenEnd(): Int = tokens.getOrNull(tokenIndex)?.end ?: bufferEnd

    override fun advance() {
        tokenIndex += 1
    }

    override fun getBufferSequence(): CharSequence = buffer

    override fun getBufferEnd(): Int = bufferEnd
}

private class SpecDDTokenBuilder(
    private val buffer: CharSequence,
    private val startOffset: Int,
    private val endOffset: Int,
) {
    private val classifiedTokens = mutableListOf<SpecDDToken>()
    private val lineClassifier = SpecDDLineClassifier()
    private var currentSectionLabel: String? = null

    fun build(): List<SpecDDToken> {
        val classificationStart = findLineStart(startOffset)
        val classificationEnd = findClassificationEnd(endOffset)
        currentSectionLabel = findCurrentSectionLabel(classificationStart)
        var lineStart = classificationStart
        while (lineStart < classificationEnd) {
            val lineEnd = findLineEnd(lineStart)
            classifyLine(lineStart, lineEnd)
            lineStart = if (lineEnd < classificationEnd && '\r' == buffer[lineEnd]) {
                if (lineEnd + 1 < classificationEnd && '\n' == buffer[lineEnd + 1]) lineEnd + 2 else lineEnd + 1
            } else {
                lineEnd + 1
            }
        }

        return fillGaps(classifiedTokens.sortedBy { token -> token.start })
    }

    private fun classifyLine(lineStart: Int, lineEnd: Int) {
        val classification = lineClassifier.classify(buffer, lineStart, lineEnd, currentSectionLabel)
        if (SpecDDLineKind.SECTION != classification.kind && lineStart < classification.contentStart) {
            classifiedTokens.add(
                SpecDDToken(
                    lineStart,
                    classification.contentStart,
                    SpecDDHighlightingTokenTypes.INDENT
                )
            )
        }
        if (SpecDDLineKind.CONTINUATION == classification.kind) {
            classifiedTokens.add(
                SpecDDToken(classification.contentStart, lineEnd, SpecDDHighlightingTokenTypes.CONTINUATION_TEXT),
            )
            return
        }
        when (classification.kind) {
            SpecDDLineKind.BLANK -> return
            SpecDDLineKind.COMMENT -> {
                classifiedTokens.add(
                    SpecDDToken(classification.contentStart, lineEnd, SpecDDHighlightingTokenTypes.COMMENT),
                )
                return
            }

            SpecDDLineKind.SECTION -> {
                val sectionHeader = classification.sectionHeader ?: return
                currentSectionLabel = sectionHeader.label
                classifiedTokens.add(
                    SpecDDToken(
                        sectionHeader.labelStart,
                        sectionHeader.labelEnd,
                        sectionHeader.label.sectionTokenType,
                    ),
                )
                classifiedTokens.add(
                    SpecDDToken(
                        sectionHeader.colonStart,
                        sectionHeader.colonStart + 1,
                        SpecDDHighlightingTokenTypes.SECTION_COLON,
                    ),
                )
                if (sectionHeader.valueStart < sectionHeader.valueEnd) {
                    classifiedTokens.add(
                        SpecDDToken(
                            sectionHeader.valueStart,
                            sectionHeader.valueEnd,
                            SpecDDHighlightingTokenTypes.SECTION_VALUE,
                        ),
                    )
                }
                return
            }

            SpecDDLineKind.CONTINUATION -> Unit
            SpecDDLineKind.TASK -> {
                val taskMarker = classification.taskMarker ?: return
                classifiedTokens.add(
                    SpecDDToken(taskMarker.markerStart, taskMarker.markerEnd, taskMarker.status.tokenType),
                )
                val taskId = taskMarker.taskId
                if (null != taskId) {
                    classifiedTokens.add(SpecDDToken(taskId.start, taskId.end, SpecDDHighlightingTokenTypes.TASK_ID))
                }
            }

            SpecDDLineKind.SCENARIO_STEP -> {
                val scenarioStep = classification.scenarioStep ?: return
                classifiedTokens.add(
                    SpecDDToken(scenarioStep.start, scenarioStep.end, SpecDDHighlightingTokenTypes.SCENARIO_STEP),
                )
            }

            SpecDDLineKind.KEY_VALUE -> {
                val keyValue = classification.keyValue ?: return
                classifiedTokens.add(
                    SpecDDToken(keyValue.keyStart, keyValue.keyEnd, SpecDDHighlightingTokenTypes.KEY_VALUE_KEY),
                )
                classifiedTokens.add(
                    SpecDDToken(
                        keyValue.colonStart,
                        keyValue.colonStart + 1,
                        SpecDDHighlightingTokenTypes.SECTION_COLON,
                    ),
                )
            }

            SpecDDLineKind.TEXT -> Unit
        }

        classifyInlinePatterns(classification.contentStart, lineEnd)
    }

    private fun classifyInlinePatterns(contentStart: Int, lineEnd: Int) {
        addCodeSpanMatches(contentStart, lineEnd)
        addPathMatches(contentStart, lineEnd)
        addSymbolMatches(contentStart, lineEnd)
        addRegexMatches(TASK_ID_PATTERN, contentStart, lineEnd, SpecDDHighlightingTokenTypes.TASK_ID)
    }

    private fun addPathMatches(contentStart: Int, lineEnd: Int) {
        val line = buffer.subSequence(contentStart, lineEnd).toString()
        val urlRanges = URL_PATTERN
            .findAll(line)
            .map { match -> match.range.first..match.range.last }
            .toList()

        for (match in PATH_PATTERN.findAll(line)) {
            val start = contentStart + match.range.first
            val end = contentStart + match.range.last + 1
            if (!hasAllowedPathBoundary(start)) continue
            if (urlRanges.any { urlRange -> match.range.first <= urlRange.last && urlRange.first < match.range.last + 1 }) {
                continue
            }
            if (isRangeFree(start, end)) {
                classifiedTokens.add(SpecDDToken(start, end, SpecDDHighlightingTokenTypes.PATH))
            }
        }
    }

    private fun hasAllowedPathBoundary(start: Int): Boolean {
        if (0 == start) return true

        val previous = buffer[start - 1]
        return previous.isWhitespace() || previous in PATH_OPENING_PUNCTUATION
    }

    private fun addSymbolMatches(contentStart: Int, lineEnd: Int) {
        var offset = contentStart
        while (offset < lineEnd) {
            if ('@' != buffer[offset] || !canStartSymbol(contentStart, offset, lineEnd)) {
                offset += 1
                continue
            }

            val symbolStart = offset + 1
            var symbolEnd = symbolStart + 1
            while (symbolEnd < lineEnd && isSymbolPart(buffer[symbolEnd])) {
                symbolEnd += 1
            }
            val trimmedEnd = trimSentencePeriod(symbolStart, symbolEnd, lineEnd)
            if (offset < trimmedEnd && isRangeFree(offset, trimmedEnd)) {
                classifiedTokens.add(SpecDDToken(offset, trimmedEnd, SpecDDHighlightingTokenTypes.SYMBOL))
            }
            offset = symbolEnd
        }
    }

    private fun canStartSymbol(contentStart: Int, atOffset: Int, lineEnd: Int): Boolean {
        if (atOffset + 1 >= lineEnd || !isSymbolStart(buffer[atOffset + 1])) return false
        if (contentStart == atOffset) return true

        val previous = buffer[atOffset - 1]
        if ('\\' == previous) return false
        return previous.isWhitespace() || previous in SYMBOL_OPENING_PUNCTUATION
    }

    private fun trimSentencePeriod(symbolStart: Int, symbolEnd: Int, lineEnd: Int): Int {
        if (symbolStart >= symbolEnd || '.' != buffer[symbolEnd - 1]) return symbolEnd
        if (symbolEnd >= lineEnd) return symbolEnd - 1

        val next = buffer[symbolEnd]
        if (next.isWhitespace() || next in SYMBOL_CLOSING_PUNCTUATION) return symbolEnd - 1
        return symbolEnd
    }

    private fun addCodeSpanMatches(contentStart: Int, lineEnd: Int) {
        var offset = contentStart
        while (offset < lineEnd) {
            if ('`' != buffer[offset]) {
                offset += 1
                continue
            }

            val closingOffset = indexOf('`', offset + 1, lineEnd)
            if (NO_OFFSET == closingOffset) return

            if (isRangeFree(offset, closingOffset + 1)) {
                classifiedTokens.add(SpecDDToken(offset, offset + 1, SpecDDHighlightingTokenTypes.CODE_SPAN_DELIMITER))
                classifiedTokens.add(SpecDDToken(offset + 1, closingOffset, SpecDDHighlightingTokenTypes.CODE_SPAN))
                classifiedTokens.add(
                    SpecDDToken(
                        closingOffset,
                        closingOffset + 1,
                        SpecDDHighlightingTokenTypes.CODE_SPAN_DELIMITER,
                    ),
                )
            }
            offset = closingOffset + 1
        }
    }

    private fun findCurrentSectionLabel(beforeOffset: Int): String? {
        var current: String? = null
        var lineStart = 0
        while (lineStart < beforeOffset) {
            val lineEnd = minOf(findLineEnd(lineStart), beforeOffset)
            val classification = lineClassifier.classify(buffer, lineStart, lineEnd, current)
            if (SpecDDLineKind.SECTION == classification.kind) {
                current = classification.sectionHeader?.label
            }
            lineStart = if (lineEnd < beforeOffset && '\r' == buffer[lineEnd]) {
                if (lineEnd + 1 < beforeOffset && '\n' == buffer[lineEnd + 1]) lineEnd + 2 else lineEnd + 1
            } else {
                lineEnd + 1
            }
        }
        return current
    }

    private fun addRegexMatches(pattern: Regex, contentStart: Int, lineEnd: Int, type: IElementType) {
        val line = buffer.subSequence(contentStart, lineEnd).toString()
        for (match in pattern.findAll(line)) {
            val start = contentStart + match.range.first
            val end = contentStart + match.range.last + 1
            if (isRangeFree(start, end)) {
                classifiedTokens.add(SpecDDToken(start, end, type))
            }
        }
    }

    private fun fillGaps(sortedTokens: List<SpecDDToken>): List<SpecDDToken> {
        val result = mutableListOf<SpecDDToken>()
        var current = startOffset
        for (token in sortedTokens) {
            val clippedStart = maxOf(token.start, startOffset)
            val clippedEnd = minOf(token.end, endOffset)
            if (clippedEnd <= current || clippedEnd <= clippedStart) continue
            if (current < clippedStart) {
                result.add(SpecDDToken(current, clippedStart, SpecDDHighlightingTokenTypes.TEXT))
            }
            result.add(SpecDDToken(clippedStart, clippedEnd, token.type))
            current = clippedEnd
        }
        if (current < endOffset) {
            result.add(SpecDDToken(current, endOffset, SpecDDHighlightingTokenTypes.TEXT))
        }
        return result
    }

    private fun isRangeFree(start: Int, end: Int): Boolean {
        for (token in classifiedTokens) {
            if (start < token.end && token.start < end) return false
        }
        return true
    }

    private fun findLineStart(offset: Int): Int {
        var current = offset
        while (0 < current && '\n' != buffer[current - 1] && '\r' != buffer[current - 1]) {
            current -= 1
        }
        return current
    }

    private fun findClassificationEnd(offset: Int): Int {
        var current = offset
        while (current < buffer.length && '\n' != buffer[current] && '\r' != buffer[current]) {
            current += 1
        }
        return current
    }

    private fun findLineEnd(lineStart: Int): Int {
        var offset = lineStart
        while (offset < buffer.length && '\n' != buffer[offset] && '\r' != buffer[offset]) {
            offset += 1
        }
        return offset
    }

    private fun indexOf(target: Char, start: Int, end: Int): Int {
        var offset = start
        while (offset < end) {
            if (target == buffer[offset]) return offset
            offset += 1
        }
        return NO_OFFSET
    }
}

private val TASK_ID_PATTERN = Regex("""#\d+\b""")
private const val NO_OFFSET = -1
private val PATH_PATTERN = Regex(
    """(?:\./|\.\./|/)[A-Za-z0-9_*?.{}\[\].-]+(?:/[A-Za-z0-9_*?.{}\[\].-]+)*""",
)
private val URL_PATTERN = Regex("""\b[A-Za-z][A-Za-z0-9+.-]*://\S+""")
private val PATH_OPENING_PUNCTUATION = setOf('(', '[', '{', '<', '"', '\'', '`')
private val SYMBOL_OPENING_PUNCTUATION = setOf('(', '[', '{', '<', '"', '\'')
private val SYMBOL_CLOSING_PUNCTUATION = setOf(')', ']', '}', '>', '"', '\'')

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

private data class SpecDDToken(
    val start: Int,
    val end: Int,
    val type: IElementType,
)

private object SpecDDHighlightingTokenTypes {
    val TEXT = SpecDDHighlightingTokenType("TEXT")
    val COMMENT = SpecDDHighlightingTokenType("COMMENT")
    val INDENT = SpecDDHighlightingTokenType("INDENT")
    val CONTINUATION_TEXT = SpecDDHighlightingTokenType("CONTINUATION_TEXT")
    val SECTION_LABEL = SpecDDHighlightingTokenType("SECTION_LABEL")
    val KEY_VALUE_KEY = SpecDDHighlightingTokenType("KEY_VALUE_KEY")
    val SECTION_META = SpecDDHighlightingTokenType("SECTION_META")
    val SECTION_POSITIVE = SpecDDHighlightingTokenType("SECTION_POSITIVE")
    val SECTION_NEGATIVE = SpecDDHighlightingTokenType("SECTION_NEGATIVE")
    val SECTION_REQUIRED = SpecDDHighlightingTokenType("SECTION_REQUIRED")
    val SECTION_COLON = SpecDDHighlightingTokenType("SECTION_COLON")
    val SECTION_VALUE = SpecDDHighlightingTokenType("SECTION_VALUE")
    val TASK_DONE = SpecDDHighlightingTokenType("TASK_DONE")
    val TASK_OPEN = SpecDDHighlightingTokenType("TASK_OPEN")
    val TASK_BLOCKED = SpecDDHighlightingTokenType("TASK_BLOCKED")
    val TASK_QUESTION = SpecDDHighlightingTokenType("TASK_QUESTION")
    val TASK_SKIPPED = SpecDDHighlightingTokenType("TASK_SKIPPED")
    val TASK_INVALID = SpecDDHighlightingTokenType("TASK_INVALID")
    val TASK_ID = SpecDDHighlightingTokenType("TASK_ID")
    val SCENARIO_STEP = SpecDDHighlightingTokenType("SCENARIO_STEP")
    val CODE_SPAN = SpecDDHighlightingTokenType("CODE_SPAN")
    val CODE_SPAN_DELIMITER = SpecDDHighlightingTokenType("CODE_SPAN_DELIMITER")
    val PATH = SpecDDHighlightingTokenType("PATH")
    val SYMBOL = SpecDDHighlightingTokenType("SYMBOL")
}

private class SpecDDHighlightingTokenType(debugName: String) : IElementType(debugName, SpecDDLanguage)

private val SpecDDTaskStatus.tokenType: IElementType
    get() = when (this) {
        SpecDDTaskStatus.DONE -> SpecDDHighlightingTokenTypes.TASK_DONE
        SpecDDTaskStatus.OPEN -> SpecDDHighlightingTokenTypes.TASK_OPEN
        SpecDDTaskStatus.BLOCKED -> SpecDDHighlightingTokenTypes.TASK_BLOCKED
        SpecDDTaskStatus.QUESTION -> SpecDDHighlightingTokenTypes.TASK_QUESTION
        SpecDDTaskStatus.SKIPPED -> SpecDDHighlightingTokenTypes.TASK_SKIPPED
        SpecDDTaskStatus.INVALID -> SpecDDHighlightingTokenTypes.TASK_INVALID
    }

private val String.sectionTokenType: IElementType
    get() = when (this) {
        "Spec",
        "Platform",
            -> SpecDDHighlightingTokenTypes.SECTION_META

        "Can modify",
        "Can read",
        "Exposes",
        "Accepts",
        "Returns",
        "Handles",
            -> SpecDDHighlightingTokenTypes.SECTION_POSITIVE

        "Must not",
        "Forbids",
        "Raises",
            -> SpecDDHighlightingTokenTypes.SECTION_NEGATIVE

        "Must" -> SpecDDHighlightingTokenTypes.SECTION_REQUIRED
        else -> SpecDDHighlightingTokenTypes.SECTION_LABEL
    }
