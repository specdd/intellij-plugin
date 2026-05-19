package ai.specdd.idea.highlighting

import com.intellij.openapi.editor.DefaultLanguageHighlighterColors
import com.intellij.openapi.editor.HighlighterColors
import com.intellij.openapi.editor.colors.TextAttributesKey
import com.intellij.openapi.editor.markup.TextAttributes
import java.awt.Color
import java.awt.Font

object SpecDDHighlightingColors {
    val COMMENT: TextAttributesKey = TextAttributesKey.createTextAttributesKey(
        "SPECDD_COMMENT",
        DefaultLanguageHighlighterColors.LINE_COMMENT,
    )

    @Suppress("DEPRECATION")
    val INDENT: TextAttributesKey = TextAttributesKey.createTextAttributesKey(
        "SPECDD_INDENT",
        TextAttributes(Color(0x6E7781), null, null, null, Font.PLAIN),
    )

    @Suppress("DEPRECATION")
    val CONTINUATION_TEXT: TextAttributesKey = TextAttributesKey.createTextAttributesKey(
        "SPECDD_CONTINUATION_TEXT",
        TextAttributes(Color(0x7A828E), null, null, null, Font.PLAIN),
    )

    @Suppress("DEPRECATION")
    val SECTION_LABEL: TextAttributesKey = TextAttributesKey.createTextAttributesKey(
        "SPECDD_SECTION_LABEL",
        TextAttributes(Color(0x58A6FF), null, null, null, Font.BOLD),
    )
    val KEY_VALUE_KEY: TextAttributesKey = TextAttributesKey.createTextAttributesKey(
        "SPECDD_KEY_VALUE_KEY",
        DefaultLanguageHighlighterColors.INSTANCE_FIELD,
    )

    @Suppress("DEPRECATION")
    val SECTION_META: TextAttributesKey = TextAttributesKey.createTextAttributesKey(
        "SPECDD_SECTION_META",
        TextAttributes(Color(0x7A004B), null, null, null, Font.BOLD),
    )

    @Suppress("DEPRECATION")
    val SECTION_POSITIVE: TextAttributesKey = TextAttributesKey.createTextAttributesKey(
        "SPECDD_SECTION_POSITIVE",
        TextAttributes(Color(0x22863A), null, null, null, Font.BOLD),
    )

    @Suppress("DEPRECATION")
    val SECTION_NEGATIVE: TextAttributesKey = TextAttributesKey.createTextAttributesKey(
        "SPECDD_SECTION_NEGATIVE",
        TextAttributes(Color(0xD32F2F), null, null, null, Font.BOLD),
    )

    @Suppress("DEPRECATION")
    val SECTION_REQUIRED: TextAttributesKey = TextAttributesKey.createTextAttributesKey(
        "SPECDD_SECTION_REQUIRED",
        TextAttributes(Color(0xB26A00), null, null, null, Font.BOLD),
    )
    val SECTION_COLON: TextAttributesKey = TextAttributesKey.createTextAttributesKey(
        "SPECDD_SECTION_COLON",
        DefaultLanguageHighlighterColors.OPERATION_SIGN,
    )
    val SECTION_VALUE: TextAttributesKey = TextAttributesKey.createTextAttributesKey(
        "SPECDD_SECTION_VALUE",
        DefaultLanguageHighlighterColors.STRING,
    )
    val TASK_DONE: TextAttributesKey = TextAttributesKey.createTextAttributesKey(
        "SPECDD_TASK_DONE",
        DefaultLanguageHighlighterColors.STRING,
    )
    val TASK_OPEN: TextAttributesKey = TextAttributesKey.createTextAttributesKey(
        "SPECDD_TASK_OPEN",
        DefaultLanguageHighlighterColors.MARKUP_TAG,
    )

    @Suppress("DEPRECATION")
    val TASK_BLOCKED: TextAttributesKey = TextAttributesKey.createTextAttributesKey(
        "SPECDD_TASK_BLOCKED",
        TextAttributes(Color(0xD32F2F), null, null, null, Font.BOLD),
    )
    val TASK_QUESTION: TextAttributesKey = TextAttributesKey.createTextAttributesKey(
        "SPECDD_TASK_QUESTION",
        DefaultLanguageHighlighterColors.METADATA,
    )
    val TASK_SKIPPED: TextAttributesKey = TextAttributesKey.createTextAttributesKey(
        "SPECDD_TASK_SKIPPED",
        DefaultLanguageHighlighterColors.LINE_COMMENT,
    )
    val TASK_INVALID: TextAttributesKey = TextAttributesKey.createTextAttributesKey(
        "SPECDD_TASK_INVALID",
        HighlighterColors.BAD_CHARACTER,
    )
    val TASK_ID: TextAttributesKey = TextAttributesKey.createTextAttributesKey(
        "SPECDD_TASK_ID",
        DefaultLanguageHighlighterColors.NUMBER,
    )
    val SCENARIO_STEP: TextAttributesKey = TextAttributesKey.createTextAttributesKey(
        "SPECDD_SCENARIO_STEP",
        DefaultLanguageHighlighterColors.KEYWORD,
    )
    @Suppress("DEPRECATION")
    val CODE_SPAN: TextAttributesKey = TextAttributesKey.createTextAttributesKey(
        "SPECDD_CODE_SPAN",
        TextAttributes(Color(0x3A6EA5), null, null, null, Font.PLAIN),
    )

    @Suppress("DEPRECATION")
    val CODE_SPAN_DELIMITER: TextAttributesKey = TextAttributesKey.createTextAttributesKey(
        "SPECDD_CODE_SPAN_DELIMITER",
        TextAttributes(Color(0x7A828E), null, null, null, Font.PLAIN),
    )
    val PATH: TextAttributesKey = TextAttributesKey.createTextAttributesKey(
        "SPECDD_PATH",
        DefaultLanguageHighlighterColors.STRING,
    )
    val SYMBOL: TextAttributesKey = TextAttributesKey.createTextAttributesKey(
        "SPECDD_SYMBOL",
        DefaultLanguageHighlighterColors.FUNCTION_CALL,
    )
    val BAD_CHARACTER: TextAttributesKey = TextAttributesKey.createTextAttributesKey(
        "SPECDD_BAD_CHARACTER",
        HighlighterColors.BAD_CHARACTER,
    )
}
