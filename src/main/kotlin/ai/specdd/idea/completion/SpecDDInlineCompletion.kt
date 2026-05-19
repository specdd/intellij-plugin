package ai.specdd.idea.completion

import ai.specdd.idea.references.isInRoot
import ai.specdd.idea.references.relativePath
import com.intellij.codeInsight.lookup.LookupElement
import com.intellij.codeInsight.lookup.LookupElementBuilder
import com.intellij.openapi.vfs.VirtualFile

object SpecDDInlineCompletion {
    @Suppress("UNUSED_PARAMETER")
    fun defaultProjectFilter(file: VirtualFile): Boolean = true

    fun complete(
        text: CharSequence,
        offset: Int,
        projectRoot: VirtualFile?,
        specDirectory: VirtualFile? = projectRoot,
        isInProject: (VirtualFile) -> Boolean = { true },
    ): SpecDDInlineCompletionResult? {
        val prefix = prefixAt(text, offset) ?: return null
        val variants = linkedSetOf<SpecDDInlineCompletionVariant>()

        if (null != projectRoot) {
            variants.addAll(pathVariants(projectRoot, specDirectory ?: projectRoot, prefix, isInProject))
        }
        variants.addAll(symbolVariants(text, prefix))

        if (variants.isEmpty()) return null
        return SpecDDInlineCompletionResult(prefix, variants.toList())
    }

    internal fun prefixAt(text: CharSequence, offset: Int): String? {
        if (offset < 0 || offset > text.length) return null

        var start = offset
        while (start > 0 && isInlinePrefixCharacter(text[start - 1])) {
            start -= 1
        }

        if (start == offset) return null
        return text.subSequence(start, offset).toString()
    }

    private fun pathVariants(
        projectRoot: VirtualFile,
        specDirectory: VirtualFile,
        prefix: String,
        isInProject: (VirtualFile) -> Boolean,
    ): List<SpecDDInlineCompletionVariant> {
        if (!projectRoot.isDirectory) return emptyList()
        if (!isInRoot(specDirectory, projectRoot)) return emptyList()
        if (!hasExplicitPathPrefix(prefix)) return emptyList()

        val variants = mutableListOf<String>()
        val stack = ArrayDeque<VirtualFile>()
        stack.add(projectRoot)

        while (stack.isNotEmpty() && variants.size < MAX_PATH_VARIANTS) {
            val file = stack.removeLast()
            if (file != projectRoot && file.isDirectory && file.name in SKIPPED_DIRECTORIES) continue
            if (file != projectRoot && !isInProject(file)) continue

            if (file != projectRoot) {
                val lookupString = pathLookupString(projectRoot, specDirectory, file, prefix)
                if (lookupString.startsWith(prefix, ignoreCase = true)) {
                    variants.add(lookupString)
                }
            }

            if (file.isDirectory) {
                file.children.reversedArray().forEach { child -> stack.add(child) }
            }
        }

        return variants
            .sorted()
            .map { lookupString -> SpecDDInlineCompletionVariant(lookupString) }
    }

    private fun pathLookupString(
        projectRoot: VirtualFile,
        specDirectory: VirtualFile,
        path: VirtualFile,
        prefix: String,
    ): String {
        if (prefix.startsWith("../")) {
            return relativePath(specDirectory, path)
        }
        if (prefix.startsWith("/")) {
            return "/${relativePath(projectRoot, path)}"
        }
        return "./${relativePath(specDirectory, path)}"
    }

    private fun symbolVariants(text: CharSequence, prefix: String): List<SpecDDInlineCompletionVariant> =
        SYMBOL_PATTERN
            .findAll(text)
            .map { match -> match.value }
            .filter { symbol -> symbol.startsWith(prefix, ignoreCase = true) }
            .filter { symbol -> symbol != prefix }
            .map { symbol -> SpecDDInlineCompletionVariant(symbol) }
            .toList()
}

data class SpecDDInlineCompletionResult(
    val prefix: String,
    val variants: List<SpecDDInlineCompletionVariant>,
)

data class SpecDDInlineCompletionVariant(
    val lookupString: String,
) {
    fun toLookupElement(): LookupElement =
        LookupElementBuilder.create(lookupString)
}

private val INLINE_PREFIX_CHARS = setOf('/', '.', '~', '*', '?', '[', ']', '{', '}', '_', '-')
private val SYMBOL_PATTERN = Regex("""\b[A-Z][A-Za-z0-9_]*(?:\.[A-Za-z_$][A-Za-z0-9_$]*)+(?:\([^)]*\))?""")
private const val MAX_PATH_VARIANTS = 200
private val SKIPPED_DIRECTORIES = setOf(".git", ".gradle", ".idea", "build", "node_modules", "out")

private fun isInlinePrefixCharacter(character: Char): Boolean =
    character.isLetterOrDigit() || character in INLINE_PREFIX_CHARS

private fun hasExplicitPathPrefix(text: String): Boolean =
    text.startsWith("./") || text.startsWith("../") || text.startsWith("/")
