package ai.specdd.idea.references

import com.intellij.openapi.util.TextRange
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.collections.shouldContainExactly

class SpecDDPathReferenceExtractorBehaviorSpec : BehaviorSpec({
    given("a SpecDD path reference extractor") {
        val extractor = SpecDDPathReferenceExtractor()

        `when`("path-bearing sections contain exact paths and globs") {
            then("it extracts body paths and key-value keys with file ranges") {
                val text = """
                    |Spec: Demo
                    |Structure:
                    |  src/main: Main sources
                    |Owns:
                    |  fixtures/*.sdd
                    |Purpose:
                    |  src/not-a-path
                    |References:
                    |  docs/readme.md
                    |  # ignored comment
                    |  [ ] ignored task
                    |  Given ignored step
                    |  generated: ignored value text
                    """.trimMargin()

                extractor.extract(text).shouldContainExactly(
                    SpecDDPathCandidate("src/main", TextRange(24, 32), true),
                    SpecDDPathCandidate("fixtures/*.sdd", TextRange(55, 69), true),
                    SpecDDPathCandidate("src/not-a-path", TextRange(81, 95), true),
                    SpecDDPathCandidate("docs/readme.md", TextRange(110, 124), true),
                    SpecDDPathCandidate("generated", TextRange(187, 196), true),
                )
            }
        }

        `when`("non-path-bearing sections contain path-shaped text") {
            then("it extracts inline paths") {
                val text = """
                    |Spec: Demo
                    |Must:
                    |  src/main
                    |Done when:
                    |  docs/readme.md
                    """.trimMargin()

                extractor.extract(text).shouldContainExactly(
                    SpecDDPathCandidate("src/main", TextRange(19, 27), true),
                    SpecDDPathCandidate("docs/readme.md", TextRange(41, 55), true),
                )
            }
        }

        `when`("scenario steps contain paths and globs") {
            then("it extracts inline references from step text") {
                val text = """
                    |Scenario: write authority remains local
                    |  Given this fixture owns kitchen-sink.sdd and generated/*
                    |  And ../README.md and ../src/.specdd/bootstrap.md may be read
                    """.trimMargin()

                extractor.extract(text).shouldContainExactly(
                    SpecDDPathCandidate("kitchen-sink.sdd", TextRange(66, 82), true),
                    SpecDDPathCandidate("generated/*", TextRange(87, 98), true),
                    SpecDDPathCandidate("../README.md", TextRange(105, 117), true),
                    SpecDDPathCandidate("../src/.specdd/bootstrap.md", TextRange(122, 149), true),
                )
            }
        }

        `when`("path-bearing key-value sections use bare names") {
            then("it treats the keys as concrete path candidates") {
                val text = """
                    |Structure:
                    |  parser: Shared parser
                """.trimMargin()

                extractor.extract(text).shouldContainExactly(
                    SpecDDPathCandidate("parser", TextRange(13, 19), true),
                )
            }
        }

        `when`("path candidates use trailing whitespace and CRLF line endings") {
            then("it trims candidate ranges and advances over CRLF") {
                val text = "References:\r\n  docs/readme.md  \r\n  src/*"

                extractor.extract(text).shouldContainExactly(
                    SpecDDPathCandidate("docs/readme.md", TextRange(15, 29), true),
                    SpecDDPathCandidate("src/*", TextRange(35, 40), true),
                )
            }
        }

        `when`("path candidates use CR line endings") {
            then("it advances over CR") {
                val text = "References:\r  docs/readme.md"

                extractor.extract(text).shouldContainExactly(
                    SpecDDPathCandidate("docs/readme.md", TextRange(14, 28), true),
                )
            }
        }
    }
})
