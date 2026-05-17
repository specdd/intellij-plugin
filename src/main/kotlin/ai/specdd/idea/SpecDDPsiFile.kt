package ai.specdd.idea

import ai.specdd.idea.references.SpecDDPathReferenceProvider
import com.intellij.extapi.psi.PsiFileBase
import com.intellij.psi.FileViewProvider
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiReference

class SpecDDPsiFile(viewProvider: FileViewProvider) : PsiFileBase(viewProvider, SpecDDLanguage) {
    override fun getFileType(): SpecDDFileType = SpecDDFileType()

    override fun findReferenceAt(offset: Int): PsiReference? =
        specDDPathReferenceAt(this, offset)

    override fun toString(): String = "SpecDD file"
}

internal var specDDPathReferenceAt: (PsiElement, Int) -> PsiReference? = { element, offset ->
    SpecDDPathReferenceProvider().getReferenceAt(element, offset)
}
