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
                    |  ./src/main: Main sources
                    |Owns:
                    |  ./fixtures/*.sdd
                    |Purpose:
                    |  src/not-a-path
                    |References:
                    |  ./docs/readme.md
                    |  # ignored comment
                    |  [ ] check ./generated/report.json
                    |  Given ignored step
                    |  ./generated: ignored value text
                    """.trimMargin()

                extractor.extract(text).shouldContainExactly(
                    SpecDDPathCandidate("./src/main", TextRange(24, 34), true),
                    SpecDDPathCandidate("./fixtures/*.sdd", TextRange(57, 73), true),
                    SpecDDPathCandidate("./docs/readme.md", TextRange(114, 130), true),
                    SpecDDPathCandidate("./generated/report.json", TextRange(163, 186), true),
                    SpecDDPathCandidate("./generated", TextRange(210, 221), true),
                )
            }
        }

        `when`("non-path-bearing sections contain path-shaped text") {
            then("it extracts inline paths as navigation-only candidates") {
                val text = """
                    |Spec: Demo
                    |Must:
                    |  ./src/main
                    |Done when:
                    |  ./docs/readme.md
                    """.trimMargin()

                extractor.extract(text).shouldContainExactly(
                    SpecDDPathCandidate("./src/main", TextRange(19, 29), true, warnIfUnresolved = false),
                    SpecDDPathCandidate("./docs/readme.md", TextRange(43, 59), true, warnIfUnresolved = false),
                )
            }
        }

        `when`("scenario steps contain paths and globs") {
            then("it extracts inline references from step text") {
                val text = """
                    |Scenario: write authority remains local
                    |  Given this fixture owns ./kitchen-sink.sdd and ./generated/*
                    |  And ../README.md and ../src/.specdd/bootstrap.md may be read
                    """.trimMargin()

                extractor.extract(text).shouldContainExactly(
                    SpecDDPathCandidate("./kitchen-sink.sdd", TextRange(66, 84), true, warnIfUnresolved = false),
                    SpecDDPathCandidate("./generated/*", TextRange(89, 102), true, warnIfUnresolved = false),
                    SpecDDPathCandidate("../README.md", TextRange(109, 121), true, warnIfUnresolved = false),
                    SpecDDPathCandidate("../src/.specdd/bootstrap.md", TextRange(126, 153), true, warnIfUnresolved = false),
                )
            }
        }

        `when`("path candidates use recursive globstar syntax") {
            then("it extracts globstar references with explicit path prefixes") {
                val text = """
                    |References:
                    |  ./**/*.sdd
                    |  ../fixtures/**/*.sdd
                    |  /src/**/*.kt
                """.trimMargin()

                extractor.extract(text).shouldContainExactly(
                    SpecDDPathCandidate("./**/*.sdd", TextRange(14, 24), true),
                    SpecDDPathCandidate("../fixtures/**/*.sdd", TextRange(27, 47), true),
                    SpecDDPathCandidate("/src/**/*.kt", TextRange(50, 62), true),
                )
            }
        }

        `when`("path-bearing key-value sections use bare names") {
            then("it treats the keys as concrete path candidates") {
                val text = """
                    |Structure:
                    |  ./parser: Shared parser
                """.trimMargin()

                extractor.extract(text).shouldContainExactly(
                    SpecDDPathCandidate("./parser", TextRange(13, 21), true),
                )
            }
        }

        `when`("path-bearing sections contain prose and common resource filenames") {
            then("it extracts only the strong path candidates") {
                val text = """
                    |Owns:
                    |  Plugin metadata and icons
                    |  ./plugin.xml
                    |  ./logo.svg
                    |  ./module.custom-ext
                    |  ./Makefile
                    |Can modify:
                    |  UI resource files
                    |  ./pluginIcon.svg
                """.trimMargin()

                extractor.extract(text).shouldContainExactly(
                    SpecDDPathCandidate("./plugin.xml", TextRange(36, 48), true),
                    SpecDDPathCandidate("./logo.svg", TextRange(51, 61), true),
                    SpecDDPathCandidate("./module.custom-ext", TextRange(64, 83), true),
                    SpecDDPathCandidate("./Makefile", TextRange(86, 96), true),
                    SpecDDPathCandidate("./pluginIcon.svg", TextRange(131, 147), true),
                )
            }
        }

        `when`("spec text contains URLs") {
            then("it does not extract URL host paths as file candidates") {
                val text = """
                    |References:
                    |  https://github.com/specdd/intellij-plugin
                    |Purpose:
                    |  See https://specdd.ai for ./docs/readme.md
                """.trimMargin()

                extractor.extract(text).shouldContainExactly(
                    SpecDDPathCandidate("./docs/readme.md", TextRange(93, 109), true, warnIfUnresolved = false),
                )
            }
        }

        `when`("spec text contains path-like text inside inline code spans") {
            then("it does not extract candidates from balanced code spans") {
                val text = """
                    |References:
                    |  Use `./docs/missing.md` as an example.
                    |  Use ./docs/readme.md as a reference.
                    |  Use `./unterminated.md as ordinary text with ./docs/open.md.
                """.trimMargin()

                extractor.extract(text).shouldContainExactly(
                    SpecDDPathCandidate("./docs/readme.md", TextRange(59, 75), true),
                    SpecDDPathCandidate("./docs/open.md", TextRange(139, 153), true),
                )
            }
        }

        `when`("path candidates use trailing whitespace and CRLF line endings") {
            then("it trims candidate ranges and advances over CRLF") {
                val text = "References:\r\n  ./docs/readme.md  \r\n  ./src/*"

                extractor.extract(text).shouldContainExactly(
                    SpecDDPathCandidate("./docs/readme.md", TextRange(15, 31), true),
                    SpecDDPathCandidate("./src/*", TextRange(37, 44), true),
                )
            }
        }

        `when`("path candidates use CR line endings") {
            then("it advances over CR") {
                val text = "References:\r  ./docs/readme.md"

                extractor.extract(text).shouldContainExactly(
                    SpecDDPathCandidate("./docs/readme.md", TextRange(14, 30), true),
                )
            }
        }
    }
})
