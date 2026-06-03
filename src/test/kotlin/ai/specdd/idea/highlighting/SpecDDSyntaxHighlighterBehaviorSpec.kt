package ai.specdd.idea.highlighting

import com.intellij.openapi.editor.colors.TextAttributesKey
import com.intellij.psi.TokenType
import com.intellij.psi.tree.IElementType
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.collections.shouldNotContain
import io.kotest.matchers.shouldBe

class SpecDDSyntaxHighlighterBehaviorSpec : BehaviorSpec({
    given("a SpecDD syntax highlighter") {
        val highlighter = SpecDDSyntaxHighlighter()

        `when`("highlighting keys are requested for non-SpecDD token types") {
            then("bad characters and unknown tokens are mapped explicitly") {
                highlighter.getTokenHighlights(TokenType.BAD_CHARACTER).toList() shouldContainExactly
                        listOf(SpecDDHighlightingColors.BAD_CHARACTER)

                highlighter.getTokenHighlights(TokenType.WHITE_SPACE).toList() shouldBe emptyList()
                highlighter.getTokenHighlights(IElementType("UNKNOWN", null)).toList() shouldBe emptyList()
            }
        }

        `when`("a kitchen-sink style document is lexed") {
            then("all supported SpecDD highlight categories are emitted") {
                val text = """
                    |# comment
                    |Spec: SpecDD Kitchen Sink Fixture
                    |
                    |Platform:
                    |Purpose:
                    |Structure:
                    |Owns:
                    |Can modify:
                    |Can read:
                    |References:
                    |Must:
                    |Must not:
                    |Forbids:
                    |Depends on:
                    |Exposes:
                    |Accepts:
                    |Returns:
                    |Raises:
                    |Handles:
                    |Tasks:
                    |  [x] #0 done task
                    |  [X] #1 uppercase done task
                    |  [ ] #2 open task
                    |  [!] #3 blocked task
                    |  [?] #4 question task
                    |  [-] #5 skipped task
                    |  [invalid] #6 unsupported task marker
                    |Done when:
                    |Scenario: every documented section is represented
                    |  Given file /src/main/kotlin/ai/specdd/idea/SpecDDLanguage.kt exists
                    |  When SpecDDKitchenSink.ReportBuilder.build(input) runs
                    |  Then ./generated/coverage.json exists
                    |  And #8 is visible
                    |  But Andromeda is not a step keyword
                    |Example: titled example
                    |  public symbol: @SpecDDKitchenSink.ReportBuilder.build
                    |  glob path: ./generated/*.json
                    |  output file: ./generated/coverage.json
                    |  unknown text
                    |    continuation text
                """.trimMargin()

                val tokens = highlighter.highlight(text)

                tokens.map { it.text to it.keys }.toSet() shouldContain
                        ("# comment" to listOf(SpecDDHighlightingColors.COMMENT))
                tokens.withKey(SpecDDHighlightingColors.SECTION_LABEL).map { it.text }.toSet() shouldBe
                        setOf(
                            "Purpose",
                            "Structure",
                            "Owns",
                            "References",
                            "Depends on",
                            "Tasks",
                            "Done when",
                            "Example",
                            "Scenario",
                        )
                tokens.withKey(SpecDDHighlightingColors.KEY_VALUE_KEY).map { it.text }.toSet() shouldBe
                        setOf("public symbol", "glob path", "output file")
                tokens.withKey(SpecDDHighlightingColors.SECTION_META).map { it.text }.toSet() shouldBe
                        setOf("Spec", "Platform")
                tokens.withKey(SpecDDHighlightingColors.SECTION_POSITIVE).map { it.text }.toSet() shouldBe
                        setOf("Can modify", "Can read", "Exposes", "Accepts", "Returns", "Handles")
                tokens.withKey(SpecDDHighlightingColors.SECTION_NEGATIVE).map { it.text }.toSet() shouldBe
                        setOf("Must not", "Forbids", "Raises")
                tokens.withKey(SpecDDHighlightingColors.SECTION_REQUIRED).map { it.text }.toSet() shouldBe
                        setOf("Must")
                tokens.withKey(SpecDDHighlightingColors.SECTION_COLON).map { it.text }.distinct() shouldBe listOf(":")
                tokens.withKey(SpecDDHighlightingColors.SECTION_VALUE).map { it.text } shouldContain
                        " SpecDD Kitchen Sink Fixture"
                tokens.withKey(SpecDDHighlightingColors.SECTION_VALUE).map { it.text } shouldContain
                        " titled example"
                tokens.withKey(SpecDDHighlightingColors.INDENT).map { it.text }.toSet() shouldBe setOf("  ", "    ")
                tokens.withKey(SpecDDHighlightingColors.CONTINUATION_TEXT).map { it.text } shouldContain
                        "continuation text"
                tokens.withKey(SpecDDHighlightingColors.TASK_DONE).map { it.text }.toSet() shouldBe
                        setOf("[x]", "[X]")
                tokens.withKey(SpecDDHighlightingColors.TASK_OPEN).map { it.text }.toSet() shouldBe setOf("[ ]")
                tokens.withKey(SpecDDHighlightingColors.TASK_BLOCKED).map { it.text }.toSet() shouldBe setOf("[!]")
                tokens.withKey(SpecDDHighlightingColors.TASK_QUESTION).map { it.text }.toSet() shouldBe setOf("[?]")
                tokens.withKey(SpecDDHighlightingColors.TASK_SKIPPED).map { it.text }.toSet() shouldBe setOf("[-]")
                tokens.withKey(SpecDDHighlightingColors.TASK_INVALID).map { it.text }.toSet() shouldBe
                        setOf("[invalid]")
                tokens.withKey(SpecDDHighlightingColors.TASK_ID).map { it.text }.toSet() shouldBe
                        setOf("#0", "#1", "#2", "#3", "#4", "#5", "#6", "#8")
                tokens.withKey(SpecDDHighlightingColors.SCENARIO_STEP).map { it.text }.toSet() shouldBe
                        setOf("Given", "When", "Then", "And", "But")
                tokens.withKey(SpecDDHighlightingColors.PATH).map { it.text }.toSet() shouldBe
                        setOf(
                            "/src/main/kotlin/ai/specdd/idea/SpecDDLanguage.kt",
                            "./generated/*.json",
                            "./generated/coverage.json",
                        )
                tokens.withKey(SpecDDHighlightingColors.SYMBOL).map { it.text }.toSet() shouldBe
                        setOf("@SpecDDKitchenSink.ReportBuilder.build")
                tokens.withKey(SpecDDHighlightingColors.SYMBOL).map { it.text } shouldNotContain
                        "SpecDDKitchenSink.ReportBuilder.build(input)"
                tokens.withKey(SpecDDHighlightingColors.SCENARIO_STEP).map { it.text } shouldNotContain "Andromeda"
            }
        }

        `when`("a slice of a CRLF document is lexed") {
            then("token offsets are relative to the original buffer and line endings are preserved as text") {
                val text = "xx\r\n  # sliced comment\r\nMust: keep going\r\n"
                val lexer = highlighter.highlightingLexer

                lexer.start(text, 4, text.length - 2, 0)

                lexer.state shouldBe 0
                lexer.bufferSequence shouldBe text
                lexer.bufferEnd shouldBe text.length - 2

                val tokens = generateSequence {
                    val type = lexer.tokenType ?: return@generateSequence null
                    LexedToken(text.substring(lexer.tokenStart, lexer.tokenEnd), type.toString()).also {
                        lexer.advance()
                    }
                }.toList()

                tokens.first() shouldBe LexedToken("  ", "INDENT")
                tokens shouldContain LexedToken("# sliced comment", "COMMENT")
                tokens shouldContain LexedToken("Must", "SECTION_REQUIRED")
                tokens shouldContain LexedToken(":", "SECTION_COLON")
                tokens shouldContain LexedToken(" keep going", "SECTION_VALUE")

                lexer.tokenType shouldBe null
                lexer.tokenStart shouldBe text.length - 2
                lexer.tokenEnd shouldBe text.length - 2
            }
        }

        `when`("lexing restarts inside a section line") {
            then("tokens keep the line-level section highlighting") {
                val text = "Spec: SpecDD Kitchen Sink Fixture"
                val lexer = highlighter.highlightingLexer

                lexer.start(text, 2, text.length, 0)

                val tokens = generateSequence {
                    val type = lexer.tokenType ?: return@generateSequence null
                    LexedToken(text.substring(lexer.tokenStart, lexer.tokenEnd), type.toString()).also {
                        lexer.advance()
                    }
                }.toList()

                tokens.first() shouldBe LexedToken("ec", "SECTION_META")
                tokens shouldContain LexedToken(":", "SECTION_COLON")
                tokens shouldContain LexedToken(" SpecDD Kitchen Sink Fixture", "SECTION_VALUE")
            }
        }

        `when`("lexing restarts inside a section value") {
            then("the remaining value keeps section value highlighting") {
                val text = "Spec: SpecDD Kitchen Sink Fixture"
                val lexer = highlighter.highlightingLexer

                lexer.start(text, 10, text.length, 0)

                lexer.tokenType.toString() shouldBe "SECTION_VALUE"
                text.substring(lexer.tokenStart, lexer.tokenEnd) shouldBe "DD Kitchen Sink Fixture"
            }
        }

        `when`("lexing receives a slice ending inside a section value") {
            then("classification still uses the complete line context") {
                val text = "Spec: SpecDD Kitchen Sink Fixture"
                val lexer = highlighter.highlightingLexer

                lexer.start(text, 0, 10, 0)

                val tokens = generateSequence {
                    val type = lexer.tokenType ?: return@generateSequence null
                    LexedToken(text.substring(lexer.tokenStart, lexer.tokenEnd), type.toString()).also {
                        lexer.advance()
                    }
                }.toList()

                tokens shouldContain LexedToken("Spec", "SECTION_META")
                tokens shouldContain LexedToken(":", "SECTION_COLON")
                tokens shouldContain LexedToken(" Spec", "SECTION_VALUE")
            }
        }

        `when`("lexing restarts inside a task line") {
            then("it recovers the Tasks section context from earlier lines") {
                val text = "Spec: Example\nTasks:\n  [?] #7 Check context"
                val lexer = highlighter.highlightingLexer

                lexer.start(text, text.indexOf("[?]"), text.length, 0)

                val tokens = generateSequence {
                    val type = lexer.tokenType ?: return@generateSequence null
                    LexedToken(text.substring(lexer.tokenStart, lexer.tokenEnd), type.toString()).also {
                        lexer.advance()
                    }
                }.toList()

                tokens shouldContain LexedToken("[?]", "TASK_QUESTION")
                tokens shouldContain LexedToken("#7", "TASK_ID")
            }
        }

        `when`("a document contains old Mac line endings") {
            then("the lexer advances through carriage returns") {
                val text = "Spec:\rOwns: file.sdd\r"

                highlighter.highlight(text).map { it.text to it.keys }.toSet() shouldContain
                        ("Owns" to listOf(SpecDDHighlightingColors.SECTION_LABEL))
            }
        }

        `when`("a short task marker candidate is lexed") {
            then("it highlights only the continuation indent") {
                highlighter.highlight("  [").map { it.text to it.keys } shouldContain
                        ("  " to listOf(SpecDDHighlightingColors.INDENT))
            }
        }

        `when`("a four-space indented body line is lexed") {
            then("it highlights visible content as continuation text") {
                val tokens = highlighter.highlight("    continued body text")

                tokens.map { it.text to it.keys } shouldContain
                        ("    " to listOf(SpecDDHighlightingColors.INDENT))
                tokens.map { it.text to it.keys } shouldContain
                        ("continued body text" to listOf(SpecDDHighlightingColors.CONTINUATION_TEXT))
            }
        }

        `when`("a four-space indented line looks like another construct") {
            then("continuation text highlighting takes precedence") {
                val tokens = highlighter.highlight("    glob path: generated/*.json")

                tokens.map { it.text to it.keys } shouldContain
                        ("glob path: generated/*.json" to listOf(SpecDDHighlightingColors.CONTINUATION_TEXT))
                tokens.withKey(SpecDDHighlightingColors.SECTION_LABEL) shouldBe emptyList()
                tokens.withKey(SpecDDHighlightingColors.PATH) shouldBe emptyList()
            }
        }

        `when`("a four-space indented comment is lexed") {
            then("comment highlighting still takes precedence over continuation text") {
                val tokens = highlighter.highlight("    # continuation comment")

                tokens.map { it.text to it.keys } shouldContain
                        ("# continuation comment" to listOf(SpecDDHighlightingColors.COMMENT))
                tokens.withKey(SpecDDHighlightingColors.CONTINUATION_TEXT) shouldBe emptyList()
            }
        }

        `when`("the kitchen-sink question task is lexed") {
            then("it highlights the task marker and id without comment highlighting") {
                val text =
                    "Tasks:\n  [?] #7 Decide whether fixtures should include intentionally invalid examples in a separate file."
                val tokens = highlighter.highlight(text)

                tokens.map { it.text to it.keys } shouldContain
                        ("[?]" to listOf(SpecDDHighlightingColors.TASK_QUESTION))
                tokens.map { it.text to it.keys } shouldContain
                        ("#7" to listOf(SpecDDHighlightingColors.TASK_ID))
                tokens.withKey(SpecDDHighlightingColors.COMMENT) shouldBe emptyList()
            }
        }

        `when`("task-looking body text outside Tasks is lexed") {
            then("it does not receive task marker highlighting") {
                val tokens = highlighter.highlight(
                    """
                    |Purpose:
                    |  [ ] ordinary prose
                    |  [invalid] ordinary prose
                    """.trimMargin(),
                )

                tokens.withKey(SpecDDHighlightingColors.TASK_OPEN) shouldBe emptyList()
                tokens.withKey(SpecDDHighlightingColors.TASK_INVALID) shouldBe emptyList()
            }
        }

        `when`("a generic key-value line is lexed") {
            then("it highlights the key and colon while still highlighting value patterns") {
                val tokens = highlighter.highlight("  glob path: ./generated/*.json")

                tokens.map { it.text to it.keys } shouldContain
                        ("  " to listOf(SpecDDHighlightingColors.INDENT))
                tokens.map { it.text to it.keys } shouldContain
                        ("glob path" to listOf(SpecDDHighlightingColors.KEY_VALUE_KEY))
                tokens.map { it.text to it.keys } shouldContain
                        (":" to listOf(SpecDDHighlightingColors.SECTION_COLON))
                tokens.map { it.text to it.keys } shouldContain
                        ("./generated/*.json" to listOf(SpecDDHighlightingColors.PATH))
            }
        }

        `when`("a structure block contains a generated key") {
            then("the key does not inherit section label highlighting") {
                val tokens = highlighter.highlight("  generated: Optional generated outputs for local experiments")

                tokens.map { it.text to it.keys } shouldContain
                        ("generated" to listOf(SpecDDHighlightingColors.KEY_VALUE_KEY))
                tokens.withKey(SpecDDHighlightingColors.SECTION_LABEL) shouldBe emptyList()
            }
        }

        `when`("an inline pattern appears after plain text") {
            then("it preserves the plain text gap before the highlighted token") {
                val tokens = highlighter.highlight("  see ./generated/*.json")

                tokens.map { it.text to it.keys } shouldContain
                        ("see " to emptyList())
                tokens.map { it.text to it.keys } shouldContain
                        ("./generated/*.json" to listOf(SpecDDHighlightingColors.PATH))
            }
        }

        `when`("body text contains brace-alternative globs") {
            then("it highlights the complete glob without trailing punctuation") {
                val tokens = highlighter.highlight("  see ./src/{main,test}.sdd, and /src/{main,test}/**/*.kt.")

                tokens.withKey(SpecDDHighlightingColors.PATH).map { it.text } shouldBe
                        listOf("./src/{main,test}.sdd", "/src/{main,test}/**/*.kt")
            }
        }

        `when`("body text contains unprefixed path-like prose and URLs") {
            then("it does not highlight them as file paths") {
                val tokens = highlighter.highlight(
                    "  see generated/*.json, src/main/App.kt, plugin.xml, and https://github.com/specdd/intellij-plugin",
                )

                tokens.withKey(SpecDDHighlightingColors.PATH) shouldBe emptyList()
            }
        }

        `when`("body text contains inline code spans") {
            then("it highlights balanced single-line backtick spans") {
                val tokens = highlighter.highlight("  use `SpecDD.Parser` and `./fixtures/*.sdd`")

                tokens.map { it.text to it.keys } shouldContain
                        ("SpecDD.Parser" to listOf(SpecDDHighlightingColors.CODE_SPAN))
                tokens.map { it.text to it.keys } shouldContain
                        ("./fixtures/*.sdd" to listOf(SpecDDHighlightingColors.CODE_SPAN))
                tokens.withKey(SpecDDHighlightingColors.CODE_SPAN_DELIMITER).map { it.text } shouldBe
                        listOf("`", "`", "`", "`")
                tokens.withKey(SpecDDHighlightingColors.PATH) shouldBe emptyList()
                tokens.withKey(SpecDDHighlightingColors.SYMBOL) shouldBe emptyList()
            }
        }

        `when`("a task line contains an inline code span with a task id") {
            then("the code span wins over inline task-id highlighting") {
                val tokens = highlighter.highlight("Tasks:\n  [ ] compare `#123` with #124")

                tokens.map { it.text to it.keys } shouldContain
                        ("#123" to listOf(SpecDDHighlightingColors.CODE_SPAN))
                tokens.withKey(SpecDDHighlightingColors.TASK_ID).map { it.text } shouldBe listOf("#124")
            }
        }

        `when`("body text contains an unmatched backtick") {
            then("it does not highlight a code span") {
                val tokens = highlighter.highlight("  use `SpecDD.Parser")

                tokens.withKey(SpecDDHighlightingColors.CODE_SPAN) shouldBe emptyList()
                tokens.withKey(SpecDDHighlightingColors.SYMBOL) shouldBe emptyList()
            }
        }

        `when`("body text contains explicit and plain symbols") {
            then("it highlights only explicit at-prefixed symbol references") {
                val tokens = highlighter.highlight(
                    "  Call @InvoiceService.createInvoice, not InvoiceService.createInvoice or \\@literal.",
                )

                tokens.withKey(SpecDDHighlightingColors.SYMBOL).map { it.text } shouldBe
                        listOf("@InvoiceService.createInvoice")
            }
        }

        `when`("explicit symbols end with period punctuation") {
            then("it trims sentence periods but keeps non-sentence periods") {
                val tokens = highlighter.highlight("  Call @Trailing. and @Kept., then continue.")

                tokens.withKey(SpecDDHighlightingColors.SYMBOL).map { it.text } shouldBe
                        listOf("@Trailing", "@Kept.")
            }
        }

        `when`("explicit symbols end with colon punctuation") {
            then("it trims terminal colons but keeps internal colons") {
                val tokens = highlighter.highlight("  See (@Symbol:) and (@Namespace:Symbol:)")

                tokens.withKey(SpecDDHighlightingColors.SYMBOL).map { it.text } shouldBe
                        listOf("@Symbol", "@Namespace:Symbol")
            }
        }

        `when`("an invalid task state is lexed") {
            then("it highlights the full invalid state") {
                val tokens = highlighter.highlight("Tasks:\n  [invalid] #9 unsupported state")

                tokens.map { it.text to it.keys } shouldContain
                        ("[invalid]" to listOf(SpecDDHighlightingColors.TASK_INVALID))
                tokens.map { it.text to it.keys } shouldContain
                        ("#9" to listOf(SpecDDHighlightingColors.TASK_ID))
            }
        }

        `when`("a malformed task marker is lexed inside Tasks") {
            then("it highlights the malformed marker as invalid") {
                val tokens = highlighter.highlight("Tasks:\n  [invalid task marker")

                tokens.map { it.text to it.keys } shouldContain
                        ("[invalid task marker" to listOf(SpecDDHighlightingColors.TASK_INVALID))
            }
        }
    }
})

private data class HighlightedToken(
    val text: String,
    val keys: List<TextAttributesKey>,
)

private data class LexedToken(
    val text: String,
    val type: String,
)

private fun SpecDDSyntaxHighlighter.highlight(text: String): List<HighlightedToken> {
    val lexer = highlightingLexer
    lexer.start(text)

    val tokens = mutableListOf<HighlightedToken>()
    while (null != lexer.tokenType) {
        val tokenType = lexer.tokenType ?: break
        tokens.add(
            HighlightedToken(
                text.substring(lexer.tokenStart, lexer.tokenEnd),
                getTokenHighlights(tokenType).toList(),
            ),
        )
        lexer.advance()
    }

    return tokens
}

private fun List<HighlightedToken>.withKey(key: TextAttributesKey): List<HighlightedToken> =
    filter { token -> key in token.keys }
