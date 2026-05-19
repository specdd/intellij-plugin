package ai.specdd.idea.validation

import ai.specdd.idea.parser.SpecDDLineClassifier
import ai.specdd.idea.parser.SpecDDLineKind
import ai.specdd.idea.parser.SpecDDTaskMarker
import ai.specdd.idea.parser.SpecDDTaskStatus
import com.intellij.openapi.util.TextRange

class SpecDDStructureValidator(
    private val lineClassifier: SpecDDLineClassifier = SpecDDLineClassifier(),
) {
    fun validate(text: CharSequence): SpecDDValidationResult {
        val issues = mutableListOf<SpecDDValidationIssue>()
        var firstKnownSectionLabel: String? = null
        var firstKnownSectionRange: TextRange? = null
        var currentSectionLabel: String? = null
        var currentSectionHasBodyEntry = false
        val seenSectionLabels = mutableSetOf<String>()
        val seenScenarioValues = mutableSetOf<String>()

        forEachLine(text) { lineStart, lineEnd ->
            val classification = lineClassifier.classify(text, lineStart, lineEnd, currentSectionLabel)
            val indentationIssue = validateIndentation(text, lineStart, classification.contentStart, lineEnd, classification.kind)
            if (null != indentationIssue) issues.add(indentationIssue)

            when (classification.kind) {
                SpecDDLineKind.SECTION -> {
                    val sectionHeader = classification.sectionHeader ?: return@forEachLine
                    currentSectionLabel = sectionHeader.label
                    currentSectionHasBodyEntry = false
                    if (lineStart != sectionHeader.labelStart) {
                        issues.add(
                            SpecDDValidationIssue(
                                range = TextRange(lineStart, sectionHeader.labelStart),
                                message = "Section headers must start at column 0.",
                            ),
                        )
                    }
                    if (null == firstKnownSectionLabel) {
                        firstKnownSectionLabel = sectionHeader.label
                        firstKnownSectionRange = TextRange(sectionHeader.labelStart, sectionHeader.labelEnd)
                    }
                    val duplicateIssue = validateDuplicateSection(
                        label = sectionHeader.label,
                        labelStart = sectionHeader.labelStart,
                        labelEnd = sectionHeader.labelEnd,
                        inlineValue = text.subSequence(sectionHeader.valueStart, lineEnd).toString().trim(),
                        seenSectionLabels = seenSectionLabels,
                        seenScenarioValues = seenScenarioValues,
                    )
                    if (null != duplicateIssue) issues.add(duplicateIssue)
                    val issue = validateSectionValue(
                        text = text,
                        label = sectionHeader.label,
                        colonStart = sectionHeader.colonStart,
                        valueStart = sectionHeader.valueStart,
                        lineEnd = lineEnd,
                    )
                    if (null != issue) issues.add(issue)
                }

                SpecDDLineKind.TEXT -> {
                    val issue = validateNonSectionLine(
                        text = text,
                        lineStart = lineStart,
                        contentStart = classification.contentStart,
                        lineEnd = lineEnd,
                        currentSectionLabel = currentSectionLabel,
                        lineKind = classification.kind,
                        hasPreviousBodyEntry = currentSectionHasBodyEntry,
                    )
                    if (null != issue) issues.add(issue)
                    if (null == issue && classification.kind in BODY_ENTRY_LINE_KINDS) {
                        currentSectionHasBodyEntry = true
                    }
                }

                SpecDDLineKind.TASK -> {
                    val taskMarker = classification.taskMarker ?: return@forEachLine
                    if (SpecDDTaskStatus.INVALID == taskMarker.status) {
                        issues.add(
                            SpecDDValidationIssue(
                                range = TextRange(taskMarker.markerStart, taskMarker.markerEnd),
                                message = "Invalid SpecDD task state '${
                                    text.subSequence(
                                        taskMarker.markerStart,
                                        taskMarker.markerEnd,
                                    )
                                }'.",
                            ),
                        )
                    }
                    val taskTextIssue = validateTaskText(text, lineEnd, taskMarker)
                    if (null != taskTextIssue) issues.add(taskTextIssue)
                    val issue = validateNonSectionLine(
                        text = text,
                        lineStart = lineStart,
                        contentStart = classification.contentStart,
                        lineEnd = lineEnd,
                        currentSectionLabel = currentSectionLabel,
                        lineKind = classification.kind,
                        hasPreviousBodyEntry = currentSectionHasBodyEntry,
                    )
                    if (null != issue) issues.add(issue)
                    if (null == issue && null == taskTextIssue && classification.kind in BODY_ENTRY_LINE_KINDS) {
                        currentSectionHasBodyEntry = true
                    }
                }

                SpecDDLineKind.CONTINUATION,
                SpecDDLineKind.SCENARIO_STEP,
                SpecDDLineKind.KEY_VALUE -> {
                    val issue = validateNonSectionLine(
                        text = text,
                        lineStart = lineStart,
                        contentStart = classification.contentStart,
                        lineEnd = lineEnd,
                        currentSectionLabel = currentSectionLabel,
                        lineKind = classification.kind,
                        hasPreviousBodyEntry = currentSectionHasBodyEntry,
                    )
                    if (null != issue) issues.add(issue)
                    if (null == issue && classification.kind in BODY_ENTRY_LINE_KINDS) {
                        currentSectionHasBodyEntry = true
                    }
                }

                SpecDDLineKind.BLANK,
                SpecDDLineKind.COMMENT -> Unit
            }
        }

        if (null != firstKnownSectionLabel && "Spec" != firstKnownSectionLabel) {
            issues.add(
                SpecDDValidationIssue(
                    range = firstKnownSectionRange ?: TextRange.EMPTY_RANGE,
                    message = "SpecDD files should start with the Spec section.",
                    suggestion = "Spec",
                ),
            )
        }

        return SpecDDValidationResult(issues.sortedBy { issue -> issue.range.startOffset })
    }

    private fun validateIndentation(
        text: CharSequence,
        lineStart: Int,
        contentStart: Int,
        lineEnd: Int,
        lineKind: SpecDDLineKind,
    ): SpecDDValidationIssue? {
        if (contentStart >= lineEnd) return null
        if (SpecDDLineKind.COMMENT == lineKind) return null
        if (lineStart == contentStart) return null

        val indentation = text.subSequence(lineStart, contentStart)
        if (indentation.any { character -> ' ' != character }) return invalidIndentation(lineStart, contentStart)
        if (0 != indentation.length % INDENT_SIZE) return invalidIndentation(lineStart, contentStart)

        return null
    }

    private fun invalidIndentation(lineStart: Int, contentStart: Int): SpecDDValidationIssue =
        SpecDDValidationIssue(
            range = TextRange(lineStart, contentStart),
            message = "Indentation must use spaces in multiples of 2.",
        )

    private fun bodyEntryIndentationRange(lineStart: Int, contentStart: Int, lineEnd: Int): TextRange {
        if (lineStart < contentStart) return TextRange(lineStart, contentStart)
        return TextRange(contentStart, lineEnd)
    }

    private fun validateDuplicateSection(
        label: String,
        labelStart: Int,
        labelEnd: Int,
        inlineValue: String,
        seenSectionLabels: MutableSet<String>,
        seenScenarioValues: MutableSet<String>,
    ): SpecDDValidationIssue? {
        if (!SpecDDKnownSections.isKnown(label)) return null
        if ("Scenario" == label) {
            if (seenScenarioValues.add(inlineValue)) return null

            return SpecDDValidationIssue(
                range = TextRange(labelStart, labelEnd),
                message = "Scenario '$inlineValue' must not be repeated.",
            )
        }
        if (label in REPEATABLE_SECTIONS) return null
        if (seenSectionLabels.add(label)) return null

        return SpecDDValidationIssue(
            range = TextRange(labelStart, labelEnd),
            message = "Section '$label' must not be repeated.",
        )
    }

    private fun validateNonSectionLine(
        text: CharSequence,
        lineStart: Int,
        contentStart: Int,
        lineEnd: Int,
        currentSectionLabel: String?,
        lineKind: SpecDDLineKind,
        hasPreviousBodyEntry: Boolean,
    ): SpecDDValidationIssue? {
        if (contentStart >= lineEnd) return null

        val sectionSyntaxIssue = validateSectionSyntaxCandidate(text, lineStart, contentStart, lineEnd)
        if (null != sectionSyntaxIssue) return sectionSyntaxIssue

        return validateSectionBodyLine(
            sectionLabel = currentSectionLabel,
            lineKind = lineKind,
            hasPreviousBodyEntry = hasPreviousBodyEntry,
            lineStart = lineStart,
            contentStart = contentStart,
            lineEnd = lineEnd,
        )
    }

    private fun validateSectionSyntaxCandidate(
        text: CharSequence,
        lineStart: Int,
        contentStart: Int,
        lineEnd: Int,
    ): SpecDDValidationIssue? {
        val colonOffset = indexOf(text, ':', contentStart, lineEnd)
        if (NO_OFFSET == colonOffset) {
            return validateMissingColon(text, contentStart, lineEnd)
        }

        val label = text.subSequence(contentStart, colonOffset).toString().trimEnd()
        if (!isSectionCandidate(label)) return null

        if (SpecDDKnownSections.isKnown(label)) {
            return SpecDDValidationIssue(
                range = TextRange(contentStart, contentStart + label.length),
                message = "Section '$label' is missing ':'.",
            )
        }

        if (lineStart != contentStart) return null

        return SpecDDValidationIssue(
            range = TextRange(contentStart, contentStart + label.length),
            message = "Unknown SpecDD section '$label'.",
            suggestion = SpecDDKnownSections.closestLabel(label),
        )
    }

    private fun validateSectionBodyLine(
        sectionLabel: String?,
        lineKind: SpecDDLineKind,
        hasPreviousBodyEntry: Boolean,
        lineStart: Int,
        contentStart: Int,
        lineEnd: Int,
    ): SpecDDValidationIssue? {
        if (contentStart >= lineEnd) return null
        if (null == sectionLabel) {
            return SpecDDValidationIssue(
                range = TextRange(contentStart, lineEnd),
                message = "Invalid SpecDD syntax.",
            )
        }

        if (sectionLabel in BODYLESS_SECTIONS) {
            return SpecDDValidationIssue(
                range = TextRange(contentStart, lineEnd),
                message = "Section '$sectionLabel' does not support follow-up lines.",
            )
        }
        if (SpecDDLineKind.CONTINUATION == lineKind && !hasPreviousBodyEntry) {
            return SpecDDValidationIssue(
                range = TextRange(contentStart, lineEnd),
                message = "Continuation line must follow a body entry in the same section.",
            )
        }
        val indentationWidth = contentStart - lineStart
        if (
            lineKind in BODY_ENTRY_LINE_KINDS &&
            0 == indentationWidth % INDENT_SIZE &&
            BODY_ENTRY_INDENT_SIZE != indentationWidth
        ) {
            return SpecDDValidationIssue(
                range = bodyEntryIndentationRange(lineStart, contentStart, lineEnd),
                message = "Body entries must be indented by exactly 2 spaces.",
            )
        }

        val allowedLineKinds = BODY_LINE_KINDS_BY_SECTION[sectionLabel] ?: DEFAULT_BODY_LINE_KINDS
        if (lineKind in allowedLineKinds) return null

        return SpecDDValidationIssue(
            range = TextRange(contentStart, lineEnd),
            message = "Invalid SpecDD syntax.",
        )
    }

    private fun validateSectionValue(
        text: CharSequence,
        label: String,
        colonStart: Int,
        valueStart: Int,
        lineEnd: Int,
    ): SpecDDValidationIssue? {
        if (label in INLINE_VALUE_SECTIONS) {
            if (valueStart < lineEnd && ' ' != text[valueStart]) {
                return SpecDDValidationIssue(
                    range = TextRange(valueStart, lineEnd),
                    message = "Inline value for section '$label' must be separated from ':' by a space.",
                )
            }

            if (label in REQUIRED_INLINE_VALUE_SECTIONS && firstNonWhitespace(text, valueStart, lineEnd) >= lineEnd) {
                return SpecDDValidationIssue(
                    range = if (valueStart < lineEnd) {
                        TextRange(valueStart, lineEnd)
                    } else {
                        TextRange(colonStart, valueStart)
                    },
                    message = "Section '$label' requires an inline value.",
                )
            }

            return null
        }

        val contentStart = firstNonWhitespace(text, valueStart, lineEnd)
        if (contentStart >= lineEnd) return null

        return SpecDDValidationIssue(
            range = TextRange(contentStart, lineEnd),
            message = "Section '$label' does not support inline text after ':'.",
        )
    }

    private fun validateTaskText(
        text: CharSequence,
        lineEnd: Int,
        taskMarker: SpecDDTaskMarker,
    ): SpecDDValidationIssue? {
        val textStart = firstNonWhitespace(text, taskMarker.taskId?.end ?: taskMarker.markerEnd, lineEnd)
        if (textStart < lineEnd) return null

        val rangeStart = taskMarker.taskId?.start ?: taskMarker.markerStart
        return SpecDDValidationIssue(
            range = TextRange(rangeStart, lineEnd),
            message = "Task entries must include task text.",
        )
    }

    private fun validateMissingColon(text: CharSequence, contentStart: Int, lineEnd: Int): SpecDDValidationIssue? {
        val label = text.subSequence(contentStart, lineEnd).toString().trimEnd()
        if (!SpecDDKnownSections.isKnown(label)) return null

        return SpecDDValidationIssue(
            range = TextRange(contentStart, contentStart + label.length),
            message = "Section '$label' is missing ':'.",
        )
    }

    private fun forEachLine(text: CharSequence, block: (Int, Int) -> Unit) {
        var lineStart = 0
        while (lineStart < text.length) {
            val lineEnd = findLineEnd(text, lineStart)
            block(lineStart, lineEnd)
            lineStart = nextLineStart(text, lineEnd)
        }
    }

    private fun findLineEnd(text: CharSequence, lineStart: Int): Int {
        var offset = lineStart
        while (offset < text.length && '\n' != text[offset] && '\r' != text[offset]) {
            offset += 1
        }
        return offset
    }

    private fun nextLineStart(text: CharSequence, lineEnd: Int): Int {
        if (lineEnd >= text.length) return text.length
        if ('\r' != text[lineEnd]) return lineEnd + 1
        if (lineEnd + 1 < text.length && '\n' == text[lineEnd + 1]) return lineEnd + 2
        return lineEnd + 1
    }

    private fun indexOf(text: CharSequence, target: Char, start: Int, end: Int): Int {
        var offset = start
        while (offset < end) {
            if (target == text[offset]) return offset
            offset += 1
        }
        return NO_OFFSET
    }

    private fun firstNonWhitespace(text: CharSequence, start: Int, end: Int): Int {
        var offset = start
        while (offset < end && text[offset].isWhitespace()) {
            offset += 1
        }
        return offset
    }

    private fun isSectionCandidate(label: String): Boolean {
        if (label.isBlank()) return false

        return label.all { character -> character.isLetter() || character.isWhitespace() }
    }

    private companion object {
        const val INDENT_SIZE = 2
        const val BODY_ENTRY_INDENT_SIZE = 2
        const val NO_OFFSET = -1
    }
}

private val INLINE_VALUE_SECTIONS = setOf("Spec", "Platform", "Scenario", "Example")
private val REQUIRED_INLINE_VALUE_SECTIONS = setOf("Spec", "Platform", "Scenario")
private val BODYLESS_SECTIONS = setOf("Spec", "Platform")
private val REPEATABLE_SECTIONS = setOf("Scenario", "Example")
private val DEFAULT_BODY_LINE_KINDS = setOf(
    SpecDDLineKind.TEXT,
    SpecDDLineKind.SCENARIO_STEP,
    SpecDDLineKind.KEY_VALUE,
    SpecDDLineKind.CONTINUATION,
)
private val BODY_ENTRY_LINE_KINDS = setOf(
    SpecDDLineKind.TEXT,
    SpecDDLineKind.TASK,
    SpecDDLineKind.SCENARIO_STEP,
    SpecDDLineKind.KEY_VALUE,
)
private val BODY_LINE_KINDS_BY_SECTION = mapOf(
    "Tasks" to setOf(SpecDDLineKind.TASK, SpecDDLineKind.CONTINUATION),
)
