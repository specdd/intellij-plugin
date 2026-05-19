package ai.specdd.idea.parser

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe

class SpecDDLineClassifierBehaviorSpec : BehaviorSpec({
    given("a SpecDD line classifier") {
        val classifier = SpecDDLineClassifier()

        `when`("a blank line is classified") {
            then("it reports a blank classification") {
                classifier.classify("  ", 0, 2) shouldBe
                        SpecDDLineClassification.blank(lineStart = 0, lineEnd = 2)
            }
        }

        `when`("a whole-line comment is classified") {
            then("it reports the comment content start") {
                classifier.classify("  # comment", 0, 11) shouldBe
                        SpecDDLineClassification.comment(lineStart = 0, lineEnd = 11, contentStart = 2)
            }
        }

        `when`("a known section header with a value is classified") {
            then("it reports section label, colon, and value offsets") {
                classifier.classify("  Must not: add settings", 0, 24) shouldBe
                        SpecDDLineClassification.section(
                            lineStart = 0,
                            lineEnd = 24,
                            contentStart = 2,
                            sectionHeader = SpecDDSectionHeader(
                                label = "Must not",
                                labelStart = 2,
                                labelEnd = 10,
                                colonStart = 10,
                                valueStart = 11,
                                valueEnd = 24,
                            ),
                        )
            }
        }

        `when`("a known section header without a value is classified") {
            then("it reports an empty value range") {
                classifier.classify("Spec:", 0, 5) shouldBe
                        SpecDDLineClassification.section(
                            lineStart = 0,
                            lineEnd = 5,
                            contentStart = 0,
                            sectionHeader = SpecDDSectionHeader(
                                label = "Spec",
                                labelStart = 0,
                                labelEnd = 4,
                                colonStart = 4,
                                valueStart = 5,
                                valueEnd = 5,
                            ),
                        )
            }
        }

        `when`("a task marker with a numeric id is classified") {
            then("it reports the marker and task id ranges") {
                classifier.classify("  [x] #12 Done", 0, 14, "Tasks") shouldBe
                        SpecDDLineClassification.task(
                            lineStart = 0,
                            lineEnd = 14,
                            contentStart = 2,
                            taskMarker = SpecDDTaskMarker(2, 5, SpecDDTaskStatus.DONE, SpecDDTaskId(6, 9)),
                        )
            }
        }

        `when`("a continuation line is classified") {
            then("it reports continuation before task, scenario, key-value, and text classifications") {
                val taskLike = "    [ ] not a task at continuation indent"
                classifier.classify(taskLike, 0, taskLike.length) shouldBe
                        SpecDDLineClassification.continuation(lineStart = 0, lineEnd = taskLike.length, contentStart = 4)

                val stepLike = "    Given not a step at continuation indent"
                classifier.classify(stepLike, 0, stepLike.length) shouldBe
                        SpecDDLineClassification.continuation(lineStart = 0, lineEnd = stepLike.length, contentStart = 4)

                val keyValueLike = "    key: value"
                classifier.classify(keyValueLike, 0, keyValueLike.length) shouldBe
                        SpecDDLineClassification.continuation(
                            lineStart = 0,
                            lineEnd = keyValueLike.length,
                            contentStart = 4,
                        )
            }
        }

        `when`("a question task marker with a numeric id is classified") {
            then("it remains a task instead of a comment") {
                val line =
                    "  [?] #7 Decide whether fixtures should include intentionally invalid examples in a separate file."

                classifier.classify(line, 0, line.length, "Tasks") shouldBe
                        SpecDDLineClassification.task(
                            lineStart = 0,
                            lineEnd = line.length,
                            contentStart = 2,
                            taskMarker = SpecDDTaskMarker(2, 5, SpecDDTaskStatus.QUESTION, SpecDDTaskId(6, 8)),
                        )
            }
        }

        `when`("a task marker has no numeric id") {
            then("it reports only the marker range") {
                classifier.classify("[-] skipped", 0, 11, "Tasks") shouldBe
                        SpecDDLineClassification.task(
                            lineStart = 0,
                            lineEnd = 11,
                            contentStart = 0,
                            taskMarker = SpecDDTaskMarker(0, 3, SpecDDTaskStatus.SKIPPED, null),
                        )
            }
        }

        `when`("a task marker has an empty hash id") {
            then("it ignores the id range") {
                classifier.classify("[!] # blocked", 0, 13, "Tasks") shouldBe
                        SpecDDLineClassification.task(
                            lineStart = 0,
                            lineEnd = 13,
                            contentStart = 0,
                            taskMarker = SpecDDTaskMarker(0, 3, SpecDDTaskStatus.BLOCKED, null),
                        )
            }
        }

        `when`("a scenario step is classified") {
            then("it reports the step keyword range") {
                classifier.classify("  Given a spec", 0, 14) shouldBe
                        SpecDDLineClassification.scenarioStep(
                            lineStart = 0,
                            lineEnd = 14,
                            contentStart = 2,
                            scenarioStep = SpecDDScenarioStep("Given", 2, 7),
                        )
            }
        }

        `when`("a line starts with a step prefix inside a longer word") {
            then("it remains text") {
                classifier.classify("Andromeda is not a step", 0, 23) shouldBe
                        SpecDDLineClassification.text(lineStart = 0, lineEnd = 23, contentStart = 0)
            }
        }

        `when`("a generic key-value line is classified") {
            then("it reports the key, colon, and value offsets") {
                classifier.classify("  glob path: generated/*.json", 0, 29) shouldBe
                        SpecDDLineClassification.keyValue(
                            lineStart = 0,
                            lineEnd = 29,
                            contentStart = 2,
                            keyValue = SpecDDKeyValue(
                                keyStart = 2,
                                keyEnd = 11,
                                colonStart = 11,
                                valueStart = 12,
                                valueEnd = 29,
                            ),
                        )
            }
        }

        `when`("a generic key-value line uses uppercase, Unicode, and punctuation in the key") {
            then("it accepts the first colon followed by a space as the key-value separator") {
                val line = "  Output/Δ Path!: generated/Δ.json"

                classifier.classify(line, 0, line.length) shouldBe
                        SpecDDLineClassification.keyValue(
                            lineStart = 0,
                            lineEnd = line.length,
                            contentStart = 2,
                            keyValue = SpecDDKeyValue(
                                keyStart = 2,
                                keyEnd = 16,
                                colonStart = 16,
                                valueStart = 17,
                                valueEnd = line.length,
                            ),
                        )
            }
        }

        `when`("a generic key-value line does not have a space after the colon") {
            then("it remains text") {
                classifier.classify("path:generated/*.json", 0, 21) shouldBe
                        SpecDDLineClassification.text(lineStart = 0, lineEnd = 21, contentStart = 0)
            }
        }

        `when`("a generic key-value line has an empty or whitespace-ended key") {
            then("it remains text") {
                classifier.classify(": value", 0, 7) shouldBe
                        SpecDDLineClassification.text(lineStart = 0, lineEnd = 7, contentStart = 0)

                classifier.classify("path : value", 0, 12) shouldBe
                        SpecDDLineClassification.text(lineStart = 0, lineEnd = 12, contentStart = 0)
            }
        }

        `when`("an unsupported task marker is classified") {
            then("it reports an invalid task marker") {
                classifier.classify("  [invalid] #9 Fix state", 0, 24, "Tasks") shouldBe
                        SpecDDLineClassification.task(
                            lineStart = 0,
                            lineEnd = 24,
                            contentStart = 2,
                            taskMarker = SpecDDTaskMarker(2, 11, SpecDDTaskStatus.INVALID, SpecDDTaskId(12, 14)),
                        )
            }
        }

        `when`("a task marker candidate is not closed before whitespace") {
            then("it remains ordinary text") {
                classifier.classify("  [invalid task marker", 0, 21, "Tasks") shouldBe
                        SpecDDLineClassification.text(lineStart = 0, lineEnd = 21, contentStart = 2)
            }
        }

        `when`("a task-looking line is classified outside Tasks") {
            then("it remains ordinary text") {
                classifier.classify("  [ ] ordinary body text", 0, 24, "Purpose") shouldBe
                        SpecDDLineClassification.text(lineStart = 0, lineEnd = 24, contentStart = 2)
            }
        }

        `when`("a line is not recognized as SpecDD structure") {
            then("it remains text") {
                classifier.classify("  unknown text", 0, 14) shouldBe
                        SpecDDLineClassification.text(lineStart = 0, lineEnd = 14, contentStart = 2)
            }
        }

        `when`("an unsupported task marker body is mapped directly") {
            then("it rejects the marker body") {
                shouldThrow<IllegalStateException> {
                    SpecDDTaskStatus.fromMarkerBody('z')
                }
            }
        }
    }
})
