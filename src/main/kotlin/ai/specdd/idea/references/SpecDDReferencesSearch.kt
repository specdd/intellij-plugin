package ai.specdd.idea.references

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.util.Computable
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFileSystemItem
import com.intellij.psi.PsiNamedElement
import com.intellij.psi.PsiPolyVariantReference
import com.intellij.psi.PsiReference
import com.intellij.psi.search.RequestResultProcessor
import com.intellij.psi.search.SearchScope
import com.intellij.psi.search.UsageSearchContext
import com.intellij.psi.search.searches.ReferencesSearch
import com.intellij.util.Processor
import com.intellij.util.QueryExecutor

class SpecDDReferencesSearch(
    private val referenceAt: (PsiElement, Int) -> PsiReference? = SpecDDPathReferenceProvider()::getReferenceAt,
    private val searchScopeProvider: (ReferencesSearch.SearchParameters) -> SearchScope = { parameters ->
        parameters.effectiveSearchScope
    },
    private val searchRequestRegistrar: (
        searchTerm: String,
        searchScope: SearchScope,
        target: PsiElement,
        processor: RequestResultProcessor,
        queryParameters: ReferencesSearch.SearchParameters,
    ) -> Unit = { searchTerm, searchScope, target, processor, queryParameters ->
        queryParameters.optimizer.searchWord(
            searchTerm,
            searchScope,
            UsageSearchContext.ANY,
            true,
            target,
            processor,
        )
    },
) : QueryExecutor<PsiReference, ReferencesSearch.SearchParameters> {
    override fun execute(
        queryParameters: ReferencesSearch.SearchParameters,
        consumer: Processor<in PsiReference>,
    ): Boolean {
        val target = queryParameters.elementToSearch
        val processor = SpecDDReferenceRequestProcessor(target, referenceAt)

        searchTermsFor(target).forEach { searchTerm ->
            searchRequestRegistrar(
                searchTerm,
                searchScopeProvider(queryParameters),
                target,
                processor,
                queryParameters,
            )
        }

        return true
    }
}

internal fun searchTermsFor(element: PsiElement): Set<String> {
    val targetName = readAction {
        when (element) {
            is PsiFileSystemItem -> element.name
            is PsiNamedElement -> element.name
            else -> runCatching { element.navigationElement }
                .getOrNull()
                ?.takeIf { navigation -> navigation !== element }
                ?.let { navigation -> (navigation as? PsiNamedElement)?.name }
        }
    }

    return targetName
        ?.takeIf { name -> name.isNotBlank() }
        ?.let { name -> setOf(name) }
        ?: emptySet()
}

private fun <T> readAction(computation: () -> T): T {
    val application = ApplicationManager.getApplication()
    if (null == application) return computation()

    var executed = false
    val result = runCatching {
        application.runReadAction(
            Computable {
                executed = true
                computation()
            },
        )
    }.getOrElse {
        return computation()
    }

    if (!executed) return computation()
    return result
}

internal class SpecDDReferenceRequestProcessor(
    private val target: PsiElement,
    private val referenceAt: (PsiElement, Int) -> PsiReference?,
) : RequestResultProcessor(target) {
    override fun processTextOccurrence(
        element: PsiElement,
        offsetInElement: Int,
        consumer: Processor<in PsiReference>,
    ): Boolean {
        val offsetInFile = element.textRange?.startOffset?.plus(offsetInElement) ?: return true
        val reference = referenceAt(element, offsetInFile) ?: return true

        if (reference is SpecDDPathReference && !reference.participatesInAutomaticRename()) return true
        if (!reference.isReferenceToSearchTarget(target)) return true
        return consumer.process(reference)
    }
}

private fun PsiReference.isReferenceToSearchTarget(target: PsiElement): Boolean {
    if (runCatching { isReferenceTo(target) }.getOrDefault(false)) return true

    val resolved = when (this) {
        is PsiPolyVariantReference -> multiResolve(false).mapNotNull { result -> result.element }
        else -> listOfNotNull(runCatching { resolve() }.getOrNull())
    }

    return resolved.any { element -> element.matchesSearchTarget(target) }
}

private fun PsiElement.matchesSearchTarget(target: PsiElement): Boolean {
    if (this == target) return true
    if (runCatching { isEquivalentTo(target) }.getOrDefault(false)) return true

    val navigation = runCatching { navigationElement }.getOrNull()
    val targetNavigation = runCatching { target.navigationElement }.getOrNull()
    if (null == navigation || null == targetNavigation) return false
    if (navigation == targetNavigation) return true
    return runCatching { navigation.isEquivalentTo(targetNavigation) }.getOrDefault(false)
}
