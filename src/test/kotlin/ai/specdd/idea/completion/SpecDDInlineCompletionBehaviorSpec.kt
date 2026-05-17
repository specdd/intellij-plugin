package ai.specdd.idea.completion

import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import java.nio.file.Files
import java.nio.file.attribute.PosixFilePermissions
import kotlin.io.path.createDirectories
import kotlin.io.path.createFile
import kotlin.io.path.createTempDirectory

class SpecDDInlineCompletionBehaviorSpec : BehaviorSpec({
    given("SpecDD inline completion") {
        `when`("completion is requested for a project path prefix") {
            then("it returns matching project-relative paths") {
                val root = createTempDirectory()
                root.resolve("src/main").createDirectories()
                root.resolve("src/main/App.kt").createFile()
                root.resolve("README.md").createFile()

                val completion = SpecDDInlineCompletion.complete("Can read:\n  src/ma", 18, root)

                completion!!.prefix shouldBe "src/ma"
                completion.variants.map { variant -> variant.lookupString }
                    .shouldContainExactly("src/main", "src/main/App.kt")
            }
        }

        `when`("completion walks skipped directories") {
            then("it does not return variants from heavy directories") {
                val root = createTempDirectory()
                root.resolve("node_modules/pkg/index.js").parent.createDirectories()
                root.resolve("node_modules/pkg/index.js").createFile()

                SpecDDInlineCompletion.complete("References:\n  node", 17, root).shouldBeNull()
            }
        }

        `when`("completion has more path matches than the variant cap") {
            then("it caps returned variants") {
                val root = createTempDirectory()
                val source = root.resolve("src").createDirectories()
                repeat(205) { index ->
                    source.resolve("file-$index.sdd").createFile()
                }

                val text = "References:\n  src/file"
                val completion = SpecDDInlineCompletion.complete(text, text.length, root)

                completion!!.variants.size shouldBe 200
            }
        }

        `when`("completion has more directory matches than the variant cap") {
            then("it caps returned directory variants") {
                val root = createTempDirectory()
                val source = root.resolve("src").createDirectories()
                repeat(205) { index ->
                    source.resolve("dir-$index").createDirectories()
                }

                val text = "References:\n  src/dir"
                val completion = SpecDDInlineCompletion.complete(text, text.length, root)

                completion!!.variants.size shouldBe 200
            }
        }

        `when`("completion encounters an unreadable directory") {
            then("it continues without throwing") {
                val root = createTempDirectory()
                val unreadable = root.resolve("src/unreadable").createDirectories()
                Files.setPosixFilePermissions(unreadable, PosixFilePermissions.fromString("---------"))

                try {
                    val text = "References:\n  src"
                    val completion = SpecDDInlineCompletion.complete(text, text.length, root)

                    completion!!.variants.map { variant -> variant.lookupString }
                        .shouldContainExactly("src")
                } finally {
                    Files.setPosixFilePermissions(unreadable, PosixFilePermissions.fromString("rwx------"))
                }
            }
        }

        `when`("completion is requested for a current-directory relative path prefix") {
            then("it returns paths relative to the current spec directory") {
                val root = createTempDirectory()
                val specDirectory = root.resolve("specs").createDirectories()
                specDirectory.resolve("local").createDirectories()
                specDirectory.resolve("local/example.sdd").createFile()
                root.resolve("src/main.sdd").parent.createDirectories()
                root.resolve("src/main.sdd").createFile()

                val text = "References:\n  ./loc"
                val completion = SpecDDInlineCompletion.complete(text, text.length, root, specDirectory)

                completion!!.prefix shouldBe "./loc"
                completion.variants.map { variant -> variant.lookupString }
                    .shouldContainExactly("./local", "./local/example.sdd")
            }
        }

        `when`("completion is requested for a parent-directory relative path prefix") {
            then("it returns paths relative to the current spec directory") {
                val root = createTempDirectory()
                val specDirectory = root.resolve("specs").createDirectories()
                root.resolve("fixtures/kitchen-sink.sdd").parent.createDirectories()
                root.resolve("fixtures/kitchen-sink.sdd").createFile()

                val text = "References:\n  ../fi"
                val completion = SpecDDInlineCompletion.complete(text, text.length, root, specDirectory)

                completion!!.prefix shouldBe "../fi"
                completion.variants.map { variant -> variant.lookupString }
                    .shouldContainExactly("../fixtures", "../fixtures/kitchen-sink.sdd")
            }
        }

        `when`("the spec directory is outside the project root") {
            then("it returns no path variants") {
                val root = createTempDirectory()
                root.resolve("main.sdd").createFile()
                val outside = createTempDirectory()

                SpecDDInlineCompletion.complete("References:\n  ./m", 17, root, outside).shouldBeNull()
            }
        }

        `when`("the project root is not a directory") {
            then("it returns no path variants") {
                val root = createTempDirectory().resolve("main.sdd").createFile()

                SpecDDInlineCompletion.complete("References:\n  main", 17, root).shouldBeNull()
            }
        }

        `when`("completion is requested for a local symbol prefix") {
            then("it returns matching symbol-like tokens from the file") {
                val text = "Purpose:\n  Use SpecDD.Parser.classify and SpecDD.Parser.render\nMust:\n  SpecDD.P"

                val completion = SpecDDInlineCompletion.complete(text, text.length, null)

                completion!!.prefix shouldBe "SpecDD.P"
                completion.variants.map { variant -> variant.lookupString }
                    .shouldContainExactly("SpecDD.Parser.classify", "SpecDD.Parser.render")
            }
        }

        `when`("file paths and local symbols overlap") {
            then("it deduplicates variants while keeping paths first") {
                val root = createTempDirectory()
                root.resolve("SpecDD.Parser.classify").createFile()
                val text = "Purpose:\n  SpecDD.Parser.classify\nMust:\n  SpecDD.P"

                val completion = SpecDDInlineCompletion.complete(text, text.length, root)

                completion!!.variants.map { variant -> variant.lookupString }
                    .shouldContainExactly("SpecDD.Parser.classify")
            }
        }

        `when`("completion is requested outside inline prefixes") {
            then("it returns no completion") {
                SpecDDInlineCompletion.complete("Purpose:\n  ", 11, null).shouldBeNull()
                SpecDDInlineCompletion.complete("Purpose", -1, null).shouldBeNull()
                SpecDDInlineCompletion.complete("Purpose", 8, null).shouldBeNull()
            }
        }

        `when`("a completion variant is inspected") {
            then("it exposes a lookup element") {
                val variant = SpecDDInlineCompletionVariant("src/main")

                variant.toLookupElement().lookupString shouldBe "src/main"
            }
        }
    }
})
