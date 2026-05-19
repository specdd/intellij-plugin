package ai.specdd.idea.completion

import ai.specdd.idea.SpecDDFileType
import ai.specdd.idea.references.pathResolutionContext
import com.intellij.codeInsight.completion.CompletionContributor
import com.intellij.codeInsight.completion.CompletionParameters
import com.intellij.codeInsight.completion.CompletionResultSet
import com.intellij.codeInsight.completion.CompletionType

class SpecDDCompletionContributor : CompletionContributor() {
    override fun fillCompletionVariants(parameters: CompletionParameters, result: CompletionResultSet) {
        if (CompletionType.BASIC != parameters.completionType) return
        if (parameters.originalFile.fileType !is SpecDDFileType) return

        val sectionCompletion = SpecDDSectionCompletion.complete(
            text = parameters.editor.document.charsSequence,
            offset = parameters.offset,
        )
        if (null != sectionCompletion) {
            result
                .withPrefixMatcher(sectionCompletion.prefix)
                .caseInsensitive()
                .addAllElements(sectionCompletion.variants.map { variant -> variant.toLookupElement() })
            return
        }

        val pathContext = parameters.originalFile.pathResolutionContext()
        val inlineCompletion = SpecDDInlineCompletion.complete(
            text = parameters.editor.document.charsSequence,
            offset = parameters.offset,
            projectRoot = pathContext?.projectRoot,
            specDirectory = pathContext?.specDirectory,
            isInProject = pathContext?.isInProject ?: SpecDDInlineCompletion::defaultProjectFilter,
        ) ?: return

        result
            .withPrefixMatcher(inlineCompletion.prefix)
            .caseInsensitive()
            .addAllElements(inlineCompletion.variants.map { variant -> variant.toLookupElement() })
    }
}
