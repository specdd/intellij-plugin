package ai.specdd.idea.highlighting

import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import java.awt.Color
import java.awt.Font

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

        `when`("indent defaults are requested") {
            then("they provide a muted non-comment color") {
                val attributes = SpecDDHighlightingColors.INDENT.defaultAttributes

                attributes.foregroundColor shouldBe Color(0x6E7781)
                attributes.effectColor.shouldBeNull()
                attributes.effectType.shouldBeNull()
                attributes.fontType shouldBe Font.PLAIN
                SpecDDHighlightingColors.INDENT shouldNotBe SpecDDHighlightingColors.COMMENT
            }
        }

        `when`("continuation text defaults are requested") {
            then("they provide a muted visible non-comment color") {
                val attributes = SpecDDHighlightingColors.CONTINUATION_TEXT.defaultAttributes

                attributes.foregroundColor shouldBe Color(0x7A828E)
                attributes.effectColor.shouldBeNull()
                attributes.effectType.shouldBeNull()
                attributes.fontType shouldBe Font.PLAIN
                SpecDDHighlightingColors.CONTINUATION_TEXT shouldNotBe SpecDDHighlightingColors.COMMENT
                SpecDDHighlightingColors.CONTINUATION_TEXT shouldNotBe SpecDDHighlightingColors.INDENT
            }
        }

        `when`("semantic section group defaults are requested") {
            then("they provide distinct grouped attributes") {
                SpecDDHighlightingColors.SECTION_LABEL.defaultAttributes.foregroundColor shouldBe Color(0x58A6FF)
                SpecDDHighlightingColors.SECTION_LABEL.defaultAttributes.fontType shouldBe Font.BOLD
                SpecDDHighlightingColors.SECTION_META.defaultAttributes.foregroundColor shouldBe Color(0x7A004B)
                SpecDDHighlightingColors.SECTION_META.defaultAttributes.fontType shouldBe Font.BOLD
                SpecDDHighlightingColors.SECTION_POSITIVE.defaultAttributes.foregroundColor shouldBe Color(0x22863A)
                SpecDDHighlightingColors.SECTION_POSITIVE.defaultAttributes.fontType shouldBe Font.BOLD
                SpecDDHighlightingColors.SECTION_NEGATIVE.defaultAttributes.foregroundColor shouldBe Color(0xD32F2F)
                SpecDDHighlightingColors.SECTION_NEGATIVE.defaultAttributes.fontType shouldBe Font.BOLD
                SpecDDHighlightingColors.SECTION_REQUIRED.defaultAttributes.foregroundColor shouldBe Color(0xB26A00)
                SpecDDHighlightingColors.SECTION_REQUIRED.defaultAttributes.fontType shouldBe Font.BOLD
                SpecDDHighlightingColors.KEY_VALUE_KEY shouldNotBe SpecDDHighlightingColors.SECTION_LABEL
                SpecDDHighlightingColors.SECTION_META shouldNotBe SpecDDHighlightingColors.SECTION_LABEL
            }
        }

        `when`("blocked task marker defaults are requested") {
            then("they provide color without diagnostic underline") {
                val attributes = SpecDDHighlightingColors.TASK_BLOCKED.defaultAttributes

                attributes.foregroundColor shouldBe Color(0xD32F2F)
                attributes.effectColor.shouldBeNull()
                attributes.effectType.shouldBeNull()
                attributes.fontType shouldBe Font.BOLD
            }
        }

        `when`("inline code defaults are requested") {
            then("they provide readable code text and quieter delimiters") {
                val codeAttributes = SpecDDHighlightingColors.CODE_SPAN.defaultAttributes
                val delimiterAttributes = SpecDDHighlightingColors.CODE_SPAN_DELIMITER.defaultAttributes

                codeAttributes.foregroundColor shouldBe Color(0x3A6EA5)
                codeAttributes.fontType shouldBe Font.PLAIN
                delimiterAttributes.foregroundColor shouldBe Color(0x7A828E)
                delimiterAttributes.fontType shouldBe Font.PLAIN
                SpecDDHighlightingColors.CODE_SPAN shouldNotBe SpecDDHighlightingColors.CODE_SPAN_DELIMITER
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
