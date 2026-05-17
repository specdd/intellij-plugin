package ai.specdd.idea.validation

import ai.specdd.idea.parser.SpecDDLanguageFacts

object SpecDDKnownSections {
    val labels: Set<String> = SpecDDLanguageFacts.sectionLabels.toSet()

    fun isKnown(label: String): Boolean = label in labels

    fun closestLabel(label: String): String? {
        var closest: String? = null
        var closestDistance = Int.MAX_VALUE
        for (knownLabel in labels) {
            val distance = levenshteinDistance(label.lowercase(), knownLabel.lowercase())
            if (distance < closestDistance) {
                closest = knownLabel
                closestDistance = distance
            }
        }

        if (MAX_TYPO_DISTANCE < closestDistance) return null
        return closest
    }

    private fun levenshteinDistance(left: String, right: String): Int {
        if (left == right) return 0
        if (left.isEmpty()) return right.length
        if (right.isEmpty()) return left.length

        var previous = IntArray(right.length + 1) { index -> index }
        var current = IntArray(right.length + 1)

        for (leftIndex in left.indices) {
            current[0] = leftIndex + 1
            for (rightIndex in right.indices) {
                val substitutionCost = if (left[leftIndex] == right[rightIndex]) 0 else 1
                current[rightIndex + 1] = minOf(
                    current[rightIndex] + 1,
                    previous[rightIndex + 1] + 1,
                    previous[rightIndex] + substitutionCost,
                )
            }

            val swap = previous
            previous = current
            current = swap
        }

        return previous[right.length]
    }

    private const val MAX_TYPO_DISTANCE = 2
}
