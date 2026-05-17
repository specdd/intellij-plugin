package ai.specdd.idea.parser

object SpecDDLanguageFacts {
    val sectionLabels: List<String> = listOf(
        "Can modify",
        "Can read",
        "Must not",
        "Depends on",
        "Done when",
        "References",
        "Structure",
        "Platform",
        "Purpose",
        "Forbids",
        "Exposes",
        "Accepts",
        "Returns",
        "Raises",
        "Handles",
        "Scenario",
        "Example",
        "Spec",
        "Owns",
        "Must",
        "Tasks",
    )
    val scenarioSteps: List<String> = listOf("Given", "When", "Then", "And", "But")
    val taskMarkerBodies: Set<Char> = setOf(' ', 'x', 'X', '-', '!', '?')
}
