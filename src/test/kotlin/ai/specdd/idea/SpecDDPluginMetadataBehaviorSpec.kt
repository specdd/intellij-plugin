package ai.specdd.idea

import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldNotContain
import io.kotest.matchers.shouldBe
import java.nio.file.Path
import javax.xml.parsers.DocumentBuilderFactory

class SpecDDPluginMetadataBehaviorSpec : BehaviorSpec({
    given("the SpecDD plugin metadata") {
        `when`("marketplace metadata is declared") {
            then("it exposes homepage, source, issues, and change notes") {
                val pluginXml = readPluginXml()
                val root = pluginXml.documentElement
                val description = pluginXml
                    .getElementsByTagName("description")
                    .item(0)
                    .textContent
                val changeNotes = pluginXml
                    .getElementsByTagName("change-notes")
                    .item(0)
                    .textContent

                root.getAttribute("url") shouldBe "https://specdd.ai"
                description.contains("SpecDD™ is an open-source framework") shouldBe true
                description.contains("https://specdd.ai") shouldBe true
                description.contains("https://github.com/specdd/intellij-plugin") shouldBe true
                description.contains("https://github.com/specdd/intellij-plugin/issues") shouldBe true
                changeNotes.contains("Initial SpecDD language support") shouldBe true
            }
        }

        `when`("language extension points are registered") {
            then("it uses the IntelliJ language syntax highlighter extension point") {
                val pluginXml = readPluginXml()
                val extensionNames = pluginXml.extensionElementNames()

                extensionNames.shouldContain("lang.syntaxHighlighterFactory")
                extensionNames.shouldContain("lang.parserDefinition")
                extensionNames.shouldContain("langCodeStyleSettingsProvider")
                extensionNames.shouldContain("lang.psiStructureViewFactory")
                extensionNames.shouldContain("documentationProvider")
                extensionNames.shouldContain("completion.contributor")
                extensionNames.shouldContain("psi.referenceContributor")
                extensionNames.shouldNotContain("syntaxHighlighterFactory")
            }
        }

        `when`("path reference contributor is registered") {
            then("it uses the reference contributor implementation attribute") {
                val pluginXml = readPluginXml()
                val referenceContributor = pluginXml
                    .getElementsByTagName("psi.referenceContributor")
                    .item(0)
                    .attributes

                referenceContributor.getNamedItem("implementation").nodeValue shouldBe
                        "ai.specdd.idea.references.SpecDDPathReferenceContributor"
                referenceContributor.getNamedItem("language").nodeValue shouldBe "SpecDD"
            }
        }

        `when`("documentation provider is registered") {
            then("it uses the documentation provider implementation attribute") {
                val pluginXml = readPluginXml()
                val documentationProvider = pluginXml
                    .getElementsByTagName("documentationProvider")
                    .item(0)
                    .attributes

                documentationProvider.getNamedItem("implementation").nodeValue shouldBe
                        "ai.specdd.idea.documentation.SpecDDDocumentationProvider"
            }
        }

        `when`("code style settings provider is registered") {
            then("it uses the code style provider implementation attribute") {
                val pluginXml = readPluginXml()
                val codeStyleProvider = pluginXml
                    .getElementsByTagName("langCodeStyleSettingsProvider")
                    .item(0)
                    .attributes

                codeStyleProvider.getNamedItem("implementation").nodeValue shouldBe
                        "ai.specdd.idea.SpecDDCodeStyleSettingsProvider"
            }
        }

        `when`("plugin compatibility is declared") {
            then("it keeps the intended minimum IntelliJ build") {
                val pluginXml = readPluginXml()
                val ideaVersion = pluginXml
                    .getElementsByTagName("idea-version")
                    .item(0)
                    .attributes

                ideaVersion.getNamedItem("since-build").nodeValue shouldBe "223"
            }
        }
    }
})

private fun readPluginXml() =
    DocumentBuilderFactory
        .newInstance()
        .newDocumentBuilder()
        .parse(Path.of("src/main/resources/META-INF/plugin.xml").toFile())

private fun org.w3c.dom.Document.extensionElementNames(): List<String> {
    val extensions = getElementsByTagName("extensions").item(0).childNodes
    return (0 until extensions.length)
        .map { extensions.item(it) }
        .filter { org.w3c.dom.Node.ELEMENT_NODE == it.nodeType }
        .map { it.nodeName }
}
