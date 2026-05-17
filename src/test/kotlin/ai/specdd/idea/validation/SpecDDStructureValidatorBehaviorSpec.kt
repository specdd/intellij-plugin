package ai.specdd.idea.validation

import com.intellij.openapi.util.TextRange
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe

class SpecDDStructureValidatorBehaviorSpec : BehaviorSpec({
    given("a SpecDD structure validator") {
        val validator = SpecDDStructureValidator()

        `when`("a valid kitchen-sink style snippet is validated") {
            then("it reports no issues") {
                val result = validator.validate(
                    """
                    |# comment
                    |
                    |Spec: Example
                    |Purpose:
                    |  Keep validation independent from highlighting.
                    |Tasks:
                    |  [ ] #1 Add validation tests.
                    |Scenario: valid flow
                    |  Given a spec
                    |  When validation runs
                    |  Then no issue is reported
                    """.trimMargin(),
                )

                result.isValid shouldBe true
                result.issues shouldBe emptyList()
            }
        }

        `when`("unknown and misspelled sections are validated") {
            then("it reports unknown section names with likely typo suggestions") {
                val text = """
                    |Spec: Example
                    |Porpose:
                    |Completely Unknown:
                    |Not exists:
                    |lowercase ignored:
                    |Path: src/main.sdd
                """.trimMargin()

                validator.validate(text).issues.shouldContainExactly(
                    issueAt(text, "Porpose", "Unknown SpecDD section 'Porpose'.", "Purpose"),
                    issueAt(text, "Completely Unknown", "Unknown SpecDD section 'Completely Unknown'."),
                    issueAt(text, "Not exists", "Unknown SpecDD section 'Not exists'."),
                    issueAt(text, "lowercase ignored", "Unknown SpecDD section 'lowercase ignored'."),
                    issueAt(text, "Path", "Unknown SpecDD section 'Path'."),
                )
            }
        }

        `when`("known section labels miss the colon") {
            then("it reports missing section separators") {
                validator.validate("Spec\nPurpose").issues.shouldContainExactly(
                    SpecDDValidationIssue(TextRange(0, 4), "Section 'Spec' is missing ':'."),
                    SpecDDValidationIssue(TextRange(5, 12), "Section 'Purpose' is missing ':'."),
                )
            }
        }

        `when`("known section labels have whitespace before the colon") {
            then("it reports the label as missing the required separator") {
                validator.validate("Spec : Example").issues.shouldContainExactly(
                    SpecDDValidationIssue(TextRange(0, 4), "Section 'Spec' is missing ':'."),
                )
            }
        }

        `when`("inline section values miss the required post-colon space") {
            then("it reports section value separator issues") {
                val text = """
                    |Spec:Name
                    |Platform:IntelliJ Platform / Kotlin
                    |Scenario:valid flow
                """.trimMargin()

                validator.validate(text).issues.shouldContainExactly(
                    issueAt(text, "Name", "Inline value for section 'Spec' must be separated from ':' by a space."),
                    issueAt(
                        text,
                        "IntelliJ Platform / Kotlin",
                        "Inline value for section 'Platform' must be separated from ':' by a space.",
                    ),
                    issueAt(
                        text,
                        "valid flow",
                        "Inline value for section 'Scenario' must be separated from ':' by a space.",
                    ),
                )
            }
        }

        `when`("required inline section values are empty") {
            then("it reports missing inline values") {
                val text = """
                    |Spec:
                    |Platform:
                    |Purpose:
                """.trimMargin()

                validator.validate(text).issues.shouldContainExactly(
                    SpecDDValidationIssue(
                        range = TextRange(
                            text.indexOf("Spec:") + "Spec".length,
                            text.indexOf("Spec:") + "Spec:".length
                        ),
                        message = "Section 'Spec' requires an inline value.",
                    ),
                    SpecDDValidationIssue(
                        range = TextRange(
                            text.indexOf("Platform:") + "Platform".length,
                            text.indexOf("Platform:") + "Platform:".length,
                        ),
                        message = "Section 'Platform' requires an inline value.",
                    ),
                )
            }
        }

        `when`("required inline section values contain only spaces") {
            then("it reports the blank inline value range") {
                val text = "Spec:  \nPlatform:  \nPurpose:"

                validator.validate(text).issues.shouldContainExactly(
                    SpecDDValidationIssue(
                        range = TextRange(text.indexOf("Spec:") + "Spec:".length, text.indexOf("Platform:") - 1),
                        message = "Section 'Spec' requires an inline value.",
                    ),
                    SpecDDValidationIssue(
                        range = TextRange(text.indexOf("Platform:") + "Platform:".length, text.indexOf("Purpose:") - 1),
                        message = "Section 'Platform' requires an inline value.",
                    ),
                )
            }
        }

        `when`("block sections contain inline text after the colon") {
            then("it reports unsupported section follow-up text") {
                val text = """
                    |Spec: Example
                    |Platform: IntelliJ Platform / Kotlin
                    |Scenario: inline scenario titles are supported
                    |Can modify: bla bla bla
                    |Purpose: this should move to the next line
                    |Must:
                """.trimMargin()

                validator.validate(text).issues.shouldContainExactly(
                    issueAt(text, "bla bla bla", "Section 'Can modify' does not support inline text after ':'."),
                    issueAt(
                        text,
                        "this should move to the next line",
                        "Section 'Purpose' does not support inline text after ':'.",
                    ),
                )
            }
        }

        `when`("bodyless sections contain follow-up lines") {
            then("it reports unsupported section body text") {
                val text = """
                    |Spec: Example
                    |  this is not allowed under spec
                    |Platform: SpecDD/1.2
                    |  this is totally not allowed
                    |Purpose:
                """.trimMargin()

                validator.validate(text).issues.shouldContainExactly(
                    issueAt(text, "this is not allowed under spec", "Section 'Spec' does not support follow-up lines."),
                    issueAt(
                        text,
                        "this is totally not allowed",
                        "Section 'Platform' does not support follow-up lines."
                    ),
                )
            }
        }

        `when`("a section receives an unsupported body line kind") {
            then("it reports invalid syntax") {
                val text = """
                    |Spec: Example
                    |Tasks:
                    |  this is not a task
                    |  [ ] This task is valid.
                """.trimMargin()

                validator.validate(text).issues.shouldContainExactly(
                    issueAt(text, "this is not a task", "Invalid SpecDD syntax."),
                )
            }
        }

        `when`("non-repeatable sections are repeated") {
            then("it reports duplicate section issues") {
                val text = """
                    |Spec: Example
                    |Purpose:
                    |  First purpose.
                    |Scenario: first valid repeatable section
                    |Example:
                    |  First example.
                    |Scenario: second valid repeatable section
                    |Example:
                    |  Second example.
                    |Purpose:
                    |  Repeated purpose.
                    |Must:
                    |  First must.
                    |Must:
                    |  Repeated must.
                """.trimMargin()

                validator.validate(text).issues.shouldContainExactly(
                    SpecDDValidationIssue(
                        range = TextRange(text.lastIndexOf("Purpose"), text.lastIndexOf("Purpose") + "Purpose".length),
                        message = "Section 'Purpose' must not be repeated.",
                    ),
                    SpecDDValidationIssue(
                        range = TextRange(text.lastIndexOf("Must"), text.lastIndexOf("Must") + "Must".length),
                        message = "Section 'Must' must not be repeated.",
                    ),
                )
            }
        }

        `when`("scenario sections repeat an inline value") {
            then("it reports the second matching scenario as a duplicate") {
                val text = """
                    |Spec: Example
                    |Scenario: foo
                    |  Given one flow
                    |Scenario: bar
                    |  Given a different flow
                    |Scenario: foo
                    |  Given a repeated flow
                    |Example:
                    |  First example.
                    |Example:
                    |  Second example.
                """.trimMargin()
                val duplicateScenario = text.lastIndexOf("Scenario")

                validator.validate(text).issues.shouldContainExactly(
                    SpecDDValidationIssue(
                        range = TextRange(duplicateScenario, duplicateScenario + "Scenario".length),
                        message = "Scenario 'foo' must not be repeated.",
                    ),
                )
            }
        }

        `when`("line indentation is not a multiple of two spaces") {
            then("it reports indentation issues") {
                val text = "Spec: Example\nPurpose:\n   odd\n\tbad\n    ok\n"

                validator.validate(text).issues.shouldContainExactly(
                    SpecDDValidationIssue(
                        range = TextRange(23, 26),
                        message = "Indentation must use spaces in multiples of 2.",
                    ),
                    SpecDDValidationIssue(
                        range = TextRange(30, 31),
                        message = "Indentation must use spaces in multiples of 2.",
                    ),
                )
            }
        }

        `when`("section headers are indented") {
            then("it reports section indentation issues") {
                validator.validate("Spec: Example\n  Purpose:\n").issues.shouldContainExactly(
                    SpecDDValidationIssue(
                        range = TextRange(14, 16),
                        message = "Section headers must start at column 0.",
                    ),
                )
            }
        }

        `when`("invalid task states are validated") {
            then("it reports invalid task state issues") {
                val text = """
                    |Spec: Example
                    |Tasks:
                    |  [invalid] #1 Unsupported state.
                    |  [z] #2 Unsupported short state.
                """.trimMargin()

                validator.validate(text).issues.shouldContainExactly(
                    issueAt(text, "[invalid]", "Invalid SpecDD task state '[invalid]'."),
                    issueAt(text, "[z]", "Invalid SpecDD task state '[z]'."),
                )
            }
        }

        `when`("the first known section is not Spec") {
            then("it reports the structure issue after section-name issues are sorted") {
                validator.validate(
                    """
                    |Purpose:
                    |Spec: Example
                    """.trimMargin(),
                ).issues.shouldContainExactly(
                    SpecDDValidationIssue(
                        range = TextRange(0, 7),
                        message = "SpecDD files should start with the Spec section.",
                        suggestion = "Spec",
                    ),
                )
            }
        }

        `when`("text has carriage-return line separators") {
            then("it validates each line") {
                val text = "Spec: Example\rPorpose:\r"

                validator.validate(text).issues.shouldContainExactly(
                    issueAt(text, "Porpose", "Unknown SpecDD section 'Porpose'.", "Purpose"),
                )
            }
        }

        `when`("text has carriage-return-line-feed separators") {
            then("it validates each line") {
                validator.validate("Spec: Example\r\nPurpose\r\n").issues.shouldContainExactly(
                    SpecDDValidationIssue(TextRange(15, 22), "Section 'Purpose' is missing ':'."),
                )
            }
        }

        `when`("text has no sections") {
            then("it reports no structure issue") {
                validator.validate("plain text\n# comment\n").issues shouldBe emptyList()
            }
        }

        `when`("empty text is validated") {
            then("it reports no issues") {
                validator.validate("").issues shouldBe emptyList()
            }
        }
    }
})

private fun issueAt(
    text: String,
    fragment: String,
    message: String,
    suggestion: String? = null,
): SpecDDValidationIssue {
    val start = text.indexOf(fragment)
    return SpecDDValidationIssue(TextRange(start, start + fragment.length), message, suggestion)
}
