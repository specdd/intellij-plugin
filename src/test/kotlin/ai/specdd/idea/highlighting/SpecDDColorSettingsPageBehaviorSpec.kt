package ai.specdd.idea.highlighting

import com.intellij.openapi.editor.colors.EditorColorsScheme
import com.intellij.openapi.editor.colors.TextAttributesKey
import com.intellij.openapi.editor.markup.TextAttributes
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.collections.shouldContainAll
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import java.awt.Color
import java.awt.Font
import java.lang.reflect.InvocationHandler
import java.lang.reflect.Proxy

class SpecDDColorSettingsPageBehaviorSpec : BehaviorSpec({
    given("a SpecDD color settings page") {
        val page = SpecDDColorSettingsPage()

        `when`("metadata is requested") {
            then("it exposes the SpecDD highlighter, icon, display name, and demo text") {
                page.displayName shouldBe "SpecDD"
                page.icon.toString() shouldBe ai.specdd.idea.SpecDDFileType().icon.toString()
                page.highlighter.shouldBeInstanceOf<SpecDDSyntaxHighlighter>()
                page.demoText.contains("Spec: Invoice Demo Service") shouldBe true
                page.demoText.contains("@InvoiceService.create_invoice") shouldBe true
                page.demoText.contains("Scenario: valid invoice is created") shouldBe true
                page.demoText.contains("Example: unsupported currency") shouldBe true
                page.additionalHighlightingTagToDescriptorMap shouldBe emptyMap()
                page.colorDescriptors.shouldContainExactly()
            }
        }

        `when`("demo text is requested") {
            then("it includes every known section and major valid body form") {
                val demoText = page.demoText

                listOf(
                    "Spec:",
                    "Platform:",
                    "Purpose:",
                    "Structure:",
                    "Owns:",
                    "Can modify:",
                    "Can read:",
                    "References:",
                    "Must:",
                    "Must not:",
                    "Forbids:",
                    "Depends on:",
                    "Exposes:",
                    "Accepts:",
                    "Returns:",
                    "Raises:",
                    "Handles:",
                    "Tasks:",
                    "Scenario:",
                    "Example:",
                    "Done when:",
                    "# Complete SpecDD color preview",
                    "./src/invoice_demo/service.py",
                    "../models/model.sdd",
                    "/fixtures/**/*.sdd",
                    "invoice_id: string",
                    "`@InvoiceRepository`",
                    "@BillingProvider",
                    "[ ] #1",
                    "[x] #2",
                    "[X] #3",
                    "[-] #4",
                    "[!] #5",
                    "[?] #6",
                    "  Given ",
                    "  When ",
                    "  Then ",
                    "  And ",
                    "  But ",
                    "    Capture the decision",
                ).forEach { expected ->
                    demoText.contains(expected) shouldBe true
                }
            }
        }

        `when`("attribute descriptors are requested") {
            then("they expose all semantic highlighting keys") {
                page.attributeDescriptors.map { descriptor -> descriptor.key }.shouldContainAll(
                    SpecDDHighlightingColors.COMMENT,
                    SpecDDHighlightingColors.INDENT,
                    SpecDDHighlightingColors.CONTINUATION_TEXT,
                    SpecDDHighlightingColors.SECTION_LABEL,
                    SpecDDHighlightingColors.KEY_VALUE_KEY,
                    SpecDDHighlightingColors.SECTION_META,
                    SpecDDHighlightingColors.SECTION_POSITIVE,
                    SpecDDHighlightingColors.SECTION_NEGATIVE,
                    SpecDDHighlightingColors.SECTION_REQUIRED,
                    SpecDDHighlightingColors.SECTION_COLON,
                    SpecDDHighlightingColors.SECTION_VALUE,
                    SpecDDHighlightingColors.TASK_DONE,
                    SpecDDHighlightingColors.TASK_OPEN,
                    SpecDDHighlightingColors.TASK_BLOCKED,
                    SpecDDHighlightingColors.TASK_QUESTION,
                    SpecDDHighlightingColors.TASK_SKIPPED,
                    SpecDDHighlightingColors.TASK_INVALID,
                    SpecDDHighlightingColors.TASK_ID,
                    SpecDDHighlightingColors.SCENARIO_STEP,
                    SpecDDHighlightingColors.CODE_SPAN,
                    SpecDDHighlightingColors.CODE_SPAN_DELIMITER,
                    SpecDDHighlightingColors.PATH,
                    SpecDDHighlightingColors.SYMBOL,
                    SpecDDHighlightingColors.BAD_CHARACTER,
                )
                page.attributeDescriptors.map { descriptor -> descriptor.displayName }.shouldContainAll(
                    "Sections//Default label",
                    "Body//Key-value key",
                    "Tasks//Blocked marker",
                    "Inline code//Content",
                    "References//Explicit symbol",
                )
            }
        }

        `when`("a preview color scheme is customized") {
            then("it applies SpecDD-specific default preview attributes without changing the scheme object") {
                val attributesByKey = linkedMapOf<TextAttributesKey, TextAttributes>()
                val scheme = editorColorsScheme(attributesByKey)

                (page.customizeColorScheme(scheme) === scheme) shouldBe true

                attributesByKey[SpecDDHighlightingColors.INDENT]?.foregroundColor shouldBe Color(0x6E7781)
                attributesByKey[SpecDDHighlightingColors.CONTINUATION_TEXT]?.foregroundColor shouldBe Color(0x7A828E)
                attributesByKey[SpecDDHighlightingColors.SECTION_LABEL]?.foregroundColor shouldBe Color(0x58A6FF)
                attributesByKey[SpecDDHighlightingColors.SECTION_LABEL]?.fontType shouldBe Font.BOLD
                attributesByKey[SpecDDHighlightingColors.SECTION_META]?.foregroundColor shouldBe Color(0x7A004B)
                attributesByKey[SpecDDHighlightingColors.SECTION_POSITIVE]?.foregroundColor shouldBe Color(0x22863A)
                attributesByKey[SpecDDHighlightingColors.SECTION_NEGATIVE]?.foregroundColor shouldBe Color(0xD32F2F)
                attributesByKey[SpecDDHighlightingColors.SECTION_REQUIRED]?.foregroundColor shouldBe Color(0xB26A00)
                attributesByKey[SpecDDHighlightingColors.TASK_BLOCKED]?.foregroundColor shouldBe Color(0xD32F2F)
                attributesByKey[SpecDDHighlightingColors.CODE_SPAN]?.foregroundColor shouldBe Color(0x3A6EA5)
                attributesByKey[SpecDDHighlightingColors.CODE_SPAN_DELIMITER]?.foregroundColor shouldBe Color(0x7A828E)
                attributesByKey[SpecDDHighlightingColors.SYMBOL]?.foregroundColor shouldBe Color(0x9B59D0)
            }
        }
    }
})

private fun editorColorsScheme(attributesByKey: MutableMap<TextAttributesKey, TextAttributes>): EditorColorsScheme =
    Proxy.newProxyInstance(
        EditorColorsScheme::class.java.classLoader,
        arrayOf(EditorColorsScheme::class.java),
        InvocationHandler { proxy, method, args ->
            when (method.name) {
                "setAttributes" -> {
                    attributesByKey[args?.get(0) as TextAttributesKey] = args[1] as TextAttributes
                    Unit
                }

                "getAttributes" -> attributesByKey[args?.get(0) as TextAttributesKey]
                else -> proxy
            }
        },
    ) as EditorColorsScheme
