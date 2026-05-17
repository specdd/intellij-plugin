package ai.specdd.idea.structure

import ai.specdd.idea.SpecDDFileType
import ai.specdd.idea.SpecDDPsiFile
import com.intellij.ide.structureView.*
import com.intellij.ide.util.treeView.smartTree.Filter
import com.intellij.ide.util.treeView.smartTree.Grouper
import com.intellij.ide.util.treeView.smartTree.Sorter
import com.intellij.ide.util.treeView.smartTree.TreeElement
import com.intellij.lang.PsiStructureViewFactory
import com.intellij.navigation.ItemPresentation
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.fileEditor.OpenFileDescriptor
import com.intellij.psi.PsiFile
import javax.swing.Icon

class SpecDDStructureViewFactory : PsiStructureViewFactory {
    override fun getStructureViewBuilder(psiFile: PsiFile): StructureViewBuilder? {
        if (psiFile !is SpecDDPsiFile) return null

        return object : TreeBasedStructureViewBuilder() {
            override fun createStructureViewModel(editor: Editor?): StructureViewModel =
                SpecDDStructureViewModel(SpecDDStructureTreeElement.root(psiFile))
        }
    }
}

class SpecDDStructureViewModel(
    private val root: SpecDDStructureTreeElement,
) : StructureViewModel, StructureViewModel.ElementInfoProvider {
    override fun getRoot(): StructureViewTreeElement = root

    override fun getCurrentEditorElement(): Any? = null

    override fun addEditorPositionListener(listener: FileEditorPositionListener) = Unit

    override fun removeEditorPositionListener(listener: FileEditorPositionListener) = Unit

    override fun addModelListener(modelListener: ModelListener) = Unit

    override fun removeModelListener(modelListener: ModelListener) = Unit

    override fun dispose() = Unit

    override fun shouldEnterElement(element: Any?): Boolean = false

    override fun isAlwaysShowsPlus(element: StructureViewTreeElement): Boolean = false

    override fun isAlwaysLeaf(element: StructureViewTreeElement): Boolean =
        element.value is SpecDDStructureSection

    override fun getGroupers(): Array<Grouper> = Grouper.EMPTY_ARRAY

    override fun getSorters(): Array<Sorter> = Sorter.EMPTY_ARRAY

    override fun getFilters(): Array<Filter> = Filter.EMPTY_ARRAY
}

class SpecDDStructureTreeElement private constructor(
    private val file: PsiFile,
    private val section: SpecDDStructureSection?,
    private val model: SpecDDStructureModel,
) : StructureViewTreeElement {
    override fun getValue(): Any = section ?: file

    override fun getPresentation(): ItemPresentation =
        SpecDDStructurePresentation(section?.displayName ?: file.name, SpecDDFileType().icon)

    override fun getChildren(): Array<TreeElement> {
        if (null != section) return TreeElement.EMPTY_ARRAY

        return model.sections(file.text)
            .map { childSection -> section(file, childSection, model) }
            .toTypedArray()
    }

    override fun navigate(requestFocus: Boolean) {
        descriptor()?.navigate(requestFocus)
    }

    override fun canNavigate(): Boolean = null != file.virtualFile

    override fun canNavigateToSource(): Boolean = canNavigate()

    private fun descriptor(): OpenFileDescriptor? = file.virtualFile?.let { virtualFile -> OpenFileDescriptor(file.project, virtualFile, section?.sectionStart ?: 0) }

    companion object {
        fun root(file: PsiFile, model: SpecDDStructureModel = SpecDDStructureModel()): SpecDDStructureTreeElement =
            SpecDDStructureTreeElement(file, null, model)

        fun section(
            file: PsiFile,
            section: SpecDDStructureSection,
            model: SpecDDStructureModel = SpecDDStructureModel(),
        ): SpecDDStructureTreeElement = SpecDDStructureTreeElement(file, section, model)
    }
}

private class SpecDDStructurePresentation(
    private val presentableText: String,
    private val icon: Icon?,
) : ItemPresentation {
    override fun getPresentableText(): String = presentableText

    override fun getLocationString(): String? = null

    override fun getIcon(unused: Boolean): Icon? = icon
}
