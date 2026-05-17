package ai.specdd.idea.highlighting

import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.types.shouldBeInstanceOf

class SpecDDSyntaxHighlighterFactoryBehaviorSpec : BehaviorSpec({
    given("the SpecDD syntax highlighter factory") {
        `when`("a syntax highlighter is requested without project context") {
            then("it creates a SpecDD syntax highlighter") {
                SpecDDSyntaxHighlighterFactory()
                    .getSyntaxHighlighter(null, null)
                    .shouldBeInstanceOf<SpecDDSyntaxHighlighter>()
            }
        }
    }
})
