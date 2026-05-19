package ai.specdd.idea.completion

import ai.specdd.idea.parser.SpecDDLanguageFacts
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe

class SpecDDSectionCompletionBehaviorSpec : BehaviorSpec({
    given("SpecDD section completion") {
        `when`("completion is requested on an empty top-level line") {
            then("it returns all known section labels") {
                val completion = SpecDDSectionCompletion.complete("", 0)

                completion!!.prefix shouldBe ""
                completion.variants.map { variant -> variant.label }
                    .shouldContainExactly(SpecDDLanguageFacts.sectionLabels)
            }
        }

        `when`("completion is requested after a partial section label") {
            then("it returns matching section labels") {
                val completion = SpecDDSectionCompletion.complete("Pur", 3)

                completion!!.prefix shouldBe "Pur"
                completion.variants.map { variant -> variant.label }.shouldContainExactly("Purpose")
            }
        }

        `when`("completion is requested after a multi-word partial section label") {
            then("it supports spaces inside the prefix") {
                val completion = SpecDDSectionCompletion.complete("Can m", 5)

                completion!!.variants.map { variant -> variant.label }.shouldContainExactly("Can modify")
            }
        }

        `when`("completion is requested with different prefix case") {
            then("it matches section labels case-insensitively") {
                val completion = SpecDDSectionCompletion.complete("must n", 6)

                completion!!.variants.map { variant -> variant.label }.shouldContainExactly("Must not")
            }
        }

        `when`("completion is requested after a newline") {
            then("it uses the current line prefix") {
                val completion = SpecDDSectionCompletion.complete("Spec: Demo\nRef", 14)

                completion!!.prefix shouldBe "Ref"
                completion.variants.map { variant -> variant.label }.shouldContainExactly("References")
            }
        }

        `when`("completion is requested on invalid positions") {
            then("it returns no completion") {
                SpecDDSectionCompletion.complete("Spec", -1).shouldBeNull()
                SpecDDSectionCompletion.complete("Spec", 5).shouldBeNull()
            }
        }

        `when`("completion is requested on unsupported line contexts") {
            then("it returns no completion") {
                SpecDDSectionCompletion.complete("  Pur", 5).shouldBeNull()
                SpecDDSectionCompletion.complete("# Pur", 5).shouldBeNull()
                SpecDDSectionCompletion.complete("[ ] Task", 3).shouldBeNull()
                SpecDDSectionCompletion.complete("glob: value", 5).shouldBeNull()
                SpecDDSectionCompletion.complete("Given a spec", 5).shouldBeNull()
                SpecDDSectionCompletion.complete("Not a section", 13).shouldBeNull()
            }
        }

        `when`("a completion variant is inspected") {
            then("it exposes header insertion text and a lookup element") {
                val variant = SpecDDSectionCompletionVariant("Purpose")

                variant.lookupString shouldBe "Purpose:"
                variant.toLookupElement().lookupString shouldBe "Purpose:"
            }
        }

        `when`("inline-capable completion variants are inspected") {
            then("they expose insertion text with a trailing value space") {
                listOf("Spec", "Platform", "Scenario", "Example").forEach { label ->
                    val variant = SpecDDSectionCompletionVariant(label)

                    variant.lookupString shouldBe "$label: "
                    variant.toLookupElement().lookupString shouldBe "$label: "
                }
            }
        }

        `when`("raw prefixes are inspected") {
            then("they distinguish section prefixes from non-section syntax") {
                SpecDDSectionCompletion.prefixAt("Can ", 4) shouldBe "Can "
                SpecDDSectionCompletion.prefixAt("Can modify:", 11).shouldBeNull()
                SpecDDSectionCompletion.prefixAt("Can-modify", 10).shouldBeNull()
            }
        }

        `when`("known section labels are completed") {
            then("each variant uses the parser-known label spelling") {
                SpecDDSectionCompletion.complete("", 0)!!.variants
                    .map { variant -> variant.label }
                    .shouldContainExactlyInAnyOrder(SpecDDLanguageFacts.sectionLabels)
                SpecDDSectionCompletion.complete("", 0)!!.variants
                    .map { variant -> variant.lookupString }
                    .shouldContain("Spec: ")
                SpecDDSectionCompletion.complete("", 0)!!.variants
                    .map { variant -> variant.lookupString }
                    .shouldContain("Purpose:")
            }
        }
    }
})
