package ai.specdd.idea.parser

import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.collections.shouldContainExactly

class SpecDDLanguageFactsBehaviorSpec : BehaviorSpec({
    given("SpecDD language facts") {
        `when`("known section labels are requested") {
            then("they expose the supported section vocabulary in matching order") {
                SpecDDLanguageFacts.sectionLabels.shouldContainExactly(
                    "Can modify",
                    "Can read",
                    "Must not",
                    "Depends on",
                    "Done when",
                    "References",
                    "Structure",
                    "Platform",
                    "Purpose",
                    "Forbids",
                    "Exposes",
                    "Accepts",
                    "Returns",
                    "Raises",
                    "Handles",
                    "Scenario",
                    "Example",
                    "Spec",
                    "Owns",
                    "Must",
                    "Tasks",
                )
            }
        }

        `when`("scenario steps are requested") {
            then("they expose the supported Gherkin-like keywords") {
                SpecDDLanguageFacts.scenarioSteps.shouldContainExactly("Given", "When", "Then", "And", "But")
            }
        }

        `when`("task marker bodies are requested") {
            then("they expose the supported task states") {
                SpecDDLanguageFacts.taskMarkerBodies.shouldContainExactly(' ', 'x', 'X', '-', '!', '?')
            }
        }
    }
})
