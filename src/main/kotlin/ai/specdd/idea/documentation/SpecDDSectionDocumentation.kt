package ai.specdd.idea.documentation

import ai.specdd.idea.parser.SpecDDLanguageFacts

object SpecDDSectionDocumentation {
    private const val RESOURCE_PATH = "/ai/specdd/idea/documentation/sections.txt"

    private val descriptions: Map<String, String> = loadDescriptions()

    fun htmlFor(label: String): String? {
        val description = descriptions[label] ?: return null
        return buildString {
            append("<div class=\"definition\"><b>")
            append(escapeHtml(label))
            append("</b></div>")
            paragraphs(description).forEach { paragraph ->
                append("<p>")
                append(escapeHtml(paragraph))
                append("</p>")
            }
        }
    }

    fun labels(): Set<String> = descriptions.keys

    fun coversKnownSections(): Boolean = descriptions.keys == SpecDDLanguageFacts.sectionLabels.toSet()

    private fun loadDescriptions(): Map<String, String> {
        val resource = SpecDDSectionDocumentation::class.java.getResource(RESOURCE_PATH) ?: error("Missing SpecDD section documentation resource $RESOURCE_PATH")
        return parseDescriptions(resource.readText())
    }

    internal fun parseDescriptions(text: String): Map<String, String> {
        val descriptions = linkedMapOf<String, String>()
        var currentLabel: String? = null
        val currentBody = mutableListOf<String>()

        fun flush() {
            val label = currentLabel ?: return
            descriptions[label] = currentBody.joinToString("\n").trim()
            currentBody.clear()
        }

        text.lineSequence().forEach { line ->
            if (line.startsWith("## ")) {
                flush()
                currentLabel = line.removePrefix("## ").trim()
            } else {
                currentBody.add(line)
            }
        }
        flush()

        return descriptions
    }

    private fun paragraphs(text: String): List<String> =
        text
            .split(Regex("\\n\\s*\\n"))
            .map { paragraph -> paragraph.lineSequence().joinToString(" ") { line -> line.trim() }.trim() }
            .filter { paragraph -> paragraph.isNotEmpty() }

    private fun escapeHtml(text: String): String =
        text
            .replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;")
}
