package ai.specdd.idea.validation

import ai.specdd.idea.parser.SpecDDLanguageFacts
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe

class SpecDDKnownSectionsBehaviorSpec : BehaviorSpec({
    given("the known section registry") {
        `when`("known labels are requested") {
            then("it mirrors the shared language facts") {
                SpecDDKnownSections.labels shouldBe SpecDDLanguageFacts.sectionLabels.toSet()
            }
        }

        `when`("a label is checked") {
            then("it distinguishes known and unknown section labels") {
                SpecDDKnownSections.isKnown("Purpose") shouldBe true
                SpecDDKnownSections.isKnown("Porpose") shouldBe false
            }
        }

        `when`("a likely typo is checked") {
            then("it returns the closest known section") {
                SpecDDKnownSections.closestLabel("Porpose") shouldBe "Purpose"
                SpecDDKnownSections.closestLabel("Mustnt") shouldBe "Must not"
            }
        }

        `when`("an unrelated label is checked") {
            then("it does not invent a suggestion") {
                SpecDDKnownSections.closestLabel("Completely Unknown").shouldBeNull()
            }
        }

        `when`("an empty label is checked") {
            then("it does not match known sections") {
                SpecDDKnownSections.closestLabel("").shouldBeNull()
            }
        }
    }
})
