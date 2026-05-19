package ai.specdd.idea.references

import ai.specdd.idea.directory
import ai.specdd.idea.file
import ai.specdd.idea.testVirtualRoot
import com.intellij.openapi.util.TextRange
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe

class SpecDDPathResolverBehaviorSpec : BehaviorSpec({
    given("a SpecDD path resolver") {
        val resolver = SpecDDPathResolver()

        `when`("an exact existing file is resolved") {
            then("it returns the normalized target") {
                val root = testVirtualRoot()
                val specDirectory = root.directory("specs")
                val target = specDirectory.file("main.sdd")

                val resolution = resolver.resolve(candidate("main.sdd"), context(root, specDirectory))

                resolution.status shouldBe SpecDDPathResolutionStatus.RESOLVED
                resolution.targets.shouldContainExactly(listOf(target))
            }
        }

        `when`("a glob matches files and directories") {
            then("it returns all matching project targets") {
                val root = testVirtualRoot()
                val specDirectory = root
                val directory = root.directory("src/main")
                val file = root.file("src/App.kt")
                root.file("other/App.kt")

                val resolution = resolver.resolve(candidate("src/*"), context(root, specDirectory))

                resolution.status shouldBe SpecDDPathResolutionStatus.RESOLVED
                resolution.targets.shouldContainExactly(listOf(directory, file))
            }
        }

        `when`("a glob pattern is malformed") {
            then("it is unresolved without throwing") {
                val root = testVirtualRoot()

                resolver.resolve(candidate("src/[broken"), context(root, root)).status shouldBe
                        SpecDDPathResolutionStatus.UNRESOLVED
            }
        }

        `when`("a glob pattern compiles to an invalid regex") {
            then("it is unresolved without throwing") {
                val root = testVirtualRoot()

                resolver.resolve(candidate("[z-a]"), context(root, root)).status shouldBe
                        SpecDDPathResolutionStatus.UNRESOLVED
            }
        }

        `when`("glob patterns use single-character, character-class, and brace syntax") {
            then("it matches the supported pattern forms") {
                val root = testVirtualRoot()
                val first = root.file("src/a.sdd")
                val second = root.file("src/b.sdd")
                root.file("src/long.sdd")

                resolver.resolve(candidate("src/?.sdd"), context(root, root)).targets
                    .shouldContainExactly(first, second)
                resolver.resolve(candidate("src/[ab].sdd"), context(root, root)).targets
                    .shouldContainExactly(first, second)
                resolver.resolve(candidate("src/{a,b}.sdd"), context(root, root)).targets
                    .shouldContainExactly(first, second)
            }
        }

        `when`("a current-directory glob uses a single segment wildcard") {
            then("it matches only direct children of the spec directory") {
                val root = testVirtualRoot()
                val specDirectory = root.directory("specs")
                val target = specDirectory.file("root.sdd")
                specDirectory.file("nested/example.sdd")

                resolver.resolve(candidate("./*.sdd"), context(root, specDirectory)).targets
                    .shouldContainExactly(target)
            }
        }

        `when`("a current-directory glob uses a globstar directory wildcard") {
            then("it matches direct and nested children of the spec directory") {
                val root = testVirtualRoot()
                val specDirectory = root.directory("specs")
                val nested = specDirectory.file("nested/deep/example.sdd")
                val rootLevel = specDirectory.file("root.sdd")
                specDirectory.file("nested/deep/example.txt")
                root.file("outside.sdd")

                resolver.resolve(candidate("./**/*.sdd"), context(root, specDirectory)).targets
                    .shouldContainExactly(nested, rootLevel)
            }
        }

        `when`("a current-directory glob uses a bare globstar wildcard") {
            then("it matches across directory boundaries") {
                val root = testVirtualRoot()
                val specDirectory = root.directory("specs")
                val nested = specDirectory.file("nested/deep/example.sdd")
                val rootLevel = specDirectory.file("root.sdd")
                specDirectory.file("nested/deep/example.txt")

                resolver.resolve(candidate("./**.sdd"), context(root, specDirectory)).targets
                    .shouldContainExactly(nested, rootLevel)
            }
        }

        `when`("a project-root glob uses a globstar directory wildcard") {
            then("it matches recursively from the project root") {
                val root = testVirtualRoot()
                val specDirectory = root.directory("specs")
                val target = root.file("src/main/App.kt")
                root.file("src/main/App.txt")

                resolver.resolve(candidate("/src/**/*.kt"), context(root, specDirectory)).targets
                    .shouldContainExactly(target)
            }
        }

        `when`("a parent-directory glob uses a globstar directory wildcard") {
            then("it matches recursively from the spec directory parent") {
                val root = testVirtualRoot()
                val specDirectory = root.directory("specs/local")
                val target = root.file("specs/shared/nested/example.sdd")
                specDirectory.file("nested/local.sdd")
                root.file("outside.sdd")

                resolver.resolve(candidate("../shared/**/*.sdd"), context(root, specDirectory)).targets
                    .shouldContainExactly(target)
            }
        }

        `when`("an exact path contains current-directory segments") {
            then("it resolves the normalized target") {
                val root = testVirtualRoot()
                val target = root.file("main.sdd")

                resolver.resolve(candidate("./main.sdd"), context(root, root)).targets
                    .shouldContainExactly(target)
            }
        }

        `when`("an exact path uses a project-root prefix") {
            then("it resolves from the project root") {
                val root = testVirtualRoot()
                val specDirectory = root.directory("specs")
                val target = root.file("main.sdd")

                resolver.resolve(candidate("/main.sdd"), context(root, specDirectory)).targets
                    .shouldContainExactly(target)
            }
        }

        `when`("an exact path uses a project-root prefix from a nested source spec") {
            then("it resolves from the project root instead of the nested source directory") {
                val root = testVirtualRoot()
                val specDirectory = root.directory("src/main/kotlin/ai/specdd/idea/references")
                val target = root.file("Makefile")

                resolver.resolve(candidate("/Makefile"), context(root, specDirectory)).targets
                    .shouldContainExactly(target)
            }
        }

        `when`("a glob walks skipped directories") {
            then("it does not return targets from heavy directories") {
                val root = testVirtualRoot()
                root.file(".git/hidden.sdd")

                resolver.resolve(candidate(".git/*"), context(root, root)).status shouldBe
                        SpecDDPathResolutionStatus.UNRESOLVED
            }
        }

        `when`("a glob matches more than the result cap") {
            then("it caps returned targets") {
                val root = testVirtualRoot()
                repeat(505) { index ->
                    root.file("file-$index.sdd")
                }

                val resolution = resolver.resolve(candidate("*.sdd"), context(root, root))

                resolution.status shouldBe SpecDDPathResolutionStatus.RESOLVED
                resolution.targets.size shouldBe 500
            }
        }

        `when`("a glob matches more directories than the result cap") {
            then("it caps returned directory targets") {
                val root = testVirtualRoot()
                repeat(505) { index ->
                    root.directory("dir-$index")
                }

                val resolution = resolver.resolve(candidate("dir-*"), context(root, root))

                resolution.status shouldBe SpecDDPathResolutionStatus.RESOLVED
                resolution.targets.size shouldBe 500
            }
        }

        `when`("a missing path-like candidate is resolved") {
            then("it is unresolved") {
                val root = testVirtualRoot()

                resolver.resolve(candidate("missing/file.sdd"), context(root, root)).status shouldBe
                        SpecDDPathResolutionStatus.UNRESOLVED
            }
        }

        `when`("a missing prose-like candidate is resolved") {
            then("it is ignored") {
                val root = testVirtualRoot()

                resolver.resolve(candidate("Shared parser", hasPathSyntax = false), context(root, root)).status shouldBe
                        SpecDDPathResolutionStatus.IGNORED
            }
        }

        `when`("a candidate escapes the project root") {
            then("it is unresolved when it has path syntax") {
                val root = testVirtualRoot()

                resolver.resolve(candidate("../outside.txt"), context(root, root)).status shouldBe
                        SpecDDPathResolutionStatus.UNRESOLVED
            }
        }

        `when`("the spec directory is outside the project root") {
            then("it ignores the candidate") {
                val root = testVirtualRoot()
                val outside = testVirtualRoot("outside")

                resolver.resolve(candidate("file.sdd"), context(root, outside)).status shouldBe
                        SpecDDPathResolutionStatus.IGNORED
            }
        }
    }
})

private fun candidate(text: String, hasPathSyntax: Boolean = true): SpecDDPathCandidate =
    SpecDDPathCandidate(text, TextRange(0, text.length), hasPathSyntax)

private fun context(root: com.intellij.openapi.vfs.VirtualFile, specDirectory: com.intellij.openapi.vfs.VirtualFile): SpecDDPathResolutionContext =
    SpecDDPathResolutionContext(root, specDirectory)
