package ai.specdd.idea.parser

class SpecDDLineClassifier {
    fun classify(buffer: CharSequence, lineStart: Int, lineEnd: Int): SpecDDLineClassification {
        val contentStart = firstNonWhitespace(buffer, lineStart, lineEnd)
        if (contentStart >= lineEnd) {
            return SpecDDLineClassification.blank(lineStart, lineEnd)
        }

        if ('#' == buffer[contentStart]) {
            return SpecDDLineClassification.comment(lineStart, lineEnd, contentStart)
        }

        val sectionHeader = findSectionHeader(buffer, contentStart, lineEnd)
        if (null != sectionHeader) {
            return SpecDDLineClassification.section(lineStart, lineEnd, contentStart, sectionHeader)
        }

        val taskMarker = findTaskMarker(buffer, contentStart, lineEnd)
        if (null != taskMarker) {
            return SpecDDLineClassification.task(lineStart, lineEnd, contentStart, taskMarker)
        }

        val scenarioStep = findScenarioStep(buffer, contentStart, lineEnd)
        if (null != scenarioStep) {
            return SpecDDLineClassification.scenarioStep(lineStart, lineEnd, contentStart, scenarioStep)
        }

        val keyValue = findKeyValue(buffer, contentStart, lineEnd)
        if (null != keyValue) {
            return SpecDDLineClassification.keyValue(lineStart, lineEnd, contentStart, keyValue)
        }

        return SpecDDLineClassification.text(lineStart, lineEnd, contentStart)
    }

    private fun findSectionHeader(buffer: CharSequence, contentStart: Int, lineEnd: Int): SpecDDSectionHeader? {
        for (label in SpecDDLanguageFacts.sectionLabels) {
            val colonOffset = contentStart + label.length
            if (lineEnd <= colonOffset || ':' != buffer[colonOffset]) continue
            if (!matches(buffer, contentStart, label)) continue

            return SpecDDSectionHeader(
                label = label,
                labelStart = contentStart,
                labelEnd = colonOffset,
                colonStart = colonOffset,
                valueStart = colonOffset + 1,
                valueEnd = lineEnd,
            )
        }

        return null
    }

    private fun findTaskMarker(buffer: CharSequence, contentStart: Int, lineEnd: Int): SpecDDTaskMarker? {
        if (lineEnd <= contentStart || '[' != buffer[contentStart]) return null

        val markerEnd = findTaskMarkerEnd(buffer, contentStart, lineEnd) ?: return null
        val markerBody = buffer[contentStart + 1]
        val status = if (TASK_MARKER_LENGTH == markerEnd - contentStart) {
            if (markerBody !in SpecDDLanguageFacts.taskMarkerBodies) SpecDDTaskStatus.INVALID else
                SpecDDTaskStatus.fromMarkerBody(markerBody)
        } else {
            SpecDDTaskStatus.INVALID
        }

        val taskIdStart = firstNonWhitespace(buffer, markerEnd, lineEnd)
        if (taskIdStart >= lineEnd || '#' != buffer[taskIdStart]) {
            return SpecDDTaskMarker(contentStart, markerEnd, status, null)
        }

        val taskIdEnd = consumeDigits(buffer, taskIdStart + 1, lineEnd)
        if (taskIdStart + 1 >= taskIdEnd) {
            return SpecDDTaskMarker(contentStart, markerEnd, status, null)
        }

        return SpecDDTaskMarker(contentStart, markerEnd, status, SpecDDTaskId(taskIdStart, taskIdEnd))
    }

    private fun findTaskMarkerEnd(buffer: CharSequence, contentStart: Int, lineEnd: Int): Int? {
        val supportedMarkerEnd = contentStart + TASK_MARKER_LENGTH
        if (supportedMarkerEnd <= lineEnd && ']' == buffer[supportedMarkerEnd - 1]) {
            return supportedMarkerEnd
        }

        var offset = contentStart + 1
        while (offset < lineEnd && !buffer[offset].isWhitespace()) {
            if (']' == buffer[offset]) return offset + 1
            offset += 1
        }

        return null
    }

    private fun findScenarioStep(buffer: CharSequence, contentStart: Int, lineEnd: Int): SpecDDScenarioStep? {
        for (step in SpecDDLanguageFacts.scenarioSteps) {
            val stepEnd = contentStart + step.length
            if (lineEnd < stepEnd || !matches(buffer, contentStart, step)) continue
            if (stepEnd < lineEnd && !buffer[stepEnd].isWhitespace()) continue

            return SpecDDScenarioStep(step, contentStart, stepEnd)
        }

        return null
    }

    private fun findKeyValue(buffer: CharSequence, contentStart: Int, lineEnd: Int): SpecDDKeyValue? {
        val colonOffset = findKeyColon(buffer, contentStart, lineEnd)
        if (null == colonOffset) return null

        return SpecDDKeyValue(
            keyStart = contentStart,
            keyEnd = colonOffset,
            colonStart = colonOffset,
            valueStart = colonOffset + 1,
            valueEnd = lineEnd,
        )
    }

    private fun findKeyColon(buffer: CharSequence, contentStart: Int, lineEnd: Int): Int? {
        var offset = contentStart
        while (offset < lineEnd) {
            val char = buffer[offset]
            if (':' == char) {
                if (contentStart == offset) return null
                if (buffer[offset - 1].isWhitespace()) return null
                if (offset + 1 >= lineEnd || ' ' != buffer[offset + 1]) return null
                return offset
            }
            offset += 1
        }

        return null
    }

    private fun firstNonWhitespace(buffer: CharSequence, start: Int, end: Int): Int {
        var offset = start
        while (offset < end && buffer[offset].isWhitespace()) {
            offset += 1
        }
        return offset
    }

    private fun consumeDigits(buffer: CharSequence, start: Int, end: Int): Int {
        var offset = start
        while (offset < end && buffer[offset].isDigit()) {
            offset += 1
        }
        return offset
    }

    private fun matches(buffer: CharSequence, offset: Int, text: String): Boolean {
        for (index in text.indices) {
            if (text[index] != buffer[offset + index]) return false
        }
        return true
    }

    private companion object {
        const val TASK_MARKER_LENGTH = 3
    }
}

data class SpecDDLineClassification(
    val kind: SpecDDLineKind,
    val lineStart: Int,
    val lineEnd: Int,
    val contentStart: Int,
    val sectionHeader: SpecDDSectionHeader? = null,
    val taskMarker: SpecDDTaskMarker? = null,
    val scenarioStep: SpecDDScenarioStep? = null,
    val keyValue: SpecDDKeyValue? = null,
) {
    companion object {
        fun blank(lineStart: Int, lineEnd: Int): SpecDDLineClassification =
            SpecDDLineClassification(SpecDDLineKind.BLANK, lineStart, lineEnd, lineEnd)

        fun comment(lineStart: Int, lineEnd: Int, contentStart: Int): SpecDDLineClassification =
            SpecDDLineClassification(SpecDDLineKind.COMMENT, lineStart, lineEnd, contentStart)

        fun section(
            lineStart: Int,
            lineEnd: Int,
            contentStart: Int,
            sectionHeader: SpecDDSectionHeader,
        ): SpecDDLineClassification =
            SpecDDLineClassification(SpecDDLineKind.SECTION, lineStart, lineEnd, contentStart, sectionHeader)

        fun task(
            lineStart: Int,
            lineEnd: Int,
            contentStart: Int,
            taskMarker: SpecDDTaskMarker,
        ): SpecDDLineClassification =
            SpecDDLineClassification(
                kind = SpecDDLineKind.TASK,
                lineStart = lineStart,
                lineEnd = lineEnd,
                contentStart = contentStart,
                taskMarker = taskMarker,
            )

        fun scenarioStep(
            lineStart: Int,
            lineEnd: Int,
            contentStart: Int,
            scenarioStep: SpecDDScenarioStep,
        ): SpecDDLineClassification =
            SpecDDLineClassification(
                kind = SpecDDLineKind.SCENARIO_STEP,
                lineStart = lineStart,
                lineEnd = lineEnd,
                contentStart = contentStart,
                scenarioStep = scenarioStep,
            )

        fun keyValue(
            lineStart: Int,
            lineEnd: Int,
            contentStart: Int,
            keyValue: SpecDDKeyValue,
        ): SpecDDLineClassification =
            SpecDDLineClassification(
                kind = SpecDDLineKind.KEY_VALUE,
                lineStart = lineStart,
                lineEnd = lineEnd,
                contentStart = contentStart,
                keyValue = keyValue,
            )

        fun text(lineStart: Int, lineEnd: Int, contentStart: Int): SpecDDLineClassification =
            SpecDDLineClassification(SpecDDLineKind.TEXT, lineStart, lineEnd, contentStart)
    }
}

enum class SpecDDLineKind {
    BLANK,
    COMMENT,
    SECTION,
    TASK,
    SCENARIO_STEP,
    KEY_VALUE,
    TEXT,
}

data class SpecDDSectionHeader(
    val label: String,
    val labelStart: Int,
    val labelEnd: Int,
    val colonStart: Int,
    val valueStart: Int,
    val valueEnd: Int,
)

data class SpecDDTaskMarker(
    val markerStart: Int,
    val markerEnd: Int,
    val status: SpecDDTaskStatus,
    val taskId: SpecDDTaskId?,
)

enum class SpecDDTaskStatus {
    DONE,
    OPEN,
    BLOCKED,
    QUESTION,
    SKIPPED,
    INVALID,
    ;

    companion object {
        fun fromMarkerBody(markerBody: Char): SpecDDTaskStatus = when (markerBody) {
            'x', 'X' -> DONE
            ' ' -> OPEN
            '!' -> BLOCKED
            '?' -> QUESTION
            '-' -> SKIPPED
            else -> error("Unsupported SpecDD task marker body: $markerBody")
        }
    }
}

data class SpecDDTaskId(
    val start: Int,
    val end: Int,
)

data class SpecDDScenarioStep(
    val keyword: String,
    val start: Int,
    val end: Int,
)

data class SpecDDKeyValue(
    val keyStart: Int,
    val keyEnd: Int,
    val colonStart: Int,
    val valueStart: Int,
    val valueEnd: Int,
)
