package ai.specdd.idea

import com.intellij.openapi.fileTypes.LanguageFileType
import com.intellij.openapi.util.IconLoader
import javax.swing.Icon

class SpecDDFileType : LanguageFileType(SpecDDLanguage) {
    override fun getName(): String = "SpecDD"

    override fun getDescription(): String = "SpecDD specification file"

    override fun getDefaultExtension(): String = "sdd"

    override fun getIcon(): Icon = IconLoader.getIcon("/META-INF/logo.svg", SpecDDFileType::class.java)
}
