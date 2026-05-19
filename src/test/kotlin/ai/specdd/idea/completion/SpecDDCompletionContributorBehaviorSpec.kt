package ai.specdd.idea.completion

import ai.specdd.idea.SpecDDFileType
import ai.specdd.idea.SpecDDLanguage
import ai.specdd.idea.file
import ai.specdd.idea.testVirtualRoot
import com.intellij.codeInsight.completion.*
import com.intellij.codeInsight.lookup.LookupElement
import com.intellij.openapi.application.Application
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.editor.Document
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.fileTypes.FileType
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.TextRange
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldNotContain
import io.kotest.matchers.shouldBe
import java.lang.reflect.InvocationHandler
import java.lang.reflect.Proxy
import java.util.function.Consumer

class SpecDDCompletionContributorBehaviorSpec : BehaviorSpec({
    given("a SpecDD completion contributor") {
        initializeLightPlatformApplication()
        val contributor = SpecDDCompletionContributor()

        `when`("basic completion is requested for a SpecDD section prefix") {
            then("it adds matching section header lookup elements with the computed prefix") {
                val result = RecordingCompletionResultSet(contributor)

                contributor.fillCompletionVariants(
                    parameters = completionParameters("Pur", 3, SpecDDFileType(), CompletionType.BASIC),
                    result = result,
                )

                result.prefixes shouldContain "Pur"
                result.lookupStrings shouldContain "Purpose: "
                result.lookupStrings.shouldNotContain("Spec: ")
            }
        }

        `when`("completion is not basic") {
            then("it adds no lookup elements") {
                val result = RecordingCompletionResultSet(contributor)

                contributor.fillCompletionVariants(
                    parameters = completionParameters("Pur", 3, SpecDDFileType(), CompletionType.SMART),
                    result = result,
                )

                result.lookupStrings shouldBe emptyList()
            }
        }

        `when`("completion is requested for another file type") {
            then("it adds no lookup elements") {
                val result = RecordingCompletionResultSet(contributor)

                contributor.fillCompletionVariants(
                    parameters = completionParameters("Pur", 3, mockFileType(), CompletionType.BASIC),
                    result = result,
                )

                result.lookupStrings shouldBe emptyList()
            }
        }

        `when`("completion is requested outside a section label prefix") {
            then("it adds no lookup elements") {
                val result = RecordingCompletionResultSet(contributor)

                contributor.fillCompletionVariants(
                    parameters = completionParameters("  Pur", 5, SpecDDFileType(), CompletionType.BASIC),
                    result = result,
                )

                result.lookupStrings shouldBe emptyList()
            }
        }

        `when`("completion is requested for an inline local symbol prefix") {
            then("it adds local symbol lookup elements") {
                val text = "Purpose:\n  Use SpecDD.Parser.classify\nMust:\n  SpecDD.P"
                val result = RecordingCompletionResultSet(contributor)

                contributor.fillCompletionVariants(
                    parameters = completionParameters(text, text.length, SpecDDFileType(), CompletionType.BASIC),
                    result = result,
                )

                result.prefixes shouldContain "SpecDD.P"
                result.lookupStrings shouldContain "SpecDD.Parser.classify"
            }
        }

        `when`("completion is requested for an inline project path prefix") {
            then("it adds project path lookup elements") {
                val root = testVirtualRoot()
                val specFile = root.file("app.sdd")
                root.file("main.sdd")
                val text = "References:\n  /main"
                val result = RecordingCompletionResultSet(contributor)

                contributor.fillCompletionVariants(
                    parameters = completionParameters(
                        text = text,
                        offset = text.length,
                        fileType = SpecDDFileType(),
                        completionType = CompletionType.BASIC,
                        virtualFile = specFile,
                    ),
                    result = result,
                )

                result.prefixes shouldContain "/main"
                result.lookupStrings shouldContain "/main.sdd"
            }
        }
    }
})

private class RecordingCompletionResultSet(
    private val sourceContributor: CompletionContributor,
    private val elements: MutableList<LookupElement> = mutableListOf(),
    val prefixes: MutableList<String> = mutableListOf(),
) : CompletionResultSet(PlainPrefixMatcher(""), Consumer<CompletionResult> {}, sourceContributor) {
    val lookupStrings: List<String>
        get() = elements.map { element -> element.lookupString }

    override fun addElement(element: LookupElement) {
        elements.add(element)
    }

    override fun withPrefixMatcher(prefixMatcher: PrefixMatcher): CompletionResultSet {
        prefixes.add(prefixMatcher.prefix)
        return RecordingCompletionResultSet(sourceContributor, elements, prefixes)
    }

    override fun withPrefixMatcher(prefix: String): CompletionResultSet {
        prefixes.add(prefix)
        return RecordingCompletionResultSet(sourceContributor, elements, prefixes)
    }

    override fun withRelevanceSorter(sorter: CompletionSorter): CompletionResultSet = this

    override fun addLookupAdvertisement(text: String) = Unit

    override fun caseInsensitive(): CompletionResultSet = this

    override fun restartCompletionOnPrefixChange(prefixCondition: com.intellij.patterns.ElementPattern<String>) = Unit

    override fun restartCompletionWhenNothingMatches() = Unit
}

private fun completionParameters(
    text: String,
    offset: Int,
    fileType: FileType,
    completionType: CompletionType,
    virtualFile: VirtualFile? = null,
): CompletionParameters =
    CompletionParameters(
        psiElement(text, fileType),
        psiFile(text, fileType, virtualFile),
        completionType,
        offset,
        1,
        editor(text),
        completionProcess(),
    )

private fun editor(text: String): Editor =
    Proxy.newProxyInstance(
        Editor::class.java.classLoader,
        arrayOf(Editor::class.java),
        InvocationHandler { _, method, args ->
            when (method.name) {
                "getDocument" -> document(text)
                else -> defaultValue(method.returnType, args)
            }
        },
    ) as Editor

private fun document(text: String): Document =
    Proxy.newProxyInstance(
        Document::class.java.classLoader,
        arrayOf(Document::class.java),
        InvocationHandler { _, method, args ->
            when (method.name) {
                "getCharsSequence" -> text
                "getTextLength" -> text.length
                else -> defaultValue(method.returnType, args)
            }
        },
    ) as Document

private fun completionProcess(): CompletionProcess =
    Proxy.newProxyInstance(
        CompletionProcess::class.java.classLoader,
        arrayOf(CompletionProcess::class.java),
        InvocationHandler { _, method, args ->
            when (method.name) {
                "isAutopopupCompletion" -> false
                else -> defaultValue(method.returnType, args)
            }
        },
    ) as CompletionProcess

private fun psiFile(text: String, fileType: FileType, virtualFile: VirtualFile? = null): PsiFile =
    Proxy.newProxyInstance(
        PsiFile::class.java.classLoader,
        arrayOf(PsiFile::class.java),
        InvocationHandler { _, method, args ->
            when (method.name) {
                "getText" -> text
                "getFileType" -> fileType
                "getLanguage" -> SpecDDLanguage
                "getProject" -> project(virtualFile?.path)
                "getVirtualFile" -> virtualFile
                else -> defaultValue(method.returnType, args)
            }
        },
    ) as PsiFile

private fun psiElement(text: String, fileType: FileType): PsiElement =
    Proxy.newProxyInstance(
        PsiElement::class.java.classLoader,
        arrayOf(PsiElement::class.java),
        InvocationHandler { _, method, args ->
            when (method.name) {
                "getText" -> text
                "getContainingFile" -> psiFile(text, fileType)
                "getLanguage" -> SpecDDLanguage
                "getTextRange" -> TextRange(0, text.length)
                "isValid" -> true
                else -> defaultValue(method.returnType, args)
            }
        },
    ) as PsiElement

private fun mockFileType(): FileType =
    Proxy.newProxyInstance(
        FileType::class.java.classLoader,
        arrayOf(FileType::class.java),
        InvocationHandler { _, method, args ->
            when (method.name) {
                "getName" -> "Plain"
                "getDescription" -> "Plain text"
                "getDefaultExtension" -> "txt"
                "isBinary" -> false
                else -> defaultValue(method.returnType, args)
            }
        },
    ) as FileType

private fun project(basePath: String?): Project =
    Proxy.newProxyInstance(
        Project::class.java.classLoader,
        arrayOf(Project::class.java),
        InvocationHandler { _, method, args ->
            when (method.name) {
                "getBasePath" -> basePath
                "isDisposed" -> false
                else -> defaultValue(method.returnType, args)
            }
        },
    ) as Project

private fun initializeLightPlatformApplication() {
    if (null != ApplicationManager.getApplication()) return

    val completionService = RecordingCompletionService()
    val application = Proxy.newProxyInstance(
        Application::class.java.classLoader,
        arrayOf(Application::class.java),
        InvocationHandler { _, method, args ->
            when (method.name) {
                "getService" -> {
                    if (CompletionService::class.java == args?.get(0)) completionService else null
                }

                "isUnitTestMode" -> true
                "isHeadlessEnvironment" -> true
                else -> defaultValue(method.returnType, args)
            }
        },
    ) as Application
    ApplicationManager.setApplication(application)
}

@Suppress("OVERRIDE_DEPRECATION")
private class RecordingCompletionService : CompletionService() {
    override fun setAdvertisementText(advertisementText: String?) = Unit

    override fun createResultSet(
        parameters: CompletionParameters,
        consumer: com.intellij.util.Consumer<in CompletionResult>,
        contributor: CompletionContributor,
        matcher: PrefixMatcher,
    ): CompletionResultSet = RecordingCompletionResultSet(contributor)

    override fun suggestPrefix(parameters: CompletionParameters): String = ""

    override fun createMatcher(prefix: String, typoTolerant: Boolean): PrefixMatcher = PlainPrefixMatcher(prefix)

    override fun getCurrentCompletion(): CompletionProcess? = null

    override fun defaultSorter(parameters: CompletionParameters, matcher: PrefixMatcher): CompletionSorter =
        emptySorter()

    override fun emptySorter(): CompletionSorter = CompletionSorter.emptySorter()
}

private fun defaultValue(returnType: Class<*>, args: Array<Any?>?): Any? {
    if (Boolean::class.javaPrimitiveType == returnType) return false
    if (Int::class.javaPrimitiveType == returnType) return 0
    if (Long::class.javaPrimitiveType == returnType) return 0L
    if (Void.TYPE == returnType) return Unit
    if (String::class.java == returnType) return ""
    return null
}
