package ai.specdd.idea.structure

import ai.specdd.idea.SpecDDLanguage
import ai.specdd.idea.SpecDDPsiFile
import com.intellij.ide.structureView.StructureViewModel
import com.intellij.ide.structureView.StructureViewTreeElement
import com.intellij.ide.structureView.TreeBasedStructureViewBuilder
import com.intellij.ide.util.treeView.smartTree.TreeElement
import com.intellij.lang.Language
import com.intellij.mock.MockApplication
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.FileViewProvider
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import sun.misc.Unsafe
import java.lang.reflect.InvocationHandler
import java.lang.reflect.Proxy

class SpecDDStructureViewFactoryBehaviorSpec : BehaviorSpec({
    given("a SpecDD structure view factory") {
        val factory = SpecDDStructureViewFactory()

        `when`("a non-SpecDD PSI file is requested") {
            then("it does not provide a builder") {
                factory.getStructureViewBuilder(psiFile("plain.sdd", "Spec: Example")).shouldBeNull()
            }
        }

        `when`("a SpecDD PSI file is requested") {
            then("it provides a tree-based model builder") {
                val disposable = Disposer.newDisposable()
                try {
                    MockApplication.setUp(disposable)

                    val builder = factory.getStructureViewBuilder(uninitializedSpecDDPsiFile())

                    builder.shouldNotBeNull()
                    builder.shouldBeInstanceOf<TreeBasedStructureViewBuilder>()
                    builder.createStructureViewModel(null).shouldBeInstanceOf<StructureViewModel>()
                } finally {
                    Disposer.dispose(disposable)
                }
            }
        }
    }

    given("a SpecDD structure tree root") {
        `when`("children are requested") {
            then("it exposes one child per extracted section") {
                val root = SpecDDStructureTreeElement.root(
                    psiFile(
                        name = "main.sdd",
                        text = """
                            |Spec: Example
                            |Purpose:
                            |Scenario: first
                            |Example:
                        """.trimMargin(),
                    ),
                )

                root.presentation.presentableText shouldBe "main.sdd"
                root.presentation.locationString.shouldBeNull()
                root.presentation.getIcon(false).shouldNotBeNull()
                root.canNavigate() shouldBe false
                root.canNavigateToSource() shouldBe false
                root.navigate(false)

                val children = root.children
                children.map { child -> child.presentation.presentableText }.shouldContainExactly(
                    "Spec: Example",
                    "Purpose",
                    "Scenario: first",
                    "Example",
                )
                children.first().children.shouldContainExactly(*TreeElement.EMPTY_ARRAY)
                children.first().shouldBeInstanceOf<StructureViewTreeElement>()
                (children.first() as StructureViewTreeElement).value.shouldBeInstanceOf<SpecDDStructureSection>()
                SpecDDStructureTreeElement
                    .section(
                        file = psiFile("section.sdd", "Spec: Example"),
                        section = (children.first() as StructureViewTreeElement).value as SpecDDStructureSection,
                    )
                    .presentation
                    .presentableText shouldBe "Spec: Example"
            }
        }

    }

    given("a SpecDD structure view model") {
        `when`("element info is requested") {
            then("it treats section nodes as leaves") {
                val file = psiFile("main.sdd", "Spec: Example\nPurpose:")
                val root = SpecDDStructureTreeElement.root(file)
                val model = SpecDDStructureViewModel(root)
                val firstSection = root.children.first() as StructureViewTreeElement

                model.root shouldBe root
                model.currentEditorElement.shouldBeNull()
                model.shouldEnterElement(firstSection) shouldBe false
                model.isAlwaysShowsPlus(root) shouldBe false
                model.isAlwaysShowsPlus(firstSection) shouldBe false
                model.isAlwaysLeaf(root) shouldBe false
                model.isAlwaysLeaf(firstSection) shouldBe true
                model.groupers.shouldContainExactly()
                model.sorters.shouldContainExactly()
                model.filters.shouldContainExactly()

                model.addEditorPositionListener(
                    Proxy.newProxyInstance(
                        com.intellij.ide.structureView.FileEditorPositionListener::class.java.classLoader,
                        arrayOf(com.intellij.ide.structureView.FileEditorPositionListener::class.java),
                        InvocationHandler { _, _, _ -> null },
                    ) as com.intellij.ide.structureView.FileEditorPositionListener,
                )
                model.removeEditorPositionListener(
                    Proxy.newProxyInstance(
                        com.intellij.ide.structureView.FileEditorPositionListener::class.java.classLoader,
                        arrayOf(com.intellij.ide.structureView.FileEditorPositionListener::class.java),
                        InvocationHandler { _, _, _ -> null },
                    ) as com.intellij.ide.structureView.FileEditorPositionListener,
                )
                model.addModelListener(
                    Proxy.newProxyInstance(
                        com.intellij.ide.structureView.ModelListener::class.java.classLoader,
                        arrayOf(com.intellij.ide.structureView.ModelListener::class.java),
                        InvocationHandler { _, _, _ -> null },
                    ) as com.intellij.ide.structureView.ModelListener,
                )
                model.removeModelListener(
                    Proxy.newProxyInstance(
                        com.intellij.ide.structureView.ModelListener::class.java.classLoader,
                        arrayOf(com.intellij.ide.structureView.ModelListener::class.java),
                        InvocationHandler { _, _, _ -> null },
                    ) as com.intellij.ide.structureView.ModelListener,
                )
                model.dispose()
            }
        }
    }
})

private fun psiFile(name: String, text: String): PsiFile =
    Proxy.newProxyInstance(
        PsiFile::class.java.classLoader,
        arrayOf(PsiFile::class.java),
        InvocationHandler { _, method, args ->
            when (method.name) {
                "getName" -> name
                "getText" -> text
                "getVirtualFile" -> null
                "getLanguage" -> SpecDDLanguage
                "getViewProvider" -> fileViewProvider(text)
                else -> defaultValue(method.returnType, args)
            }
        },
    ) as PsiFile

private fun fileViewProvider(text: String): FileViewProvider =
    Proxy.newProxyInstance(
        FileViewProvider::class.java.classLoader,
        arrayOf(FileViewProvider::class.java),
        InvocationHandler { _, method, args ->
            when (method.name) {
                "getBaseLanguage" -> SpecDDLanguage
                "getLanguages" -> setOf(SpecDDLanguage)
                "getContents" -> text
                "isPhysical" -> false
                else -> defaultValue(method.returnType, args)
            }
        },
    ) as FileViewProvider

private fun defaultValue(returnType: Class<*>, args: Array<Any?>?): Any? = when {
    Void.TYPE == returnType -> null
    java.lang.Boolean.TYPE == returnType -> false
    Integer.TYPE == returnType -> 0
    java.lang.Long.TYPE == returnType -> 0L
    PsiElement::class.java.isAssignableFrom(returnType) -> null
    PsiFile::class.java.isAssignableFrom(returnType) -> null
    VirtualFile::class.java.isAssignableFrom(returnType) -> null
    Language::class.java.isAssignableFrom(returnType) -> SpecDDLanguage
    else -> args?.firstOrNull()
}

private fun uninitializedSpecDDPsiFile(): SpecDDPsiFile {
    val unsafeField = Unsafe::class.java.getDeclaredField("theUnsafe")
    unsafeField.isAccessible = true
    return (unsafeField.get(null) as Unsafe).allocateInstance(SpecDDPsiFile::class.java) as SpecDDPsiFile
}
