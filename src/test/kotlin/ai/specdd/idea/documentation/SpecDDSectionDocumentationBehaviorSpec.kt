package ai.specdd.idea.documentation

import ai.specdd.idea.parser.SpecDDLanguageFacts
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldNotContain

class SpecDDSectionDocumentationBehaviorSpec : BehaviorSpec({
    given("SpecDD section documentation") {
        `when`("known section labels are inspected") {
            then("it covers every parser-known section label") {
                SpecDDSectionDocumentation.coversKnownSections() shouldBe true
                SpecDDSectionDocumentation.labels().shouldContainExactlyInAnyOrder(SpecDDLanguageFacts.sectionLabels)
            }
        }

        `when`("documentation is rendered") {
            then("it returns paragraph-based escaped HTML") {
                val html = SpecDDSectionDocumentation.htmlFor("Can modify")

                html shouldContain "<b>Can modify</b>"
                html shouldContain "<p>Lists the files or paths an agent is allowed to change"
                html shouldContain "Use this section as write authority."
                html.shouldNotContain("<script")
            }
        }

        `when`("resource-style documentation text is parsed") {
            then("it preserves labels and paragraph breaks") {
                val parsed = SpecDDSectionDocumentation.parseDescriptions(
                    """
                    |## A <label>
                    |First paragraph.
                    |
                    |Second paragraph.
                    |## Next
                    |Other text.
                    """.trimMargin(),
                )

                parsed["A <label>"] shouldBe "First paragraph.\n\nSecond paragraph."
                parsed["Next"] shouldBe "Other text."
            }
        }

        `when`("an unknown section is requested") {
            then("it returns no documentation") {
                SpecDDSectionDocumentation.htmlFor("Not exists").shouldBeNull()
            }
        }
    }
})
