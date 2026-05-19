package ai.specdd.idea.references

import com.intellij.openapi.util.TextRange
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.collections.shouldContainExactly

class SpecDDSymbolReferenceExtractorBehaviorSpec : BehaviorSpec({
    given("a SpecDD symbol reference extractor") {
        val extractor = SpecDDSymbolReferenceExtractor()

        `when`("text contains explicit symbol references") {
            then("it extracts symbol text without the leading at sign") {
                val text = "Depends on:\n  @FetchClient and @Billing.Provider"

                extractor.extract(text).shouldContainExactly(
                    SpecDDSymbolReferenceCandidate(
                        text = "FetchClient",
                        range = TextRange(text.indexOf("@FetchClient"), text.indexOf("@FetchClient") + 12),
                    ),
                    SpecDDSymbolReferenceCandidate(
                        text = "Billing.Provider",
                        range = TextRange(text.indexOf("@Billing.Provider"), text.indexOf("@Billing.Provider") + 17),
                    ),
                )
            }
        }

        `when`("symbol references use formal punctuation") {
            then("it keeps allowed non-whitespace symbol characters") {
                val text = "Must:\n  Call @Service:create#id\\path/query! before returning."

                extractor.extract(text).shouldContainExactly(
                    SpecDDSymbolReferenceCandidate(
                        text = "Service:create#id\\path/query!",
                        range = TextRange(
                            text.indexOf("@Service:create"),
                            text.indexOf("@Service:create") + "@Service:create#id\\path/query!".length,
                        ),
                    ),
                )
            }
        }

        `when`("symbol references appear after opening punctuation") {
            then("it extracts them") {
                val text = "Must:\n  See (@InvoiceService) and [@PaymentService]."

                extractor.extract(text).shouldContainExactly(
                    SpecDDSymbolReferenceCandidate(
                        text = "InvoiceService",
                        range = TextRange(text.indexOf("@InvoiceService"), text.indexOf("@InvoiceService") + 15),
                    ),
                    SpecDDSymbolReferenceCandidate(
                        text = "PaymentService",
                        range = TextRange(text.indexOf("@PaymentService"), text.indexOf("@PaymentService") + 15),
                    ),
                )
            }
        }

        `when`("a final sentence period follows a symbol") {
            then("it trims the period from the candidate") {
                val text = "Must:\n  Call @InvoiceService.createInvoice."

                extractor.extract(text).shouldContainExactly(
                    SpecDDSymbolReferenceCandidate(
                        text = "InvoiceService.createInvoice",
                        range = TextRange(
                            text.indexOf("@InvoiceService.createInvoice"),
                            text.indexOf("@InvoiceService.createInvoice") + "@InvoiceService.createInvoice".length,
                        ),
                    ),
                )
            }
        }

        `when`("a final symbol period is followed by non-closing punctuation") {
            then("it keeps the period in the candidate") {
                val text = "Must:\n  Compare @Namespace., with text."

                extractor.extract(text).shouldContainExactly(
                    SpecDDSymbolReferenceCandidate(
                        text = "Namespace.",
                        range = TextRange(text.indexOf("@Namespace."), text.indexOf("@Namespace.") + 11),
                    ),
                )
            }
        }

        `when`("at signs are escaped, embedded, or missing a valid first character") {
            then("it ignores them") {
                extractor.extract(
                    "Spec: Demo\nMust:\n  Use \\@decorator, email@example.com, @9bad, and plain FetchClient.",
                ).shouldContainExactly()
            }
        }

        `when`("at signs appear in comments or section headers") {
            then("it ignores them as non-reference syntax") {
                val text = """
                    |# @CommentSymbol
                    |Spec: @TitleSymbol
                    |Must:
                    |  @BodySymbol
                    |  # @IndentedCommentSymbol
                    """.trimMargin()

                extractor.extract(text).shouldContainExactly(
                    SpecDDSymbolReferenceCandidate(
                        text = "BodySymbol",
                        range = TextRange(text.indexOf("@BodySymbol"), text.indexOf("@BodySymbol") + 11),
                    ),
                )
            }
        }

        `when`("symbol references appear inside inline code spans") {
            then("it extracts the explicit symbol reference") {
                val text = "Must:\n  Use `@dataclass` in Python."

                extractor.extract(text).shouldContainExactly(
                    SpecDDSymbolReferenceCandidate(
                        text = "dataclass",
                        range = TextRange(text.indexOf("@dataclass"), text.indexOf("@dataclass") + 10),
                    ),
                )
            }
        }

        `when`("text contains CRLF and CR line endings") {
            then("it advances over each line-ending style") {
                val text = "Must:\r\n  @Logger\rDepends on:\r  @Config"

                extractor.extract(text).shouldContainExactly(
                    SpecDDSymbolReferenceCandidate(
                        text = "Logger",
                        range = TextRange(text.indexOf("@Logger"), text.indexOf("@Logger") + 7),
                    ),
                    SpecDDSymbolReferenceCandidate(
                        text = "Config",
                        range = TextRange(text.indexOf("@Config"), text.indexOf("@Config") + 7),
                    ),
                )
            }
        }
    }
})
