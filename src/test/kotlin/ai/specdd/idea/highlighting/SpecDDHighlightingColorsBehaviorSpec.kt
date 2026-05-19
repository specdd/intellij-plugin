package ai.specdd.idea.highlighting

import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

class SpecDDHighlightingColorsBehaviorSpec : BehaviorSpec({
    given("SpecDD highlighting colors") {
        `when`("text attribute keys are requested") {
            then("they expose stable external names") {
                listOf(
                    SpecDDHighlightingColors.COMMENT.externalName,
                    SpecDDHighlightingColors.INDENT.externalName,
                    SpecDDHighlightingColors.CONTINUATION_TEXT.externalName,
                    SpecDDHighlightingColors.SECTION_LABEL.externalName,
                    SpecDDHighlightingColors.KEY_VALUE_KEY.externalName,
                    SpecDDHighlightingColors.SECTION_META.externalName,
                    SpecDDHighlightingColors.SECTION_POSITIVE.externalName,
                    SpecDDHighlightingColors.SECTION_NEGATIVE.externalName,
                    SpecDDHighlightingColors.SECTION_REQUIRED.externalName,
                    SpecDDHighlightingColors.SECTION_COLON.externalName,
                    SpecDDHighlightingColors.SECTION_VALUE.externalName,
                    SpecDDHighlightingColors.TASK_DONE.externalName,
                    SpecDDHighlightingColors.TASK_OPEN.externalName,
                    SpecDDHighlightingColors.TASK_BLOCKED.externalName,
                    SpecDDHighlightingColors.TASK_QUESTION.externalName,
                    SpecDDHighlightingColors.TASK_SKIPPED.externalName,
                    SpecDDHighlightingColors.TASK_INVALID.externalName,
                    SpecDDHighlightingColors.TASK_ID.externalName,
                    SpecDDHighlightingColors.SCENARIO_STEP.externalName,
                    SpecDDHighlightingColors.CODE_SPAN.externalName,
                    SpecDDHighlightingColors.CODE_SPAN_DELIMITER.externalName,
                    SpecDDHighlightingColors.PATH.externalName,
                    SpecDDHighlightingColors.SYMBOL.externalName,
                    SpecDDHighlightingColors.BAD_CHARACTER.externalName,
                ).shouldContainExactly(
                    "SPECDD_COMMENT",
                    "SPECDD_INDENT",
                    "SPECDD_CONTINUATION_TEXT",
                    "SPECDD_SECTION_LABEL",
                    "SPECDD_KEY_VALUE_KEY",
                    "SPECDD_SECTION_META",
                    "SPECDD_SECTION_POSITIVE",
                    "SPECDD_SECTION_NEGATIVE",
                    "SPECDD_SECTION_REQUIRED",
                    "SPECDD_SECTION_COLON",
                    "SPECDD_SECTION_VALUE",
                    "SPECDD_TASK_DONE",
                    "SPECDD_TASK_OPEN",
                    "SPECDD_TASK_BLOCKED",
                    "SPECDD_TASK_QUESTION",
                    "SPECDD_TASK_SKIPPED",
                    "SPECDD_TASK_INVALID",
                    "SPECDD_TASK_ID",
                    "SPECDD_SCENARIO_STEP",
                    "SPECDD_CODE_SPAN",
                    "SPECDD_CODE_SPAN_DELIMITER",
                    "SPECDD_PATH",
                    "SPECDD_SYMBOL",
                    "SPECDD_BAD_CHARACTER",
                )

                listOf(
                    SpecDDHighlightingColors.COMMENT,
                    SpecDDHighlightingColors.INDENT,
                    SpecDDHighlightingColors.CONTINUATION_TEXT,
                    SpecDDHighlightingColors.SECTION_LABEL,
                    SpecDDHighlightingColors.KEY_VALUE_KEY,
                    SpecDDHighlightingColors.SECTION_META,
                    SpecDDHighlightingColors.SECTION_POSITIVE,
                    SpecDDHighlightingColors.SECTION_NEGATIVE,
                    SpecDDHighlightingColors.SECTION_REQUIRED,
                    SpecDDHighlightingColors.SECTION_COLON,
                    SpecDDHighlightingColors.SECTION_VALUE,
                    SpecDDHighlightingColors.TASK_DONE,
                    SpecDDHighlightingColors.TASK_OPEN,
                    SpecDDHighlightingColors.TASK_BLOCKED,
                    SpecDDHighlightingColors.TASK_QUESTION,
                    SpecDDHighlightingColors.TASK_SKIPPED,
                    SpecDDHighlightingColors.TASK_INVALID,
                    SpecDDHighlightingColors.TASK_ID,
                    SpecDDHighlightingColors.SCENARIO_STEP,
                    SpecDDHighlightingColors.CODE_SPAN,
                    SpecDDHighlightingColors.CODE_SPAN_DELIMITER,
                    SpecDDHighlightingColors.PATH,
                    SpecDDHighlightingColors.SYMBOL,
                    SpecDDHighlightingColors.BAD_CHARACTER,
                ).distinct().size shouldBe 24
            }
        }

        `when`("fallbacks are requested") {
            then("they use non-deprecated platform fallback keys") {
                SpecDDHighlightingColors.INDENT.fallbackAttributeKey shouldBe
                        com.intellij.openapi.editor.DefaultLanguageHighlighterColors.LINE_COMMENT
                SpecDDHighlightingColors.CONTINUATION_TEXT.fallbackAttributeKey shouldBe
                        com.intellij.openapi.editor.DefaultLanguageHighlighterColors.DOC_COMMENT
                SpecDDHighlightingColors.SECTION_LABEL.fallbackAttributeKey shouldBe
                        com.intellij.openapi.editor.DefaultLanguageHighlighterColors.IDENTIFIER
                SpecDDHighlightingColors.SECTION_META.fallbackAttributeKey shouldBe
                        com.intellij.openapi.editor.DefaultLanguageHighlighterColors.METADATA
                SpecDDHighlightingColors.SECTION_POSITIVE.fallbackAttributeKey shouldBe
                        com.intellij.openapi.editor.DefaultLanguageHighlighterColors.MARKUP_TAG
                SpecDDHighlightingColors.SECTION_NEGATIVE.fallbackAttributeKey shouldBe
                        com.intellij.openapi.editor.DefaultLanguageHighlighterColors.IDENTIFIER
                SpecDDHighlightingColors.SECTION_REQUIRED.fallbackAttributeKey shouldBe
                        com.intellij.openapi.editor.DefaultLanguageHighlighterColors.KEYWORD
                SpecDDHighlightingColors.TASK_BLOCKED.fallbackAttributeKey shouldBe
                        com.intellij.openapi.editor.DefaultLanguageHighlighterColors.IDENTIFIER
                SpecDDHighlightingColors.CODE_SPAN.fallbackAttributeKey shouldBe
                        com.intellij.openapi.editor.DefaultLanguageHighlighterColors.INLINE_PARAMETER_HINT
                SpecDDHighlightingColors.CODE_SPAN_DELIMITER.fallbackAttributeKey shouldBe
                        com.intellij.openapi.editor.DefaultLanguageHighlighterColors.DOC_COMMENT_MARKUP
                SpecDDHighlightingColors.SYMBOL.fallbackAttributeKey shouldBe
                        com.intellij.openapi.editor.DefaultLanguageHighlighterColors.FUNCTION_CALL
                SpecDDHighlightingColors.INDENT shouldNotBe SpecDDHighlightingColors.COMMENT
                SpecDDHighlightingColors.CONTINUATION_TEXT shouldNotBe SpecDDHighlightingColors.COMMENT
                SpecDDHighlightingColors.CONTINUATION_TEXT shouldNotBe SpecDDHighlightingColors.INDENT
            }
        }

        `when`("semantic section group keys are requested") {
            then("they provide distinct grouped keys") {
                SpecDDHighlightingColors.KEY_VALUE_KEY shouldNotBe SpecDDHighlightingColors.SECTION_LABEL
                SpecDDHighlightingColors.SECTION_META shouldNotBe SpecDDHighlightingColors.SECTION_LABEL
            }
        }

        `when`("invalid task marker defaults are requested") {
            then("they use the platform bad-character fallback") {
                SpecDDHighlightingColors.TASK_INVALID.fallbackAttributeKey shouldBe
                        com.intellij.openapi.editor.HighlighterColors.BAD_CHARACTER
            }
        }
    }
})
