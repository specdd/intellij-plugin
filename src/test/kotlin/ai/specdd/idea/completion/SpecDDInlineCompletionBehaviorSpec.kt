package ai.specdd.idea.completion

import ai.specdd.idea.directory
import ai.specdd.idea.file
import ai.specdd.idea.testVirtualRoot
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe

class SpecDDInlineCompletionBehaviorSpec : BehaviorSpec({
    given("SpecDD inline completion") {
        `when`("completion is requested for a project-root path prefix") {
            then("it returns matching project-relative paths") {
                val root = testVirtualRoot()
                root.directory("src/main")
                root.file("src/main/App.kt")
                root.file("README.md")

                val completion = SpecDDInlineCompletion.complete("Can read:\n  /src/ma", 19, root)

                completion!!.prefix shouldBe "/src/ma"
                completion.variants.map { variant -> variant.lookupString }
                    .shouldContainExactly("/src/main", "/src/main/App.kt")
            }
        }

        `when`("completion is requested without an explicit path prefix") {
            then("it returns no path variants") {
                val root = testVirtualRoot()
                root.directory("src/main")

                SpecDDInlineCompletion.complete("Can read:\n  src/ma", 18, root).shouldBeNull()
            }
        }

        `when`("completion walks skipped directories") {
            then("it does not return variants from heavy directories") {
                val root = testVirtualRoot()
                root.file("node_modules/pkg/index.js")

                SpecDDInlineCompletion.complete("References:\n  /node", 18, root).shouldBeNull()
            }
        }

        `when`("completion has more path matches than the variant cap") {
            then("it caps returned variants") {
                val root = testVirtualRoot()
                val source = root.directory("src")
                repeat(205) { index ->
                    source.file("file-$index.sdd")
                }

                val text = "References:\n  /src/file"
                val completion = SpecDDInlineCompletion.complete(text, text.length, root)

                completion!!.variants.size shouldBe 200
            }
        }

        `when`("completion has more directory matches than the variant cap") {
            then("it caps returned directory variants") {
                val root = testVirtualRoot()
                val source = root.directory("src")
                repeat(205) { index ->
                    source.directory("dir-$index")
                }

                val text = "References:\n  /src/dir"
                val completion = SpecDDInlineCompletion.complete(text, text.length, root)

                completion!!.variants.size shouldBe 200
            }
        }

        `when`("completion is requested for a current-directory relative path prefix") {
            then("it returns paths relative to the current spec directory") {
                val root = testVirtualRoot()
                val specDirectory = root.directory("specs")
                specDirectory.directory("local")
                specDirectory.file("local/example.sdd")
                root.file("src/main.sdd")

                val text = "References:\n  ./loc"
                val completion = SpecDDInlineCompletion.complete(text, text.length, root, specDirectory)

                completion!!.prefix shouldBe "./loc"
                completion.variants.map { variant -> variant.lookupString }
                    .shouldContainExactly("./local", "./local/example.sdd")
            }
        }

        `when`("completion is requested for a parent-directory relative path prefix") {
            then("it returns paths relative to the current spec directory") {
                val root = testVirtualRoot()
                val specDirectory = root.directory("specs")
                root.file("fixtures/kitchen-sink.sdd")

                val text = "References:\n  ../fi"
                val completion = SpecDDInlineCompletion.complete(text, text.length, root, specDirectory)

                completion!!.prefix shouldBe "../fi"
                completion.variants.map { variant -> variant.lookupString }
                    .shouldContainExactly("../fixtures", "../fixtures/kitchen-sink.sdd")
            }
        }

        `when`("the spec directory is outside the project root") {
            then("it returns no path variants") {
                val root = testVirtualRoot()
                root.file("main.sdd")
                val outside = testVirtualRoot("outside")

                SpecDDInlineCompletion.complete("References:\n  ./m", 17, root, outside).shouldBeNull()
            }
        }

        `when`("the project root is not a directory") {
            then("it returns no path variants") {
                val root = testVirtualRoot().file("main.sdd")

                SpecDDInlineCompletion.complete("References:\n  main", 17, root).shouldBeNull()
            }
        }

        `when`("completion is requested for an explicit local symbol prefix") {
            then("it returns matching at-prefixed symbol references from the file") {
                val text = "Purpose:\n  Use @SpecDD.Parser.classify and @SpecDD.Parser.render\nMust:\n  @SpecDD.P"

                val completion = SpecDDInlineCompletion.complete(text, text.length, null)

                completion!!.prefix shouldBe "@SpecDD.P"
                completion.variants.map { variant -> variant.lookupString }
                    .shouldContainExactly("@SpecDD.Parser.classify", "@SpecDD.Parser.render")
            }
        }

        `when`("completion is requested for a plain dotted symbol prefix") {
            then("it returns no local symbol variants") {
                val text = "Purpose:\n  Use SpecDD.Parser.classify\nMust:\n  SpecDD.P"

                SpecDDInlineCompletion.complete(text, text.length, null).shouldBeNull()
            }
        }

        `when`("local symbol references repeat") {
            then("it deduplicates symbol variants") {
                val root = testVirtualRoot()
                val text = "Purpose:\n  @SpecDD.Parser.classify and @SpecDD.Parser.classify\nMust:\n  @SpecDD.P"

                val completion = SpecDDInlineCompletion.complete(text, text.length, root)

                completion!!.variants.map { variant -> variant.lookupString }
                    .shouldContainExactly("@SpecDD.Parser.classify")
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

        `when`("the default project filter is used") {
            then("it accepts VFS entries") {
                SpecDDInlineCompletion.defaultProjectFilter(testVirtualRoot()) shouldBe true
            }
        }
    }
})
