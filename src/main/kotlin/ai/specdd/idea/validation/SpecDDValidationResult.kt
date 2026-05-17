package ai.specdd.idea.validation

import com.intellij.openapi.util.TextRange

data class SpecDDValidationResult(
    val issues: List<SpecDDValidationIssue>,
) {
    val isValid: Boolean = issues.isEmpty()
}

data class SpecDDValidationIssue(
    val range: TextRange,
    val message: String,
    val suggestion: String? = null,
) {
    val displayMessage: String =
        if (null == suggestion) message else "$message Did you mean '$suggestion'?"
}
