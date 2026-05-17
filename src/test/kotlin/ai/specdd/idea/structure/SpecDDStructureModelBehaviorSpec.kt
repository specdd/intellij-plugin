package ai.specdd.idea.structure

import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe

class SpecDDStructureModelBehaviorSpec : BehaviorSpec({
    given("a SpecDD structure model") {
        val model = SpecDDStructureModel()

        `when`("section headers are extracted") {
            then("it returns all sections in file order with display names") {
                val text = """
                    |# comment
                    |Spec: Example
                    |Purpose:
                    |  Keep structure visible.
                    |Scenario: first
                    |  Given a spec
                    |Scenario: second
                    |Example:
                    |  output: generated/result.json
                """.trimMargin()

                model.sections(text).map { section -> section.displayName }.shouldContainExactly(
                    "Spec: Example",
                    "Purpose",
                    "Scenario: first",
                    "Scenario: second",
                    "Example",
                )
            }
        }

        `when`("text uses carriage-return line endings") {
            then("it still stores section offsets") {
                val text = "Spec: Example\r\nPurpose:\rScenario: demo\r"

                model.sections(text).shouldContainExactly(
                    SpecDDStructureSection("Spec", "Example", 0, 0, 13),
                    SpecDDStructureSection("Purpose", "", 15, 15, 23),
                    SpecDDStructureSection("Scenario", "demo", 24, 24, 38),
                )
            }
        }

        `when`("text has no sections") {
            then("it returns no structure entries") {
                model.sections("plain text\n  [ ] body without section").shouldBe(emptyList())
            }
        }
    }
})
