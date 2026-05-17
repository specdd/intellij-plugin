package ai.specdd.idea.references

import java.io.IOException
import java.nio.file.*
import java.nio.file.attribute.BasicFileAttributes
import kotlin.io.path.absolute
import kotlin.io.path.isDirectory

class SpecDDPathResolver {
    fun resolve(candidate: SpecDDPathCandidate, context: SpecDDPathResolutionContext): SpecDDPathResolution {
        val projectRoot = context.projectRoot.absolute().normalize()
        val specDirectory = context.specDirectory.absolute().normalize()
        if (!specDirectory.startsWith(projectRoot)) return SpecDDPathResolution.ignored()

        val targets = if (candidate.isGlob) {
            resolveGlob(candidate.text, projectRoot, specDirectory)
        } else {
            resolveExact(candidate.text, projectRoot, specDirectory)
        }

        if (targets.isNotEmpty()) return SpecDDPathResolution.resolved(targets)
        if (!candidate.hasPathSyntax) return SpecDDPathResolution.ignored()
        return SpecDDPathResolution.unresolved()
    }

    private fun resolveExact(text: String, projectRoot: Path, specDirectory: Path): List<Path> {
        val target = specDirectory.resolve(text).normalize()
        if (!target.startsWith(projectRoot)) return emptyList()
        if (!Files.exists(target)) return emptyList()
        return listOf(target)
    }

    private fun resolveGlob(pattern: String, projectRoot: Path, specDirectory: Path): List<Path> {
        val normalizedBase = specDirectory.normalize()
        val matcher = try {
            FileSystems.getDefault().getPathMatcher("glob:$pattern")
        } catch (_: IllegalArgumentException) {
            return emptyList()
        }
        val targets = mutableListOf<Path>()

        Files.walkFileTree(
            projectRoot,
            object : SimpleFileVisitor<Path>() {
                override fun preVisitDirectory(dir: Path, attrs: BasicFileAttributes): FileVisitResult {
                    if (dir != projectRoot && dir.fileName?.toString() in SKIPPED_DIRECTORIES) {
                        return FileVisitResult.SKIP_SUBTREE
                    }
                    addIfMatch(dir)
                    return if (MAX_GLOB_TARGETS <= targets.size) {
                        FileVisitResult.TERMINATE
                    } else {
                        FileVisitResult.CONTINUE
                    }
                }

                override fun visitFile(file: Path, attrs: BasicFileAttributes): FileVisitResult {
                    addIfMatch(file)
                    return if (MAX_GLOB_TARGETS <= targets.size) {
                        FileVisitResult.TERMINATE
                    } else {
                        FileVisitResult.CONTINUE
                    }
                }

                override fun visitFileFailed(file: Path, exc: IOException): FileVisitResult =
                    FileVisitResult.CONTINUE

                private fun addIfMatch(path: Path) {
                    if (path == projectRoot || !path.startsWith(projectRoot)) return
                    if (matcher.matches(normalizedBase.relativize(path))) {
                        targets.add(path)
                    }
                }
            }
        )

        return targets.sortedWith(pathComparator())
    }

    private fun pathComparator(): Comparator<Path> =
        compareBy<Path> { path -> !path.isDirectory() }
            .thenBy { path -> path.toString() }
}

data class SpecDDPathResolutionContext(
    val projectRoot: Path,
    val specDirectory: Path,
)

data class SpecDDPathResolution(
    val status: SpecDDPathResolutionStatus,
    val targets: List<Path>,
) {
    companion object {
        fun resolved(targets: List<Path>): SpecDDPathResolution =
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

private val SpecDDPathCandidate.isGlob: Boolean
    get() = text.any { character -> character in GLOB_CHARS }

internal val GLOB_CHARS = setOf('*', '?', '[', ']', '{', '}')
private const val MAX_GLOB_TARGETS = 500
private val SKIPPED_DIRECTORIES = setOf(".git", ".gradle", ".idea", "build", "node_modules", "out")
