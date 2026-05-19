package ai.specdd.idea.references

import com.intellij.openapi.project.Project
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElement
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe

class SpecDDSymbolReferenceBehaviorSpec : BehaviorSpec({
    given("a SpecDD symbol resolver") {
        val target = psiElement("FetchClient")
        val project = project(null)

        `when`("a symbol has exactly one valid PSI target") {
            then("it resolves to that target") {
                val resolver = SpecDDSymbolResolver {
                    listOf(FakeChooseByNameContributor(mapOf(SymbolLookupCall("FetchClient", "FetchClient") to listOf(target))))
                }

                resolver.resolve("FetchClient", project).shouldContainExactly(target)
            }
        }

        `when`("a symbol has multiple valid PSI targets") {
            then("it returns all targets for IntelliJ to present") {
                val first = psiElement("FetchClientA")
                val second = psiElement("FetchClientB")
                val resolver = SpecDDSymbolResolver {
                    listOf(
                        FakeChooseByNameContributor(
                            mapOf(SymbolLookupCall("FetchClient", "FetchClient") to listOf(first, second)),
                        ),
                    )
                }

                resolver.resolve("FetchClient", project).shouldContainExactly(first, second)
            }
        }

        `when`("a dotted symbol is resolved by final segment and full pattern") {
            then("it resolves language-agnostically through the contributor") {
                val resolver = SpecDDSymbolResolver {
                    listOf(
                        FakeChooseByNameContributor(
                            mapOf(SymbolLookupCall("SpecDDFileType", "ai.specdd.idea.SpecDDFileType") to listOf(target)),
                        ),
                    )
                }

                resolver.resolve("ai.specdd.idea.SpecDDFileType", project).shouldContainExactly(target)
            }
        }

        `when`("symbol targets exceed the cap") {
            then("it returns the first 50 unique targets") {
                val targets = (1..55).map { index -> psiElement("Target$index") }
                val resolver = SpecDDSymbolResolver {
                    listOf(FakeChooseByNameContributor(mapOf(SymbolLookupCall("Target", "Target") to targets)))
                }

                resolver.resolve("Target", project).shouldContainExactly(targets.take(50))
            }
        }

        `when`("a candidate is path-like or contains prose whitespace or invalid symbol characters") {
            then("it is ignored") {
                val resolver = SpecDDSymbolResolver {
                    listOf(
                        FakeChooseByNameContributor(
                            mapOf(SymbolLookupCall("./src/main.kt", "./src/main.kt") to listOf(target)),
                        ),
                    )
                }

                resolver.resolve("./src/main.kt", project) shouldBe emptyList()
                resolver.resolve("Fetch Client", project) shouldBe emptyList()
                resolver.resolve("Fetch\$Client", project) shouldBe emptyList()
                resolver.resolve("Fetch-Client", project) shouldBe emptyList()
            }
        }

        `when`("a contributor does not expose name lookup") {
            then("it is ignored") {
                val resolver = SpecDDSymbolResolver { listOf(NonNameContributor()) }

                resolver.resolve("FetchClient", project) shouldBe emptyList()
            }
        }

        `when`("the default platform contributor lookup is used") {
            then("it returns no target for an unknown symbol") {
                SpecDDSymbolResolver().resolve("SpecDDDefinitelyMissingSymbol", project) shouldBe emptyList()
            }
        }
    }

    given("a SpecDD symbol reference") {
        `when`("the referenced symbol resolves") {
            then("it exposes soft resolve results") {
                val target = psiElement("FetchClient")
                val reference = SpecDDSymbolReference(
                    element = psiElement("@FetchClient", project(null)),
                    rangeInElement = TextRange(0, 12),
                    symbolText = "FetchClient",
                    resolver = SpecDDSymbolResolver {
                        listOf(FakeChooseByNameContributor(mapOf(SymbolLookupCall("FetchClient", "FetchClient") to listOf(target))))
                    },
                )

                reference.canonicalText shouldBe "FetchClient"
                reference.rangeInElement shouldBe TextRange(0, 12)
                reference.isSoft shouldBe true
                reference.multiResolve(false).map { result -> result.element }.shouldContainExactly(target)
            }
        }

        `when`("a simple referenced symbol is renamed") {
            then("it rewrites the reference with the explicit at prefix") {
                var changedText: String? = null
                val reference = SpecDDSymbolReference(
                    element = psiElement("@Invoice", project(null)),
                    rangeInElement = TextRange(0, "@Invoice".length),
                    symbolText = "Invoice",
                    referenceTextUpdater = { sourceElement, range, text ->
                        range shouldBe TextRange(0, "@Invoice".length)
                        changedText = text
                        sourceElement
                    },
                )

                reference.handleElementRename("Receipt")

                changedText shouldBe "@Receipt"
            }
        }

        `when`("a qualified referenced symbol is renamed") {
            then("it preserves the qualifier and rewrites only the final segment") {
                var changedText: String? = null
                val reference = SpecDDSymbolReference(
                    element = psiElement("@invoice_demo.models.Invoice", project(null)),
                    rangeInElement = TextRange(0, "@invoice_demo.models.Invoice".length),
                    symbolText = "invoice_demo.models.Invoice",
                    referenceTextUpdater = { sourceElement, _, text ->
                        changedText = text
                        sourceElement
                    },
                )

                reference.handleElementRename("Receipt")

                changedText shouldBe "@invoice_demo.models.Receipt"
            }
        }

        `when`("a renamed symbol name already includes the explicit at prefix") {
            then("it avoids duplicating the prefix") {
                var changedText: String? = null
                val reference = SpecDDSymbolReference(
                    element = psiElement("@Invoice", project(null)),
                    rangeInElement = TextRange(0, "@Invoice".length),
                    symbolText = "Invoice",
                    referenceTextUpdater = { sourceElement, _, text ->
                        changedText = text
                        sourceElement
                    },
                )

                reference.handleElementRename("@Receipt")

                changedText shouldBe "@Receipt"
            }
        }

    }
})

private class FakeChooseByNameContributor(
    private val targetsByLookup: Map<SymbolLookupCall, List<PsiElement>>,
) {
    @Suppress("UNUSED_PARAMETER")
    fun getItemsByName(
        name: String,
        pattern: String,
        project: Project,
        includeNonProjectItems: Boolean,
    ): Array<Any> = targetsByLookup[SymbolLookupCall(name, pattern)]?.toTypedArray() ?: emptyArray()
}

private data class SymbolLookupCall(
    val name: String,
    val pattern: String,
)

private class NonNameContributor
