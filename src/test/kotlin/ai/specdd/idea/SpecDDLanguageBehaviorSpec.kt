package ai.specdd.idea

import com.intellij.lang.ASTNode
import com.intellij.lang.PsiBuilder
import com.intellij.psi.FileViewProvider
import com.intellij.psi.PsiElement
import com.intellij.psi.TokenType
import com.intellij.psi.codeStyle.CodeStyleSettingsCustomizable
import com.intellij.psi.codeStyle.LanguageCodeStyleSettingsProvider
import com.intellij.psi.tree.IElementType
import com.intellij.psi.tree.TokenSet
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.types.shouldBeInstanceOf
import io.kotest.matchers.types.shouldBeSameInstanceAs
import sun.misc.Unsafe
import java.lang.reflect.InvocationHandler
import java.lang.reflect.Proxy

class SpecDDLanguageBehaviorSpec : BehaviorSpec({
    given("the SpecDD language definition") {
        `when`("its identity is requested") {
            then("it exposes the SpecDD language id") {
                SpecDDLanguage.id shouldBe "SpecDD"
            }
        }
    }

    given("the SpecDD file type") {
        `when`("its metadata is requested") {
            then("it exposes .sdd file type details") {
                val fileType = SpecDDFileType()

                fileType.language shouldBeSameInstanceAs SpecDDLanguage
                fileType.name shouldBe "SpecDD"
                fileType.description shouldBe "SpecDD specification file"
                fileType.defaultExtension shouldBe "sdd"
                fileType.icon.shouldNotBeNull()
            }
        }

        `when`("the IntelliJ extension system creates it") {
            then("it can be instantiated as an extension class") {
                val constructor = SpecDDFileType::class.java.getDeclaredConstructor()

                constructor.newInstance().shouldNotBeNull()
            }
        }
    }

    given("the SpecDD parser definition") {
        val parserDefinition = SpecDDParserDefinition()

        `when`("its static token contracts are requested") {
            then("it exposes the SpecDD file node and no special token sets") {
                parserDefinition.fileNodeType shouldBe SpecDDElementTypes.FILE
                parserDefinition.whitespaceTokens shouldBe TokenSet.EMPTY
                parserDefinition.commentTokens shouldBe TokenSet.EMPTY
                parserDefinition.stringLiteralElements shouldBe TokenSet.EMPTY
            }
        }

        `when`("its lexer is used") {
            then("it creates one PSI token over the requested buffer slice") {
                val lexer = parserDefinition.createLexer(null)
                val text = "xxSpec:\nNot exists:yy"

                lexer.start(text, 2, text.length - 2, 0)

                lexer.state shouldBe 0
                lexer.bufferSequence shouldBe text
                lexer.bufferEnd shouldBe text.length - 2
                lexer.tokenType shouldBe SpecDDElementTypes.TEXT
                lexer.tokenStart shouldBe 2
                lexer.tokenEnd shouldBe text.length - 2

                lexer.advance()

                lexer.tokenType shouldBe null
                lexer.tokenStart shouldBe text.length - 2
                lexer.tokenEnd shouldBe text.length - 2
            }
        }

        `when`("its parser is used") {
            then("it consumes all tokens into the SpecDD file node") {
                val builder = RecordingPsiBuilder()

                parserDefinition.createParser(null).parse(SpecDDElementTypes.FILE, builder.proxy)

                builder.events.shouldContainExactly("mark", "eof:false", "advanceLexer", "eof:true", "done:FILE")
            }
        }

        `when`("a PSI element is requested for a node") {
            then("it creates a wrapper PSI element") {
                parserDefinition.createElement(astNode()).shouldNotBeNull()
            }
        }

        `when`("a PSI file is requested with a proxy view provider") {
            then("it reaches the SpecDD PSI file creation path") {
                shouldThrow<ClassCastException> {
                    parserDefinition.createFile(fileViewProvider())
                }
            }
        }

        `when`("the SpecDD PSI file metadata is requested") {
            then("it exposes the SpecDD file type and debug name") {
                val file = uninitializedSpecDDPsiFile()

                file.fileType.shouldBeInstanceOf<SpecDDFileType>()
                file.toString() shouldBe "SpecDD file"
            }
        }

        `when`("a reference is requested from the SpecDD PSI file") {
            then("it delegates to path reference lookup") {
                val file = uninitializedSpecDDPsiFile()
                val previous = specDDPathReferenceAt
                val offsets = mutableListOf<Int>()
                specDDPathReferenceAt = { _, offset ->
                    offsets.add(offset)
                    null
                }

                try {
                    file.findReferenceAt(7) shouldBe null
                } finally {
                    specDDPathReferenceAt = previous
                }

                offsets.shouldContainExactly(7)
            }
        }

        `when`("the default path reference lookup has no containing file") {
            then("it returns no reference") {
                specDDPathReferenceAt(minimalPsiElement(), 0) shouldBe null
            }
        }

        `when`("space requirements are requested") {
            then("spaces may exist between any tokens") {
                parserDefinition.spaceExistenceTypeBetweenTokens(null, null) shouldBe
                        com.intellij.lang.ParserDefinition.SpaceRequirements.MAY
            }
        }
    }

    given("the SpecDD code style settings provider") {
        val provider = SpecDDCodeStyleSettingsProvider()

        `when`("default common settings are requested") {
            then("it uses two-space indentation") {
                @Suppress("DEPRECATION")
                val indentOptions = provider.defaultCommonSettings.indentOptions
                indentOptions.shouldNotBeNull()

                provider.language shouldBeSameInstanceAs SpecDDLanguage
                indentOptions.INDENT_SIZE shouldBe 2
                indentOptions.CONTINUATION_INDENT_SIZE shouldBe 2
                indentOptions.TAB_SIZE shouldBe 2
                indentOptions.USE_TAB_CHARACTER shouldBe false
            }
        }

        `when`("a code style sample is requested") {
            then("it returns a SpecDD sample") {
                provider.getCodeSample(LanguageCodeStyleSettingsProvider.SettingsType.INDENT_SETTINGS) shouldContain
                        "Purpose:\n  Describe"
            }
        }

        `when`("indent settings are customized") {
            then("it exposes standard indent controls") {
                val customizable = RecordingCodeStyleSettingsCustomizable()

                provider.customizeSettings(
                    customizable.proxy,
                    LanguageCodeStyleSettingsProvider.SettingsType.INDENT_SETTINGS
                )

                customizable.standardOptions.shouldContainExactly("INDENT_SIZE", "TAB_SIZE", "USE_TAB_CHARACTER")
            }
        }

        `when`("non-indent settings are customized") {
            then("it does not expose additional controls") {
                val customizable = RecordingCodeStyleSettingsCustomizable()

                provider.customizeSettings(
                    customizable.proxy,
                    LanguageCodeStyleSettingsProvider.SettingsType.SPACING_SETTINGS
                )

                customizable.standardOptions shouldBe emptyList()
            }
        }
    }
})

private class RecordingCodeStyleSettingsCustomizable {
    val standardOptions = mutableListOf<String>()

    val proxy: CodeStyleSettingsCustomizable = Proxy.newProxyInstance(
        CodeStyleSettingsCustomizable::class.java.classLoader,
        arrayOf(CodeStyleSettingsCustomizable::class.java),
        InvocationHandler { _, method, args ->
            if ("showStandardOptions" == method.name && args?.firstOrNull() is Array<*>) {
                standardOptions.addAll((args.first() as Array<*>).filterIsInstance<String>())
            }
            null
        },
    ) as CodeStyleSettingsCustomizable
}

private class RecordingPsiBuilder {
    val events = mutableListOf<String>()
    private var advanced = false

    val proxy: PsiBuilder = Proxy.newProxyInstance(
        PsiBuilder::class.java.classLoader,
        arrayOf(PsiBuilder::class.java),
        InvocationHandler { _, method, args ->
            when (method.name) {
                "mark" -> {
                    events.add("mark")
                    marker()
                }

                "eof" -> {
                    events.add("eof:$advanced")
                    advanced
                }

                "advanceLexer" -> {
                    events.add("advanceLexer")
                    advanced = true
                    null
                }

                "getTreeBuilt" -> astNode()
                else -> defaultValue(method.returnType, args)
            }
        },
    ) as PsiBuilder

    private fun marker(): PsiBuilder.Marker =
        Proxy.newProxyInstance(
            PsiBuilder.Marker::class.java.classLoader,
            arrayOf(PsiBuilder.Marker::class.java),
            InvocationHandler { _, method, args ->
                when (method.name) {
                    "done" -> {
                        events.add("done:${args?.first()}")
                        null
                    }

                    else -> defaultValue(method.returnType, args)
                }
            },
        ) as PsiBuilder.Marker
}

private fun astNode(): ASTNode =
    Proxy.newProxyInstance(
        ASTNode::class.java.classLoader,
        arrayOf(ASTNode::class.java),
        InvocationHandler { _, method, args ->
            when (method.name) {
                "getElementType" -> TokenType.WHITE_SPACE
                else -> defaultValue(method.returnType, args)
            }
        },
    ) as ASTNode

private fun minimalPsiElement(): PsiElement =
    Proxy.newProxyInstance(
        PsiElement::class.java.classLoader,
        arrayOf(PsiElement::class.java),
        InvocationHandler { _, method, args -> defaultValue(method.returnType, args) },
    ) as PsiElement

private fun fileViewProvider(): FileViewProvider =
    Proxy.newProxyInstance(
        FileViewProvider::class.java.classLoader,
        arrayOf(FileViewProvider::class.java),
        InvocationHandler { _, method, args ->
            when (method.name) {
                "getBaseLanguage" -> SpecDDLanguage
                "getLanguages" -> setOf(SpecDDLanguage)
                "getContents" -> "Spec:"
                "isPhysical" -> false
                else -> defaultValue(method.returnType, args)
            }
        },
    ) as FileViewProvider

private fun defaultValue(returnType: Class<*>, args: Array<Any?>?): Any? = when {
    Void.TYPE == returnType -> null
    java.lang.Boolean.TYPE == returnType -> false
    Integer.TYPE == returnType -> 0
    java.lang.Long.TYPE == returnType -> 0L
    IElementType::class.java == returnType -> TokenType.WHITE_SPACE
    PsiElement::class.java.isAssignableFrom(returnType) -> null
    else -> args?.firstOrNull()
}

private fun uninitializedSpecDDPsiFile(): SpecDDPsiFile {
    val unsafeField = Unsafe::class.java.getDeclaredField("theUnsafe")
    unsafeField.isAccessible = true
    return (unsafeField.get(null) as Unsafe).allocateInstance(SpecDDPsiFile::class.java) as SpecDDPsiFile
}
