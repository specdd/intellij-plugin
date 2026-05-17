package ai.specdd.idea

import com.intellij.lang.Language
import com.intellij.psi.codeStyle.CodeStyleSettingsCustomizable
import com.intellij.psi.codeStyle.CommonCodeStyleSettings
import com.intellij.psi.codeStyle.LanguageCodeStyleSettingsProvider

class SpecDDCodeStyleSettingsProvider : LanguageCodeStyleSettingsProvider() {
    override fun getLanguage(): Language = SpecDDLanguage

    override fun getCodeSample(settingsType: SettingsType): String = CODE_SAMPLE

    override fun customizeSettings(consumer: CodeStyleSettingsCustomizable, settingsType: SettingsType) {
        if (SettingsType.INDENT_SETTINGS != settingsType) return

        consumer.showStandardOptions("INDENT_SIZE", "TAB_SIZE", "USE_TAB_CHARACTER")
    }

    override fun customizeDefaults(
        commonSettings: CommonCodeStyleSettings,
        indentOptions: CommonCodeStyleSettings.IndentOptions,
    ) {
        indentOptions.INDENT_SIZE = SPECDD_INDENT_SIZE
        indentOptions.CONTINUATION_INDENT_SIZE = SPECDD_INDENT_SIZE
        indentOptions.TAB_SIZE = SPECDD_INDENT_SIZE
        indentOptions.USE_TAB_CHARACTER = false
    }

    private companion object {
        const val SPECDD_INDENT_SIZE = 2
        const val CODE_SAMPLE = "Spec: Example\nPurpose:\n  Describe the behavior.\n"
    }
}
