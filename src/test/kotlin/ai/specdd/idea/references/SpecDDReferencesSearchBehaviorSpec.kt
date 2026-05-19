package ai.specdd.idea.references

import ai.specdd.idea.file
import com.intellij.openapi.application.Application
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.TextRange
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.module.Module
import com.intellij.openapi.util.Computable
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFileSystemItem
import com.intellij.psi.PsiNamedElement
import com.intellij.psi.PsiReference
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.search.PsiSearchHelper
import com.intellij.psi.search.SearchRequestCollector
import com.intellij.psi.search.SearchSession
import com.intellij.psi.search.searches.ReferencesSearch
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import java.lang.reflect.InvocationHandler
import java.lang.reflect.Proxy

class SpecDDReferencesSearchBehaviorSpec : BehaviorSpec({
    given("SpecDD references search") {
        `when`("search terms are derived from IntelliJ targets") {
            then("it uses file-system and named-element names") {
                searchTermsFor(psiFileSystemItem("invoice.py")).shouldContainExactly("invoice.py")
                searchTermsFor(psiNamedElement("Invoice")).shouldContainExactly("Invoice")
                searchTermsFor(searchPsiElement("light", navigationElement = psiNamedElement("Receipt")))
                    .shouldContainExactly("Receipt")
                searchTermsFor(searchPsiElement("anonymous")).shouldContainExactly()
            }
        }

        `when`("search terms read a PSI target name on a pooled-thread search path") {
            then("it wraps target name access in a read action") {
                var readActionActive = false
                val previousApplication = ApplicationManager.getApplication()
                ApplicationManager.setApplication(applicationThatTracksReadActions { active ->
                    readActionActive = active
                })
                val target = psiNamedElement("Invoice") { readActionActive }

                try {
                    searchTermsFor(target).shouldContainExactly("Invoice")
                } finally {
                    ApplicationManager.setApplication(previousApplication)
                }
            }
        }

        `when`("the installed application does not execute read actions in a lightweight test") {
            then("it falls back to direct target-name extraction") {
                val previousApplication = ApplicationManager.getApplication()
                ApplicationManager.setApplication(applicationThatReturnsNullForReadActions())

                try {
                    searchTermsFor(psiNamedElement("Invoice")).shouldContainExactly("Invoice")
                } finally {
                    ApplicationManager.setApplication(previousApplication)
                }
            }
        }

        `when`("the installed application rejects read actions in a lightweight test") {
            then("it falls back to direct target-name extraction") {
                val previousApplication = ApplicationManager.getApplication()
                ApplicationManager.setApplication(applicationThatThrowsForReadActions())

                try {
                    searchTermsFor(psiNamedElement("Invoice")).shouldContainExactly("Invoice")
                } finally {
                    ApplicationManager.setApplication(previousApplication)
                }
            }
        }

        `when`("query execution is requested for a named target") {
            then("it registers a word search request for the target name") {
                lateinit var scope: GlobalSearchScope
                val targetProject = projectWithSearchHelper { scope }
                scope = searchScope(targetProject)
                val target = psiNamedElement("Invoice", targetProject)
                val collector = SearchRequestCollector(SearchSession(target))
                val previousApplication = ApplicationManager.getApplication()
                ApplicationManager.setApplication(applicationThatRunsReadActions())

                try {
                    val parameters = ReferencesSearch.SearchParameters(
                        target,
                        scope,
                        false,
                        collector,
                    )

                    val registeredTerms = mutableListOf<String>()
                    SpecDDReferencesSearch(
                        referenceAt = { _, _ -> null },
                        searchScopeProvider = { searchParameters -> searchParameters.scopeDeterminedByUser },
                        searchRequestRegistrar = { searchTerm, _, _, _, _ -> registeredTerms.add(searchTerm) },
                    ).execute(parameters) { true }

                    registeredTerms.shouldContainExactly("Invoice")
                    collector.takeSearchRequests().shouldContainExactly()
                } finally {
                    ApplicationManager.setApplication(previousApplication)
                }
            }
        }

        `when`("query execution uses default IntelliJ request registration") {
            then("it adds a search request through the optimizer") {
                lateinit var scope: GlobalSearchScope
                val targetProject = projectWithSearchHelper { scope }
                scope = searchScope(targetProject)
                val target = psiNamedElement("Invoice", targetProject)
                val collector = SearchRequestCollector(SearchSession(target))
                val previousApplication = ApplicationManager.getApplication()
                ApplicationManager.setApplication(applicationThatRunsReadActions())

                try {
                    val parameters = ReferencesSearch.SearchParameters(
                        target,
                        scope,
                        false,
                        collector,
                    )

                    SpecDDReferencesSearch().execute(parameters) { true }
                } finally {
                    ApplicationManager.setApplication(previousApplication)
                }

                collector.takeSearchRequests().map { request -> request.word }.shouldContainExactly("Invoice")
            }
        }

        `when`("a text occurrence resolves to the searched target") {
            then("it reports the SpecDD reference") {
                val target = psiNamedElement("Invoice")
                val occurrence = searchPsiElement("References:\n  @invoice_demo.models.Invoice", textRange = TextRange(40, 80))
                val reference = psiReference(target)
                val accepted = mutableListOf<PsiReference>()
                val processor = SpecDDReferenceRequestProcessor(target) { element, offset ->
                    (element === occurrence) shouldBe true
                    offset shouldBe 55
                    reference
                }

                processor.processTextOccurrence(occurrence, 15) { psiReference ->
                    accepted.add(psiReference)
                    true
                }

                accepted.shouldContainExactly(reference)
            }
        }

        `when`("a text occurrence has no matching reference") {
            then("it ignores the occurrence") {
                val target = psiNamedElement("Invoice")
                val otherTarget = psiNamedElement("Receipt")
                val accepted = mutableListOf<PsiReference>()
                val processor = SpecDDReferenceRequestProcessor(target) { _, _ -> psiReference(otherTarget) }

                val keepProcessing = processor.processTextOccurrence(searchPsiElement("@Receipt"), 1) { psiReference ->
                    accepted.add(psiReference)
                    true
                }

                keepProcessing shouldBe true
                accepted.shouldContainExactly()
            }
        }

        `when`("a text occurrence has no reference at the offset") {
            then("it keeps searching") {
                val target = psiNamedElement("Invoice")
                val accepted = mutableListOf<PsiReference>()
                val processor = SpecDDReferenceRequestProcessor(target) { _, _ -> null }

                val keepProcessing = processor.processTextOccurrence(searchPsiElement("@Invoice"), 1) { psiReference ->
                    accepted.add(psiReference)
                    true
                }

                keepProcessing shouldBe true
                accepted.shouldContainExactly()
            }
        }

        `when`("a text occurrence is a glob reference") {
            then("it excludes the reference from automatic rename search") {
                val root = ai.specdd.idea.testVirtualRoot()
                val target = root.file("src/invoice.py")
                val reference = SpecDDPathReference(
                    element = searchPsiElement("./src/*.py"),
                    rangeInElement = TextRange(0, "./src/*.py".length),
                    candidate = SpecDDPathCandidate("./src/*.py", TextRange(0, "./src/*.py".length), true),
                    context = SpecDDPathResolutionContext(root, root),
                    targetMapper = { virtualFile, _ -> if (target == virtualFile) psiNamedElement("invoice.py") else null },
                )
                val accepted = mutableListOf<PsiReference>()
                val processor = SpecDDReferenceRequestProcessor(psiNamedElement("invoice.py")) { _, _ -> reference }

                val keepProcessing = processor.processTextOccurrence(searchPsiElement("./src/*.py"), 1) { psiReference ->
                    accepted.add(psiReference)
                    true
                }

                keepProcessing shouldBe true
                accepted.shouldContainExactly()
            }
        }
    }
})

private fun SpecDDReferencesSearch.execute(
    parameters: ReferencesSearch.SearchParameters,
    processor: (PsiReference) -> Boolean,
): Boolean = execute(parameters, com.intellij.util.Processor { reference -> processor(reference) })

private fun psiNamedElement(name: String, project: Project = project(null)): PsiNamedElement =
    psiNamedElement(name, project) { true }

private fun psiNamedElement(name: String, project: Project = project(null), canReadName: () -> Boolean): PsiNamedElement =
    Proxy.newProxyInstance(
        PsiNamedElement::class.java.classLoader,
        arrayOf(PsiNamedElement::class.java),
        InvocationHandler { proxy, method, args ->
            when (method.name) {
                "toString" -> "PsiNamedElement($name)"
                "getName" -> if (canReadName()) name else error("PSI name read outside read action")
                "getProject" -> project
                "getNavigationElement" -> proxy
                "isEquivalentTo" -> proxy == args?.firstOrNull()
                "isValid" -> true
                else -> defaultReturn(method.returnType)
            }
        },
    ) as PsiNamedElement

private fun psiFileSystemItem(name: String): PsiFileSystemItem =
    Proxy.newProxyInstance(
        PsiFileSystemItem::class.java.classLoader,
        arrayOf(PsiFileSystemItem::class.java),
        InvocationHandler { proxy, method, args ->
            when (method.name) {
                "toString" -> "PsiFileSystemItem($name)"
                "getName" -> name
                "getProject" -> project(null)
                "getNavigationElement" -> proxy
                "isEquivalentTo" -> proxy == args?.firstOrNull()
                "isValid" -> true
                "isDirectory" -> false
                else -> defaultReturn(method.returnType)
            }
        },
    ) as PsiFileSystemItem

private fun searchPsiElement(
    text: String,
    textRange: TextRange = TextRange(0, text.length),
    navigationElement: PsiElement? = null,
): PsiElement =
    Proxy.newProxyInstance(
        PsiElement::class.java.classLoader,
        arrayOf(PsiElement::class.java),
        InvocationHandler { proxy, method, args ->
            when (method.name) {
                "toString" -> "PsiElement($text)"
                "getText" -> text
                "getTextRange" -> textRange
                "getProject" -> project(null)
                "getNavigationElement" -> navigationElement ?: proxy
                "isEquivalentTo" -> proxy == args?.firstOrNull()
                "isValid" -> true
                else -> defaultReturn(method.returnType)
            }
        },
    ) as PsiElement

private fun psiReference(target: PsiElement): PsiReference =
    Proxy.newProxyInstance(
        PsiReference::class.java.classLoader,
        arrayOf(PsiReference::class.java),
        InvocationHandler { _, method, args ->
            when (method.name) {
                "toString" -> "PsiReference"
                "resolve" -> target
                "isReferenceTo" -> target === args?.firstOrNull()
                "getCanonicalText" -> "reference"
                else -> defaultReturn(method.returnType)
            }
        },
    ) as PsiReference

private fun applicationThatRunsReadActions(): Application =
    Proxy.newProxyInstance(
        Application::class.java.classLoader,
        arrayOf(Application::class.java),
        InvocationHandler { _, method, args ->
            when (method.name) {
                "runReadAction" -> args?.firstOrNull()?.invokeAction()
                "isUnitTestMode" -> true
                "isHeadlessEnvironment" -> true
                else -> defaultReturn(method.returnType)
            }
        },
    ) as Application

private fun applicationThatTracksReadActions(setActive: (Boolean) -> Unit): Application =
    Proxy.newProxyInstance(
        Application::class.java.classLoader,
        arrayOf(Application::class.java),
        InvocationHandler { _, method, args ->
            when (method.name) {
                "runReadAction" -> {
                    setActive(true)
                    try {
                        args?.firstOrNull()?.invokeAction()
                    } finally {
                        setActive(false)
                    }
                }

                "isUnitTestMode" -> true
                "isHeadlessEnvironment" -> true
                else -> defaultReturn(method.returnType)
            }
        },
    ) as Application

private fun applicationThatReturnsNullForReadActions(): Application =
    Proxy.newProxyInstance(
        Application::class.java.classLoader,
        arrayOf(Application::class.java),
        InvocationHandler { _, method, _ ->
            when (method.name) {
                "runReadAction" -> null
                "isUnitTestMode" -> true
                "isHeadlessEnvironment" -> true
                else -> defaultReturn(method.returnType)
            }
        },
    ) as Application

private fun applicationThatThrowsForReadActions(): Application =
    Proxy.newProxyInstance(
        Application::class.java.classLoader,
        arrayOf(Application::class.java),
        InvocationHandler { _, method, _ ->
            when (method.name) {
                "runReadAction" -> error("Read actions unavailable")
                "isUnitTestMode" -> true
                "isHeadlessEnvironment" -> true
                else -> defaultReturn(method.returnType)
            }
        },
    ) as Application

private fun Any.invokeAction(): Any? {
    if (this is Computable<*>) return compute()
    if (this is Runnable) {
        run()
        return null
    }

    return runCatching {
        val method = javaClass.methods.firstOrNull { method ->
            ("compute" == method.name || "run" == method.name) && 0 == method.parameterCount
        } ?: return@runCatching null
        method.isAccessible = true
        method.invoke(this)
    }.getOrNull()
}

private fun projectWithSearchHelper(useScope: () -> GlobalSearchScope): Project {
    val searchHelper = Proxy.newProxyInstance(
        PsiSearchHelper::class.java.classLoader,
        arrayOf(PsiSearchHelper::class.java),
        InvocationHandler { _, method, _ ->
            when (method.name) {
                "getUseScope" -> useScope()
                else -> defaultReturn(method.returnType)
            }
        },
    ) as PsiSearchHelper

    return Proxy.newProxyInstance(
        Project::class.java.classLoader,
        arrayOf(Project::class.java),
        InvocationHandler { _, method, args ->
            when (method.name) {
                "getService" -> if (PsiSearchHelper::class.java == args?.firstOrNull()) searchHelper else null
                "isDisposed" -> false
                else -> defaultReturn(method.returnType)
            }
        },
    ) as Project
}

private fun searchScope(project: Project): GlobalSearchScope =
    object : GlobalSearchScope(project) {
        override fun contains(file: VirtualFile): Boolean = true

        override fun isSearchInModuleContent(aModule: Module): Boolean = true

        override fun isSearchInLibraries(): Boolean = false
    }

private fun defaultReturn(returnType: Class<*>): Any? =
    when (returnType) {
        Boolean::class.javaPrimitiveType -> false
        Int::class.javaPrimitiveType -> 0
        Long::class.javaPrimitiveType -> 0L
        Unit::class.javaPrimitiveType -> Unit
        else -> null
    }
