package ai.specdd.idea.references

import com.intellij.patterns.ElementPattern
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiReferenceProvider
import com.intellij.psi.PsiReferenceRegistrar
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.types.shouldBeInstanceOf

class SpecDDPathReferenceContributorBehaviorSpec : BehaviorSpec({
    given("a SpecDD path reference contributor") {
        `when`("reference providers are registered") {
            then("it registers the path reference provider") {
                val registrar = RecordingReferenceRegistrar()

                SpecDDPathReferenceContributor().registerReferenceProviders(registrar)

                registrar.provider.shouldBeInstanceOf<SpecDDPathReferenceProvider>()
            }
        }
    }
})

private class RecordingReferenceRegistrar : PsiReferenceRegistrar() {
    var provider: PsiReferenceProvider? = null

    override fun <T : PsiElement> registerReferenceProvider(
        pattern: ElementPattern<T>,
        provider: PsiReferenceProvider,
        priority: Double,
    ) {
        this.provider = provider
    }
}
