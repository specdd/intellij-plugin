package ai.specdd.idea.references

import com.intellij.openapi.vfs.VirtualFile

class SpecDDPathResolver {
    fun resolve(candidate: SpecDDPathCandidate, context: SpecDDPathResolutionContext): SpecDDPathResolution {
        if (!isInRoot(context.specDirectory, context.projectRoot)) return SpecDDPathResolution.ignored()

        val targets = if (candidate.isGlob) {
            resolveGlob(candidate.text, context)
        } else {
            resolveExact(candidate.text, context)
        }

        if (targets.isNotEmpty()) return SpecDDPathResolution.resolved(targets)
        if (!candidate.hasPathSyntax) return SpecDDPathResolution.ignored()
        return SpecDDPathResolution.unresolved()
    }

    private fun resolveExact(text: String, context: SpecDDPathResolutionContext): List<VirtualFile> {
        val target = resolveCandidatePath(text, context) ?: return emptyList()
        if (!isInRoot(target, context.projectRoot)) return emptyList()
        if (!context.isInProject(target)) return emptyList()
        return listOf(target)
    }

    private fun resolveGlob(pattern: String, context: SpecDDPathResolutionContext): List<VirtualFile> {
        val matcher = globMatcher(globPatternText(pattern)) ?: return emptyList()
        val targets = mutableListOf<VirtualFile>()
        val stack = ArrayDeque<VirtualFile>()
        stack.add(context.projectRoot)

        while (stack.isNotEmpty() && targets.size < MAX_GLOB_TARGETS) {
            val file = stack.removeLast()
            if (file != context.projectRoot && file.isDirectory && file.name in SKIPPED_DIRECTORIES) continue
            if (file != context.projectRoot && !context.isInProject(file)) continue

            val relativePath = candidateRelativePath(pattern, context, file)
            if (file != context.projectRoot && null != relativePath && matcher(relativePath)) {
                targets.add(file)
            }

            if (file.isDirectory) {
                file.children.reversedArray().forEach { child -> stack.add(child) }
            }
        }

        return targets.sortedWith(pathComparator())
    }

    private fun pathComparator(): Comparator<VirtualFile> =
        compareBy<VirtualFile> { file -> !file.isDirectory }
            .thenBy { file -> file.path }
}

private fun resolveCandidatePath(text: String, context: SpecDDPathResolutionContext): VirtualFile? {
    val normalized = text.replace('\\', '/')
    return when {
        normalized.startsWith("/") -> resolveRelative(context.projectRoot, normalized.removePrefix("/"))
        else -> resolveRelative(context.specDirectory, normalized)
    }
}

private fun candidateRelativePath(
    pattern: String,
    context: SpecDDPathResolutionContext,
    file: VirtualFile,
): String? {
    return when {
        pattern.startsWith("/") -> relativePath(context.projectRoot, file)
        pattern.startsWith("./") -> if (isInRoot(file, context.specDirectory)) {
            relativePath(context.specDirectory, file)
        } else {
            null
        }
        else -> relativePath(context.specDirectory, file)
    }
}

private fun globPatternText(pattern: String): String {
    return when {
        pattern.startsWith("/") -> pattern.removePrefix("/")
        pattern.startsWith("./") -> pattern.removePrefix("./")
        else -> pattern
    }
}

class SpecDDPathResolutionContext(
    val projectRoot: VirtualFile,
    val specDirectory: VirtualFile,
    val isInProject: (VirtualFile) -> Boolean = { true },
)

data class SpecDDPathResolution(
    val status: SpecDDPathResolutionStatus,
    val targets: List<VirtualFile>,
) {
    companion object {
        fun resolved(targets: List<VirtualFile>): SpecDDPathResolution =
            SpecDDPathResolution(SpecDDPathResolutionStatus.RESOLVED, targets)

        fun unresolved(): SpecDDPathResolution =
            SpecDDPathResolution(SpecDDPathResolutionStatus.UNRESOLVED, emptyList())

        fun ignored(): SpecDDPathResolution =
            SpecDDPathResolution(SpecDDPathResolutionStatus.IGNORED, emptyList())
    }
}

enum class SpecDDPathResolutionStatus {
    RESOLVED,
    UNRESOLVED,
    IGNORED,
}

internal val SpecDDPathCandidate.isGlob: Boolean
    get() = text.any { character -> character in GLOB_CHARS }

internal fun resolveRelative(base: VirtualFile, relativePath: String): VirtualFile? {
    var current: VirtualFile? = base
    relativePath.replace('\\', '/').split('/').forEach { segment ->
        current = when (segment) {
            "", "." -> current
            ".." -> current?.parent
            else -> current?.findChild(segment)
        }
        if (null == current) return null
    }
    return current
}

internal fun isInRoot(file: VirtualFile, root: VirtualFile): Boolean {
    var current: VirtualFile? = file
    while (null != current) {
        if (current == root) return true
        current = current.parent
    }
    return false
}

internal fun relativePath(base: VirtualFile, target: VirtualFile): String {
    val baseParts = normalizedPathParts(base)
    val targetParts = normalizedPathParts(target)
    val commonSize = baseParts.zip(targetParts).takeWhile { (left, right) -> left == right }.size
    val parentSegments = List(baseParts.size - commonSize) { ".." }
    val targetSegments = targetParts.drop(commonSize)
    return (parentSegments + targetSegments).joinToString("/")
}

private fun normalizedPathParts(file: VirtualFile): List<String> =
    file.path
        .replace('\\', '/')
        .split('/')
        .filter { part -> part.isNotEmpty() }

private fun globMatcher(pattern: String): ((String) -> Boolean)? {
    val regexText = globToRegex(pattern) ?: return null
    val regex = try {
        Regex(regexText)
    } catch (_: IllegalArgumentException) {
        return null
    }
    return { text -> regex.matches(text) }
}

private fun globToRegex(pattern: String): String? {
    val builder = StringBuilder("^")
    var index = 0

    while (index < pattern.length) {
        when (val character = pattern[index]) {
            '*' -> {
                if (index + 1 < pattern.length && '*' == pattern[index + 1]) {
                    if (index + 2 < pattern.length && '/' == pattern[index + 2]) {
                        builder.append("(?:.*/)?")
                        index += 2
                    } else {
                        builder.append(".*")
                        index += 1
                    }
                } else {
                    builder.append("[^/]*")
                }
            }
            '?' -> builder.append("[^/]")
            '[' -> {
                val end = pattern.indexOf(']', startIndex = index + 1)
                if (-1 == end) return null
                builder.append(pattern.substring(index, end + 1))
                index = end
            }
            '{' -> {
                val end = pattern.indexOf('}', startIndex = index + 1)
                if (-1 == end) return null
                builder.append("(?:")
                builder.append(
                    pattern.substring(index + 1, end)
                        .split(',')
                        .joinToString("|") { alternative -> Regex.escape(alternative) },
                )
                builder.append(")")
                index = end
            }
            else -> builder.append(Regex.escape(character.toString()))
        }
        index += 1
    }

    builder.append("$")
    return builder.toString()
}

internal val GLOB_CHARS = setOf('*', '?', '[', ']', '{', '}')
private const val MAX_GLOB_TARGETS = 500
private val SKIPPED_DIRECTORIES = setOf(".git", ".gradle", ".idea", "build", "node_modules", "out")
