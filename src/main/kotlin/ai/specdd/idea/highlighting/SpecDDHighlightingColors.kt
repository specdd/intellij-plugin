package ai.specdd.idea.highlighting

import com.intellij.openapi.editor.DefaultLanguageHighlighterColors
import com.intellij.openapi.editor.HighlighterColors
import com.intellij.openapi.editor.colors.TextAttributesKey

object SpecDDHighlightingColors {
    val COMMENT: TextAttributesKey = TextAttributesKey.createTextAttributesKey(
        "SPECDD_COMMENT",
        DefaultLanguageHighlighterColors.LINE_COMMENT,
    )

    val INDENT: TextAttributesKey = TextAttributesKey.createTextAttributesKey(
        "SPECDD_INDENT",
        DefaultLanguageHighlighterColors.LINE_COMMENT,
    )

    val CONTINUATION_TEXT: TextAttributesKey = TextAttributesKey.createTextAttributesKey(
        "SPECDD_CONTINUATION_TEXT",
        DefaultLanguageHighlighterColors.DOC_COMMENT,
    )

    val SECTION_LABEL: TextAttributesKey = TextAttributesKey.createTextAttributesKey(
        "SPECDD_SECTION_LABEL",
        DefaultLanguageHighlighterColors.IDENTIFIER,
    )
    val KEY_VALUE_KEY: TextAttributesKey = TextAttributesKey.createTextAttributesKey(
        "SPECDD_KEY_VALUE_KEY",
        DefaultLanguageHighlighterColors.INSTANCE_FIELD,
    )

    val SECTION_META: TextAttributesKey = TextAttributesKey.createTextAttributesKey(
        "SPECDD_SECTION_META",
        DefaultLanguageHighlighterColors.METADATA,
    )

    val SECTION_POSITIVE: TextAttributesKey = TextAttributesKey.createTextAttributesKey(
        "SPECDD_SECTION_POSITIVE",
        DefaultLanguageHighlighterColors.MARKUP_TAG,
    )

    val SECTION_NEGATIVE: TextAttributesKey = TextAttributesKey.createTextAttributesKey(
        "SPECDD_SECTION_NEGATIVE",
        DefaultLanguageHighlighterColors.IDENTIFIER,
    )

    val SECTION_REQUIRED: TextAttributesKey = TextAttributesKey.createTextAttributesKey(
        "SPECDD_SECTION_REQUIRED",
        DefaultLanguageHighlighterColors.KEYWORD,
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

    val TASK_BLOCKED: TextAttributesKey = TextAttributesKey.createTextAttributesKey(
        "SPECDD_TASK_BLOCKED",
        DefaultLanguageHighlighterColors.IDENTIFIER,
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
    val CODE_SPAN: TextAttributesKey = TextAttributesKey.createTextAttributesKey(
        "SPECDD_CODE_SPAN",
        DefaultLanguageHighlighterColors.INLINE_PARAMETER_HINT,
    )

    val CODE_SPAN_DELIMITER: TextAttributesKey = TextAttributesKey.createTextAttributesKey(
        "SPECDD_CODE_SPAN_DELIMITER",
        DefaultLanguageHighlighterColors.DOC_COMMENT_MARKUP,
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
