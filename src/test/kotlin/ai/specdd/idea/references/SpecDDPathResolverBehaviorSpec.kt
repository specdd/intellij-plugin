package ai.specdd.idea.references

import com.intellij.openapi.util.TextRange
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import java.nio.file.Files
import java.nio.file.attribute.PosixFilePermissions
import kotlin.io.path.createDirectories
import kotlin.io.path.createFile
import kotlin.io.path.createTempDirectory

class SpecDDPathResolverBehaviorSpec : BehaviorSpec({
    given("a SpecDD path resolver") {
        val resolver = SpecDDPathResolver()

        `when`("an exact existing file is resolved") {
            then("it returns the normalized target") {
                val root = createTempDirectory()
                val specDirectory = root.resolve("specs").createDirectories()
                val target = specDirectory.resolve("main.sdd").createFile()

                val resolution = resolver.resolve(candidate("main.sdd"), context(root, specDirectory))

                resolution.status shouldBe SpecDDPathResolutionStatus.RESOLVED
                resolution.targets.shouldContainExactly(listOf(target))
            }
        }

        `when`("a glob matches files and directories") {
            then("it returns all matching project targets") {
                val root = createTempDirectory()
                val specDirectory = root
                val directory = root.resolve("src/main").createDirectories()
                val file = root.resolve("src/App.kt").createFile()
                root.resolve("other/App.kt").parent.createDirectories()
                root.resolve("other/App.kt").createFile()

                val resolution = resolver.resolve(candidate("src/*"), context(root, specDirectory))

                resolution.status shouldBe SpecDDPathResolutionStatus.RESOLVED
                resolution.targets.shouldContainExactly(listOf(directory, file))
            }
        }

        `when`("a glob pattern is malformed") {
            then("it is unresolved without throwing") {
                val root = createTempDirectory()

                resolver.resolve(candidate("src/[broken"), context(root, root)).status shouldBe
                        SpecDDPathResolutionStatus.UNRESOLVED
            }
        }

        `when`("a glob walks skipped directories") {
            then("it does not return targets from heavy directories") {
                val root = createTempDirectory()
                root.resolve(".git/hidden.sdd").parent.createDirectories()
                root.resolve(".git/hidden.sdd").createFile()

                resolver.resolve(candidate(".git/*"), context(root, root)).status shouldBe
                        SpecDDPathResolutionStatus.UNRESOLVED
            }
        }

        `when`("a glob matches more than the result cap") {
            then("it caps returned targets") {
                val root = createTempDirectory()
                repeat(505) { index ->
                    root.resolve("file-$index.sdd").createFile()
                }

                val resolution = resolver.resolve(candidate("*.sdd"), context(root, root))

                resolution.status shouldBe SpecDDPathResolutionStatus.RESOLVED
                resolution.targets.size shouldBe 500
            }
        }

        `when`("a glob matches more directories than the result cap") {
            then("it caps returned directory targets") {
                val root = createTempDirectory()
                repeat(505) { index ->
                    root.resolve("dir-$index").createDirectories()
                }

                val resolution = resolver.resolve(candidate("dir-*"), context(root, root))

                resolution.status shouldBe SpecDDPathResolutionStatus.RESOLVED
                resolution.targets.size shouldBe 500
            }
        }

        `when`("a glob encounters an unreadable directory") {
            then("it continues without throwing") {
                val root = createTempDirectory()
                val unreadable = root.resolve("unreadable").createDirectories()
                root.resolve("visible.sdd").createFile()
                Files.setPosixFilePermissions(unreadable, PosixFilePermissions.fromString("---------"))

                try {
                    resolver.resolve(candidate("*"), context(root, root)).status shouldBe
                            SpecDDPathResolutionStatus.RESOLVED
                } finally {
                    Files.setPosixFilePermissions(unreadable, PosixFilePermissions.fromString("rwx------"))
                }
            }
        }

        `when`("a missing path-like candidate is resolved") {
            then("it is unresolved") {
                val root = createTempDirectory()

                resolver.resolve(candidate("missing/file.sdd"), context(root, root)).status shouldBe
                        SpecDDPathResolutionStatus.UNRESOLVED
            }
        }

        `when`("a missing prose-like candidate is resolved") {
            then("it is ignored") {
                val root = createTempDirectory()

                resolver.resolve(candidate("Shared parser", hasPathSyntax = false), context(root, root)).status shouldBe
                        SpecDDPathResolutionStatus.IGNORED
            }
        }

        `when`("a candidate escapes the project root") {
            then("it is unresolved when it has path syntax") {
                val root = createTempDirectory()

                resolver.resolve(candidate("../outside.txt"), context(root, root)).status shouldBe
                        SpecDDPathResolutionStatus.UNRESOLVED
            }
        }

        `when`("the spec directory is outside the project root") {
            then("it ignores the candidate") {
                val root = createTempDirectory()
                val outside = createTempDirectory()

                resolver.resolve(candidate("file.sdd"), context(root, outside)).status shouldBe
                        SpecDDPathResolutionStatus.IGNORED
            }
        }
    }
})

private fun candidate(text: String, hasPathSyntax: Boolean = true): SpecDDPathCandidate =
    SpecDDPathCandidate(text, TextRange(0, text.length), hasPathSyntax)

private fun context(root: java.nio.file.Path, specDirectory: java.nio.file.Path): SpecDDPathResolutionContext =
    SpecDDPathResolutionContext(root, specDirectory)
