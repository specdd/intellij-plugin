package ai.specdd.idea.references

import com.intellij.openapi.project.Project
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiElementResolveResult
import com.intellij.psi.PsiPolyVariantReferenceBase
import com.intellij.psi.ResolveResult

class SpecDDSymbolReference(
    element: PsiElement,
    rangeInElement: TextRange,
    private val symbolText: String,
    private val resolver: SpecDDSymbolResolver = SpecDDSymbolResolver(),
) : PsiPolyVariantReferenceBase<PsiElement>(element, rangeInElement, true) {
    override fun multiResolve(incompleteCode: Boolean): Array<ResolveResult> =
        resolver
            .resolve(symbolText, element.project)
            .map { target -> PsiElementResolveResult(target) }
            .toTypedArray()

    override fun getCanonicalText(): String = symbolText
}

class SpecDDSymbolResolver(
    private val contributorProvider: () -> List<Any> = { chooseByNameContributors() },
) {
    fun resolve(symbolText: String, project: Project): List<PsiElement> {
        if (!isSymbolCandidate(symbolText)) return emptyList()

        val targets = contributorProvider()
            .flatMap { contributor ->
                symbolLookups(symbolText).flatMap { lookup ->
                    itemsByName(contributor, lookup.name, lookup.pattern, project)
                }
            }
            .mapNotNull { item -> item as? PsiElement }
            .filter { element -> element.isValid }
            .distinctBy { element -> element.navigationKey() }
            .take(MAX_SYMBOL_TARGETS)

        return targets
    }

    private fun itemsByName(contributor: Any, name: String, pattern: String, project: Project): List<Any> =
        runCatching {
            val method = contributor.javaClass.methods.firstOrNull { method ->
                "getItemsByName" == method.name && 4 == method.parameterCount
            } ?: return emptyList()

            @Suppress("UNCHECKED_CAST")
            (method.invoke(contributor, name, pattern, project, false) as? Array<Any>)?.toList() ?: emptyList()
        }.getOrDefault(emptyList())
}

private fun symbolLookups(symbolText: String): List<SymbolLookup> {
    val exact = SymbolLookup(symbolText, symbolText)
    val simpleName = symbolText.substringAfterLast('.')
    if (simpleName == symbolText) return listOf(exact)
    return listOf(exact, SymbolLookup(simpleName, symbolText))
}

private data class SymbolLookup(
    val name: String,
    val pattern: String,
)

private fun chooseByNameContributors(): List<Any> =
    runCatching {
        val extensionPointNameClass = Class.forName("com.intellij.openapi.extensions.ExtensionPointName")
        val createMethod = extensionPointNameClass.methods.first { method ->
            "create" == method.name && 1 == method.parameterCount
        }

        CHOOSE_BY_NAME_EXTENSION_POINTS.flatMap { extensionPointName ->
            val extensionPoint = createMethod.invoke(null, extensionPointName)
            val getExtensionList = extensionPoint.javaClass.methods.first { method ->
                "getExtensionList" == method.name && 0 == method.parameterCount
            }

            val extensions = runCatching { getExtensionList.invoke(extensionPoint) }.getOrNull()
            if (extensions is List<*>) extensions.filterNotNull() else emptyList()
        }
    }.getOrDefault(emptyList())

private fun isSymbolCandidate(text: String): Boolean {
    if (text.isBlank()) return false
    if (text.any { character -> character.isWhitespace() }) return false
    if (text.startsWith("./") || text.startsWith("../") || text.startsWith("/")) return false
    return text.all { character -> character.isLetterOrDigit() || '_' == character || '$' == character || '.' == character }
}

private fun PsiElement.navigationKey(): String =
    runCatching { navigationElement.textRange?.toString() }.getOrNull()
        ?: runCatching { containingFile?.virtualFile?.path }.getOrNull()
        ?: toString()

private val CHOOSE_BY_NAME_EXTENSION_POINTS = listOf(
    "com.intellij.gotoSymbolContributor",
    "com.intellij.gotoClassContributor",
)

private const val MAX_SYMBOL_TARGETS = 50
