package ai.specdd.idea.validation

import com.intellij.openapi.util.TextRange
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe

class SpecDDValidationResultBehaviorSpec : BehaviorSpec({
    given("a validation result") {
        `when`("it has no issues") {
            then("it is valid") {
                SpecDDValidationResult(emptyList()).isValid shouldBe true
            }
        }

        `when`("it has at least one issue") {
            then("it is not valid") {
                val issue = SpecDDValidationIssue(TextRange(0, 4), "Unknown SpecDD section 'Specx'.", "Spec")

                SpecDDValidationResult(listOf(issue)).isValid shouldBe false
                issue.displayMessage shouldBe "Unknown SpecDD section 'Specx'. Did you mean 'Spec'?"
            }
        }

        `when`("an issue has no suggestion") {
            then("its display message is the base message") {
                SpecDDValidationIssue(TextRange(0, 4), "Section 'Spec' is missing ':'.").displayMessage shouldBe
                        "Section 'Spec' is missing ':'."
            }
        }
    }
})
