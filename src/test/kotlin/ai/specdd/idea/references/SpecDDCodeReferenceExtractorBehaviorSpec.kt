package ai.specdd.idea.references

import com.intellij.openapi.util.TextRange
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.collections.shouldContainExactly

class SpecDDCodeReferenceExtractorBehaviorSpec : BehaviorSpec({
    given("a SpecDD code reference extractor") {
        val extractor = SpecDDCodeReferenceExtractor()

        `when`("text contains balanced inline code spans") {
            then("it extracts the inner content and full ranges") {
                val text = "Must:\n  Use `FetchClient` and `./src/main.kt`."

                extractor.extract(text).shouldContainExactly(
                    SpecDDCodeReferenceCandidate(
                        text = "FetchClient",
                        contentRange = TextRange(13, 24),
                        fullRange = TextRange(12, 25),
                    ),
                    SpecDDCodeReferenceCandidate(
                        text = "./src/main.kt",
                        contentRange = TextRange(31, 44),
                        fullRange = TextRange(30, 45),
                    ),
                )
            }
        }

        `when`("text contains empty or unmatched inline code spans") {
            then("it ignores them") {
                extractor.extract("Must:\n  `` and `unterminated").shouldContainExactly()
            }
        }

        `when`("a code span is not closed before the line ending") {
            then("it does not cross line boundaries") {
                val text = "Must:\n  `FetchClient\n  `Logger`"

                extractor.extract(text).shouldContainExactly(
                    SpecDDCodeReferenceCandidate(
                        text = "Logger",
                        contentRange = TextRange(24, 30),
                        fullRange = TextRange(23, 31),
                    ),
                )
            }
        }

        `when`("text contains CRLF line endings") {
            then("it advances over both line-ending characters") {
                val text = "Must:\r\n  `Logger`"

                extractor.extract(text).shouldContainExactly(
                    SpecDDCodeReferenceCandidate(
                        text = "Logger",
                        contentRange = TextRange(10, 16),
                        fullRange = TextRange(9, 17),
                    ),
                )
            }
        }

        `when`("text contains CR line endings") {
            then("it advances over the carriage return") {
                val text = "Must:\r  `Logger`"

                extractor.extract(text).shouldContainExactly(
                    SpecDDCodeReferenceCandidate(
                        text = "Logger",
                        contentRange = TextRange(9, 15),
                        fullRange = TextRange(8, 16),
                    ),
                )
            }
        }
    }
})
