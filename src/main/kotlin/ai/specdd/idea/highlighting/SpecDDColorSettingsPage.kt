package ai.specdd.idea.highlighting

import ai.specdd.idea.SpecDDFileType
import com.intellij.openapi.editor.colors.EditorColorsScheme
import com.intellij.openapi.editor.colors.TextAttributesKey
import com.intellij.openapi.editor.markup.TextAttributes
import com.intellij.openapi.fileTypes.SyntaxHighlighter
import com.intellij.openapi.options.colors.AttributesDescriptor
import com.intellij.openapi.options.colors.ColorDescriptor
import com.intellij.openapi.options.colors.ColorSettingsPage
import java.awt.Color
import java.awt.Font
import javax.swing.Icon

class SpecDDColorSettingsPage : ColorSettingsPage {
    override fun getIcon(): Icon = SpecDDFileType().icon

    override fun getHighlighter(): SyntaxHighlighter = SpecDDSyntaxHighlighter()

    override fun getDemoText(): String = """
        # Complete SpecDD color preview
        Spec: Invoice Demo Service
        Platform: Python 3.12

        Purpose:
          Demonstrate every valid `.sdd` section and body form used by SpecDD.
            Continuation lines keep related text attached to the previous entry.

        Structure:
          ./src/invoice_demo: package implementation
          ./tests: behavior tests
          /fixtures/**/*.sdd: fixture specs
          Generated reports are not committed.

        Owns:
          ./src/invoice_demo/service.py
          @InvoiceService
          Invoice creation workflow and validation rules.

        Can modify:
          ./src/invoice_demo/service.py
          ./tests/test_service.py
          local cache path: ./cache/*.json

        Can read:
          ../README.md
          Architecture notes before editing.

        References:
          ./service.sdd
          ../models/model.sdd
          @InvoiceRepository

        Must:
          Validate invoice input before calling @BillingProvider.
          Persist successful invoices through `@InvoiceRepository`.
          Given a draft invoice exists
          When `create_invoice` is called
          Then an invoice result is returned

        Must not:
          Call external billing adapters from model objects.
          Write outside /fixtures/generated.

        Forbids:
          ./src/invoice_demo/adapters/*
          Direct network access from domain models.

        Depends on:
          @InvoiceRepository
          BillingProvider
          Clock

        Exposes:
          @InvoiceService.create_invoice
          CLI command `invoice-demo create`

        Accepts:
          invoice_id: string
          amount_minor_units: integer
          currency: ISO-4217 code

        Returns:
          InvoiceCreationResult
          status: created

        Raises:
          InvalidInvoiceInputError
          BillingProviderError

        Handles:
          provider timeout
          unsupported currency
          duplicate invoice id

        Tasks:
          [ ] #1 Add happy-path service behavior.
          [x] #2 Cover validation failures.
          [X] #3 Update fixture specs.
          [-] #4 Skip legacy adapter migration.
          [!] #5 Blocked on billing sandbox credentials.
          [?] #6 Decide whether generated reports belong in fixtures.
            Capture the decision in the nearest local spec.

        Scenario: valid invoice is created
          Given a draft invoice with amount 1250
          When the service creates the invoice
          Then the result status is created
          And the repository stores the invoice
          But no generated report is written

        Scenario: unsupported currency is rejected
          Given a draft invoice with currency BTC
          When the service validates the invoice
          Then InvalidInvoiceInputError is raised

        Example:
          input currency: EUR
          input amount_minor_units: 1250
          output status: created

        Example: unsupported currency
          input currency: BTC
          output error: unsupported currency

        Done when:
          All scenarios have focused tests.
          No unresolved explicit paths or @symbols remain.
          The implementation matches `./service.sdd`.
        """.trimIndent()

    override fun getAdditionalHighlightingTagToDescriptorMap(): Map<String, TextAttributesKey> = emptyMap()

    override fun getAttributeDescriptors(): Array<AttributesDescriptor> = DESCRIPTORS

    override fun getColorDescriptors(): Array<ColorDescriptor> = ColorDescriptor.EMPTY_ARRAY

    override fun getDisplayName(): String = "SpecDD"

    override fun customizeColorScheme(scheme: EditorColorsScheme): EditorColorsScheme {
        DEFAULT_ATTRIBUTES.forEach { (key, attributes) ->
            scheme.setAttributes(key, attributes)
        }
        return scheme
    }

    companion object {
        private val DESCRIPTORS: Array<AttributesDescriptor> = arrayOf(
            AttributesDescriptor("Comments//Line comment", SpecDDHighlightingColors.COMMENT),
            AttributesDescriptor("Sections//Default label", SpecDDHighlightingColors.SECTION_LABEL),
            AttributesDescriptor("Sections//Metadata label", SpecDDHighlightingColors.SECTION_META),
            AttributesDescriptor("Sections//Positive label", SpecDDHighlightingColors.SECTION_POSITIVE),
            AttributesDescriptor("Sections//Negative label", SpecDDHighlightingColors.SECTION_NEGATIVE),
            AttributesDescriptor("Sections//Required label", SpecDDHighlightingColors.SECTION_REQUIRED),
            AttributesDescriptor("Sections//Colon", SpecDDHighlightingColors.SECTION_COLON),
            AttributesDescriptor("Sections//Value", SpecDDHighlightingColors.SECTION_VALUE),
            AttributesDescriptor("Body//Indented body text", SpecDDHighlightingColors.CONTINUATION_TEXT),
            AttributesDescriptor("Body//Indent guide", SpecDDHighlightingColors.INDENT),
            AttributesDescriptor("Body//Key-value key", SpecDDHighlightingColors.KEY_VALUE_KEY),
            AttributesDescriptor("Tasks//Done marker", SpecDDHighlightingColors.TASK_DONE),
            AttributesDescriptor("Tasks//Open marker", SpecDDHighlightingColors.TASK_OPEN),
            AttributesDescriptor("Tasks//Blocked marker", SpecDDHighlightingColors.TASK_BLOCKED),
            AttributesDescriptor("Tasks//Question marker", SpecDDHighlightingColors.TASK_QUESTION),
            AttributesDescriptor("Tasks//Skipped marker", SpecDDHighlightingColors.TASK_SKIPPED),
            AttributesDescriptor("Tasks//Invalid marker", SpecDDHighlightingColors.TASK_INVALID),
            AttributesDescriptor("Tasks//Task id", SpecDDHighlightingColors.TASK_ID),
            AttributesDescriptor("Scenario//Step keyword", SpecDDHighlightingColors.SCENARIO_STEP),
            AttributesDescriptor("Inline code//Content", SpecDDHighlightingColors.CODE_SPAN),
            AttributesDescriptor("Inline code//Delimiter", SpecDDHighlightingColors.CODE_SPAN_DELIMITER),
            AttributesDescriptor("References//Path or glob", SpecDDHighlightingColors.PATH),
            AttributesDescriptor("References//Explicit symbol", SpecDDHighlightingColors.SYMBOL),
            AttributesDescriptor("Invalid//Bad character", SpecDDHighlightingColors.BAD_CHARACTER),
        )

        private val DEFAULT_ATTRIBUTES: Map<TextAttributesKey, TextAttributes> = mapOf(
            SpecDDHighlightingColors.INDENT to textAttributes(0x6E7781),
            SpecDDHighlightingColors.CONTINUATION_TEXT to textAttributes(0x7A828E),
            SpecDDHighlightingColors.SECTION_LABEL to textAttributes(0x58A6FF, Font.BOLD),
            SpecDDHighlightingColors.SECTION_META to textAttributes(0x7A004B, Font.BOLD),
            SpecDDHighlightingColors.SECTION_POSITIVE to textAttributes(0x22863A, Font.BOLD),
            SpecDDHighlightingColors.SECTION_NEGATIVE to textAttributes(0xD32F2F, Font.BOLD),
            SpecDDHighlightingColors.SECTION_REQUIRED to textAttributes(0xB26A00, Font.BOLD),
            SpecDDHighlightingColors.TASK_BLOCKED to textAttributes(0xD32F2F, Font.BOLD),
            SpecDDHighlightingColors.CODE_SPAN to textAttributes(0x3A6EA5),
            SpecDDHighlightingColors.CODE_SPAN_DELIMITER to textAttributes(0x7A828E),
            SpecDDHighlightingColors.SYMBOL to textAttributes(0x9B59D0),
        )

        private fun textAttributes(color: Int, fontType: Int = Font.PLAIN): TextAttributes =
            TextAttributes(Color(color), null, null, null, fontType)
    }
}
