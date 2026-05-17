package ai.specdd.idea.completion

import com.intellij.codeInsight.lookup.LookupElement
import com.intellij.codeInsight.lookup.LookupElementBuilder
import java.io.IOException
import java.nio.file.FileVisitResult
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.SimpleFileVisitor
import java.nio.file.attribute.BasicFileAttributes

object SpecDDInlineCompletion {
    fun complete(
        text: CharSequence,
        offset: Int,
        projectRoot: Path?,
        specDirectory: Path? = projectRoot,
    ): SpecDDInlineCompletionResult? {
        val prefix = prefixAt(text, offset) ?: return null
        val variants = linkedSetOf<SpecDDInlineCompletionVariant>()

        if (null != projectRoot) {
            variants.addAll(pathVariants(projectRoot, specDirectory ?: projectRoot, prefix))
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
        projectRoot: Path,
        specDirectory: Path,
        prefix: String,
    ): List<SpecDDInlineCompletionVariant> {
        val root = projectRoot.toAbsolutePath().normalize()
        val base = specDirectory.toAbsolutePath().normalize()
        if (!Files.isDirectory(root)) return emptyList()
        if (!base.startsWith(root)) return emptyList()

        val variants = mutableListOf<String>()
        Files.walkFileTree(
            root,
            object : SimpleFileVisitor<Path>() {
                override fun preVisitDirectory(dir: Path, attrs: BasicFileAttributes): FileVisitResult {
                    if (dir != root && dir.fileName?.toString() in SKIPPED_DIRECTORIES) {
                        return FileVisitResult.SKIP_SUBTREE
                    }
                    addIfMatching(dir)
                    return if (MAX_PATH_VARIANTS <= variants.size) {
                        FileVisitResult.TERMINATE
                    } else {
                        FileVisitResult.CONTINUE
                    }
                }

                override fun visitFile(file: Path, attrs: BasicFileAttributes): FileVisitResult {
                    addIfMatching(file)
                    return if (MAX_PATH_VARIANTS <= variants.size) {
                        FileVisitResult.TERMINATE
                    } else {
                        FileVisitResult.CONTINUE
                    }
                }

                override fun visitFileFailed(file: Path, exc: IOException): FileVisitResult =
                    FileVisitResult.CONTINUE

                private fun addIfMatching(path: Path) {
                    if (path == root) return
                    val lookupString = pathLookupString(root, base, path.toAbsolutePath().normalize(), prefix)
                    if (lookupString.startsWith(prefix, ignoreCase = true)) {
                        variants.add(lookupString)
                    }
                }
            }
        )

        return variants
            .sorted()
            .map { lookupString -> SpecDDInlineCompletionVariant(lookupString) }
    }

    private fun pathLookupString(projectRoot: Path, specDirectory: Path, path: Path, prefix: String): String {
        if (prefix.startsWith("./")) {
            return "./${specDirectory.relativize(path).joinToString("/")}"
        }
        if (prefix.startsWith("../")) {
            return specDirectory.relativize(path).joinToString("/")
        }
        return projectRoot.relativize(path).joinToString("/")
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

private val INLINE_PREFIX_CHARS = setOf('/', '.', '*', '?', '[', ']', '{', '}', '_', '-')
private val SYMBOL_PATTERN = Regex("""\b[A-Z][A-Za-z0-9_]*(?:\.[A-Za-z_$][A-Za-z0-9_$]*)+(?:\([^)]*\))?""")
private const val MAX_PATH_VARIANTS = 200
private val SKIPPED_DIRECTORIES = setOf(".git", ".gradle", ".idea", "build", "node_modules", "out")

private fun isInlinePrefixCharacter(character: Char): Boolean =
    character.isLetterOrDigit() || character in INLINE_PREFIX_CHARS
