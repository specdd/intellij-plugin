package ai.specdd.idea.references

import ai.specdd.idea.directory
import ai.specdd.idea.file
import ai.specdd.idea.testVirtualRoot
import com.intellij.codeInsight.multiverse.CodeInsightContext
import com.intellij.openapi.Disposable
import com.intellij.openapi.application.Application
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Computable
import com.intellij.openapi.util.TextRange
import com.intellij.openapi.util.io.FileAttributes
import com.intellij.openapi.vfs.*
import com.intellij.psi.*
import com.intellij.psi.util.PsiModificationTracker
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import java.io.*
import java.lang.reflect.InvocationHandler
import java.lang.reflect.Proxy
import java.nio.file.Path

class SpecDDPathReferenceBehaviorSpec : BehaviorSpec({
    given("a SpecDD path reference") {
        `when`("a candidate resolves") {
            then("it returns mapped PSI resolve results") {
                val root = testVirtualRoot()
                val target = root.file("main.sdd")
                val targetElement = psiElement("target")
                val reference = SpecDDPathReference(
                    element = psiElement("main.sdd"),
                    rangeInElement = TextRange(0, 8),
                    candidate = SpecDDPathCandidate("main.sdd", TextRange(0, 8), true),
                    context = SpecDDPathResolutionContext(root, root),
                    targetMapper = { virtualFile, _ -> if (target == virtualFile) targetElement else null },
                )

                reference.canonicalText shouldBe "main.sdd"
                reference.rangeInElement shouldBe TextRange(0, 8)
                reference.isSoft shouldBe true
                (reference.multiResolve(false).map { result -> result.element }
                    .single() === targetElement) shouldBe true
            }
        }

        `when`("a candidate has no resolution context") {
            then("it returns no resolve results") {
                val reference = SpecDDPathReference(
                    element = psiElement("missing.sdd"),
                    rangeInElement = TextRange(0, 11),
                    candidate = SpecDDPathCandidate("missing.sdd", TextRange(0, 11), true),
                    context = null,
                    targetMapper = { _, _ -> psiElement("unused") },
                )

                reference.multiResolve(false) shouldBe emptyArray()
            }
        }

        `when`("a candidate is unresolved") {
            then("it returns no resolve results") {
                val root = testVirtualRoot()
                val reference = SpecDDPathReference(
                    element = psiElement("missing.sdd"),
                    rangeInElement = TextRange(0, 11),
                    candidate = SpecDDPathCandidate("missing.sdd", TextRange(0, 11), true),
                    context = SpecDDPathResolutionContext(root, root),
                    targetMapper = { _, _ -> psiElement("unused") },
                )

                reference.multiResolve(false) shouldBe emptyArray()
            }
        }

        `when`("the default target mapper finds a directory virtual file") {
            then("it returns the mapped PSI directory") {
                val targetDirectory = psiDirectory("docs")
                val psiManager = fakePsiManager(directory = targetDirectory, file = null)
                val project = projectWithPsiManager(psiManager)
                psiManager.projectRef = project

                val resolved = virtualFileToPsiElement(
                    fakeVirtualFile(directory = true),
                    psiElement("docs", project),
                )

                (resolved === targetDirectory) shouldBe true
            }
        }

        `when`("the default target mapper finds a file virtual file") {
            then("it returns the mapped PSI file") {
                val targetFile = psiFile("docs/app.sdd")
                val psiManager = fakePsiManager(directory = null, file = targetFile)
                val project = projectWithPsiManager(psiManager)
                psiManager.projectRef = project

                val resolved = virtualFileToPsiElement(
                    fakeVirtualFile(directory = false),
                    psiElement("docs/app.sdd", project),
                )

                (resolved === targetFile) shouldBe true
            }
        }

        `when`("the default target mapper is used without a PSI manager") {
            then("it returns no resolve results") {
                val root = testVirtualRoot()
                root.file("main.sdd")
                val psiManager = fakePsiManager(directory = null, file = null)
                val project = projectWithPsiManager(psiManager)
                psiManager.projectRef = project
                val reference = SpecDDPathReference(
                    element = psiElement("main.sdd", project),
                    rangeInElement = TextRange(0, 8),
                    candidate = SpecDDPathCandidate("main.sdd", TextRange(0, 8), true),
                    context = SpecDDPathResolutionContext(root, root),
                )

                reference.multiResolve(false) shouldBe emptyArray()
            }
        }

        `when`("an exact path target is renamed") {
            then("it replaces only the final path segment") {
                val updatedElement = psiElement("updated")
                var changedText: String? = null
                val reference = SpecDDPathReference(
                    element = psiElement("./src/service.py"),
                    rangeInElement = TextRange(0, "./src/service.py".length),
                    candidate = SpecDDPathCandidate("./src/service.py", TextRange(0, "./src/service.py".length), true),
                    context = null,
                    referenceTextUpdater = { _, range, text ->
                        range shouldBe TextRange(0, "./src/service.py".length)
                        changedText = text
                        updatedElement
                    },
                )

                (reference.handleElementRename("invoice_service.py") === updatedElement) shouldBe true
                changedText shouldBe "./src/invoice_service.py"
                reference.participatesInAutomaticRename() shouldBe true
            }
        }

        `when`("a bare exact path target is renamed") {
            then("it replaces the whole path text with the new name") {
                var changedText: String? = null
                val reference = SpecDDPathReference(
                    element = psiElement("service.py"),
                    rangeInElement = TextRange(0, "service.py".length),
                    candidate = SpecDDPathCandidate("service.py", TextRange(0, "service.py".length), true),
                    context = null,
                    referenceTextUpdater = { sourceElement, _, text ->
                        changedText = text
                        sourceElement
                    },
                )

                reference.handleElementRename("invoice_service.py")

                changedText shouldBe "invoice_service.py"
            }
        }

        `when`("the default reference text updater has no document") {
            then("it leaves the reference element unchanged") {
                val element = psiElement("service.py")
                val reference = SpecDDPathReference(
                    element = element,
                    rangeInElement = TextRange(0, "service.py".length),
                    candidate = SpecDDPathCandidate("service.py", TextRange(0, "service.py".length), true),
                    context = null,
                )

                (reference.handleElementRename("invoice_service.py") === element) shouldBe true
            }
        }

        `when`("a glob target is renamed") {
            then("it leaves the reference unchanged") {
                val element = psiElement("./src/*.py")
                val reference = SpecDDPathReference(
                    element = element,
                    rangeInElement = TextRange(0, "./src/*.py".length),
                    candidate = SpecDDPathCandidate("./src/*.py", TextRange(0, "./src/*.py".length), true),
                    context = null,
                    referenceTextUpdater = { _, _, _ -> error("glob references must not be rewritten") },
                )

                (reference.handleElementRename("invoice.py") === element) shouldBe true
                reference.participatesInAutomaticRename() shouldBe false
            }
        }

        `when`("an exact project-root path is rebound to a moved file") {
            then("it recomputes the reference as a project-root path") {
                val root = testVirtualRoot()
                val specDirectory = root.directory("specs")
                val target = root.file("src/invoice_service.py")
                var changedText: String? = null
                val reference = SpecDDPathReference(
                    element = psiElement("/src/service.py"),
                    rangeInElement = TextRange(0, "/src/service.py".length),
                    candidate = SpecDDPathCandidate("/src/service.py", TextRange(0, "/src/service.py".length), true),
                    context = SpecDDPathResolutionContext(root, specDirectory),
                    referenceTextUpdater = { sourceElement, _, text ->
                        changedText = text
                        sourceElement
                    },
                )

                reference.bindToElement(psiFile("invoice_service.py", target))

                changedText shouldBe "/src/invoice_service.py"
            }
        }

        `when`("an exact spec-relative path is rebound to a moved descendant file") {
            then("it recomputes the reference with an explicit relative prefix") {
                val root = testVirtualRoot()
                val specDirectory = root.directory("specs")
                val target = specDirectory.file("impl/invoice_service.py")
                var changedText: String? = null
                val reference = SpecDDPathReference(
                    element = psiElement("./impl/service.py"),
                    rangeInElement = TextRange(0, "./impl/service.py".length),
                    candidate = SpecDDPathCandidate("./impl/service.py", TextRange(0, "./impl/service.py".length), true),
                    context = SpecDDPathResolutionContext(root, specDirectory),
                    referenceTextUpdater = { sourceElement, _, text ->
                        changedText = text
                        sourceElement
                    },
                )

                reference.bindToElement(psiFile("invoice_service.py", target))

                changedText shouldBe "./impl/invoice_service.py"
            }
        }

        `when`("an exact spec-relative path is rebound to a moved sibling-tree file") {
            then("it recomputes the reference with parent segments") {
                val root = testVirtualRoot()
                val specDirectory = root.directory("specs/domain")
                val target = root.file("shared/invoice_service.py")
                var changedText: String? = null
                val reference = SpecDDPathReference(
                    element = psiElement("../shared/service.py"),
                    rangeInElement = TextRange(0, "../shared/service.py".length),
                    candidate = SpecDDPathCandidate("../shared/service.py", TextRange(0, "../shared/service.py".length), true),
                    context = SpecDDPathResolutionContext(root, specDirectory),
                    referenceTextUpdater = { sourceElement, _, text ->
                        changedText = text
                        sourceElement
                    },
                )

                reference.bindToElement(psiFile("invoice_service.py", target))

                changedText shouldBe "../../shared/invoice_service.py"
            }
        }

        `when`("an exact unprefixed path is rebound") {
            then("it recomputes the reference without adding an explicit prefix") {
                val root = testVirtualRoot()
                val specDirectory = root.directory("specs")
                val target = specDirectory.file("impl/invoice_service.py")
                var changedText: String? = null
                val reference = SpecDDPathReference(
                    element = psiElement("impl/service.py"),
                    rangeInElement = TextRange(0, "impl/service.py".length),
                    candidate = SpecDDPathCandidate("impl/service.py", TextRange(0, "impl/service.py".length), true),
                    context = SpecDDPathResolutionContext(root, specDirectory),
                    referenceTextUpdater = { sourceElement, _, text ->
                        changedText = text
                        sourceElement
                    },
                )

                reference.bindToElement(psiFile("invoice_service.py", target))

                changedText shouldBe "impl/invoice_service.py"
            }
        }

        `when`("an exact path is rebound to a directory") {
            then("it uses the directory virtual file") {
                val root = testVirtualRoot()
                val target = root.directory("src/invoice_demo")
                var changedText: String? = null
                val reference = SpecDDPathReference(
                    element = psiElement("/src/service"),
                    rangeInElement = TextRange(0, "/src/service".length),
                    candidate = SpecDDPathCandidate("/src/service", TextRange(0, "/src/service".length), true),
                    context = SpecDDPathResolutionContext(root, root),
                    referenceTextUpdater = { sourceElement, _, text ->
                        changedText = text
                        sourceElement
                    },
                )

                reference.bindToElement(psiDirectory("invoice_demo", target))

                changedText shouldBe "/src/invoice_demo"
            }
        }

        `when`("an exact path is rebound to a navigation element") {
            then("it uses the navigation target virtual file") {
                val root = testVirtualRoot()
                val target = root.file("src/invoice_service.py")
                var changedText: String? = null
                val reference = SpecDDPathReference(
                    element = psiElement("/src/service.py"),
                    rangeInElement = TextRange(0, "/src/service.py".length),
                    candidate = SpecDDPathCandidate("/src/service.py", TextRange(0, "/src/service.py".length), true),
                    context = SpecDDPathResolutionContext(root, root),
                    referenceTextUpdater = { sourceElement, _, text ->
                        changedText = text
                        sourceElement
                    },
                )

                reference.bindToElement(psiElement("light", navigationElement = psiFile("invoice_service.py", target)))

                changedText shouldBe "/src/invoice_service.py"
            }
        }

        `when`("an exact path is rebound without enough context") {
            then("it leaves the reference unchanged") {
                val element = psiElement("./service.py")
                val root = testVirtualRoot()
                val outsideRoot = testVirtualRoot("outside")
                val referenceWithoutContext = SpecDDPathReference(
                    element = element,
                    rangeInElement = TextRange(0, "./service.py".length),
                    candidate = SpecDDPathCandidate("./service.py", TextRange(0, "./service.py".length), true),
                    context = null,
                    referenceTextUpdater = { _, _, _ -> error("missing context must not be rewritten") },
                )
                val referenceWithoutTarget = SpecDDPathReference(
                    element = element,
                    rangeInElement = TextRange(0, "./service.py".length),
                    candidate = SpecDDPathCandidate("./service.py", TextRange(0, "./service.py".length), true),
                    context = SpecDDPathResolutionContext(root, root),
                    referenceTextUpdater = { _, _, _ -> error("missing target must not be rewritten") },
                )
                val referenceOutsideRoot = SpecDDPathReference(
                    element = element,
                    rangeInElement = TextRange(0, "./service.py".length),
                    candidate = SpecDDPathCandidate("./service.py", TextRange(0, "./service.py".length), true),
                    context = SpecDDPathResolutionContext(root, root),
                    referenceTextUpdater = { _, _, _ -> error("outside root target must not be rewritten") },
                )
                val referenceOutsideProject = SpecDDPathReference(
                    element = element,
                    rangeInElement = TextRange(0, "./service.py".length),
                    candidate = SpecDDPathCandidate("./service.py", TextRange(0, "./service.py".length), true),
                    context = SpecDDPathResolutionContext(root, root) { false },
                    referenceTextUpdater = { _, _, _ -> error("outside project target must not be rewritten") },
                )

                (referenceWithoutContext.bindToElement(psiFile("service.py", root.file("service.py"))) === element) shouldBe true
                (referenceWithoutTarget.bindToElement(psiElement("target")) === element) shouldBe true
                (referenceOutsideRoot.bindToElement(psiFile("service.py", outsideRoot.file("service.py"))) === element) shouldBe true
                (referenceOutsideProject.bindToElement(psiFile("service.py", root.file("service.py"))) === element) shouldBe true
            }
        }

        `when`("a glob path is rebound") {
            then("it leaves the reference unchanged") {
                val root = testVirtualRoot()
                val target = root.file("src/invoice_service.py")
                val element = psiElement("./src/*.py")
                val reference = SpecDDPathReference(
                    element = element,
                    rangeInElement = TextRange(0, "./src/*.py".length),
                    candidate = SpecDDPathCandidate("./src/*.py", TextRange(0, "./src/*.py".length), true),
                    context = SpecDDPathResolutionContext(root, root),
                    referenceTextUpdater = { _, _, _ -> error("glob references must not be rewritten") },
                )

                (reference.bindToElement(psiFile("invoice_service.py", target)) === element) shouldBe true
            }
        }
    }
})

internal fun psiElement(text: String, project: Project? = null, navigationElement: PsiElement? = null): PsiElement =
    Proxy.newProxyInstance(
        PsiElement::class.java.classLoader,
        arrayOf(PsiElement::class.java),
        InvocationHandler { _, method, _ ->
            when (method.name) {
                "toString" -> "PsiElement($text)"
                "getProject" -> project
                "getText" -> text
                "getTextRange" -> TextRange(0, text.length)
                "getNavigationElement" -> navigationElement ?: throw UnsupportedOperationException("No navigation element")
                "isValid" -> true
                else -> {
                    if (Boolean::class.javaPrimitiveType == method.returnType) false else null
                }
            }
        },
    ) as PsiElement

private fun psiDirectory(text: String, virtualFile: VirtualFile? = null): PsiDirectory =
    Proxy.newProxyInstance(
        PsiDirectory::class.java.classLoader,
        arrayOf(PsiDirectory::class.java),
        InvocationHandler { _, method, _ ->
            when (method.name) {
                "toString" -> "PsiDirectory($text)"
                "getName" -> text
                "getVirtualFile" -> virtualFile
                "isValid" -> true
                else -> {
                    if (Boolean::class.javaPrimitiveType == method.returnType) false else null
                }
            }
        },
    ) as PsiDirectory

private fun psiFile(text: String, virtualFile: VirtualFile? = null): PsiFile =
    Proxy.newProxyInstance(
        PsiFile::class.java.classLoader,
        arrayOf(PsiFile::class.java),
        InvocationHandler { _, method, _ ->
            when (method.name) {
                "toString" -> "PsiFile($text)"
                "getName" -> text
                "getVirtualFile" -> virtualFile
                "isValid" -> true
                else -> {
                    if (Boolean::class.javaPrimitiveType == method.returnType) false else null
                }
            }
        },
    ) as PsiFile

private fun projectWithPsiManager(psiManager: PsiManager): Project =
    Proxy.newProxyInstance(
        Project::class.java.classLoader,
        arrayOf(Project::class.java),
        InvocationHandler { _, method, args ->
            when (method.name) {
                "toString" -> "Project(SpecDD test)"
                "getService" -> if (PsiManager::class.java == args?.firstOrNull()) psiManager else null
                "isDisposed" -> false
                "isInitialized" -> true
                "isOpen" -> true
                else -> {
                    if (Boolean::class.javaPrimitiveType == method.returnType) false else null
                }
            }
        },
    ) as Project

private fun fakePsiManager(directory: PsiDirectory?, file: PsiFile?): TestPsiManager =
    TestPsiManager(directory, file)

private class TestPsiManager(
    private val directory: PsiDirectory?,
    private val file: PsiFile?,
) : PsiManager() {
    lateinit var projectRef: Project

    override fun getProject(): Project = projectRef

    override fun findFile(file: VirtualFile): PsiFile? = this.file

    override fun findFile(file: VirtualFile, context: CodeInsightContext): PsiFile? = this.file

    override fun findDirectory(file: VirtualFile): PsiDirectory? = directory

    override fun findViewProvider(file: VirtualFile): FileViewProvider? = null

    override fun findViewProvider(file: VirtualFile, context: CodeInsightContext): FileViewProvider? = null

    override fun areElementsEquivalent(element1: PsiElement?, element2: PsiElement?): Boolean = element1 == element2

    override fun reloadFromDisk(file: PsiFile) = Unit

    override fun addPsiTreeChangeListener(listener: PsiTreeChangeListener) = Unit

    override fun addPsiTreeChangeListener(listener: PsiTreeChangeListener, parentDisposable: Disposable) = Unit

    override fun addPsiTreeChangeListenerBackgroundable(
        listener: PsiTreeChangeListener,
        parentDisposable: Disposable,
    ) = Unit

    override fun removePsiTreeChangeListener(listener: PsiTreeChangeListener) = Unit

    override fun getModificationTracker(): PsiModificationTracker = modificationTracker()

    override fun startBatchFilesProcessingMode() = Unit

    override fun finishBatchFilesProcessingMode() = Unit

    override fun <T : Any?> runInBatchFilesMode(runnable: Computable<T>): T = runnable.compute()

    override fun isDisposed(): Boolean = false

    override fun dropResolveCaches() = Unit

    override fun dropPsiCaches() = Unit

    override fun isInProject(element: PsiElement): Boolean = true

    override fun findCachedViewProvider(file: VirtualFile): FileViewProvider? = null
}

private fun fakeVirtualFile(directory: Boolean): VirtualFile =
    object : VirtualFile() {
        override fun getName(): String = if (directory) "docs" else "app.sdd"
        override fun getFileSystem(): VirtualFileSystem = nullLocalFileSystem()
        override fun getPath(): String = name
        override fun isWritable(): Boolean = true
        override fun isDirectory(): Boolean = directory
        override fun isValid(): Boolean = true
        override fun getParent(): VirtualFile? = null
        override fun getChildren(): Array<VirtualFile> = emptyArray()
        override fun getOutputStream(requestor: Any?, newModificationStamp: Long, newTimeStamp: Long): OutputStream =
            ByteArrayOutputStream()

        override fun contentsToByteArray(): ByteArray = ByteArray(0)
        override fun getTimeStamp(): Long = 0L
        override fun getLength(): Long = 0L
        override fun refresh(asynchronous: Boolean, recursive: Boolean, postRunnable: Runnable?) = Unit
        override fun getInputStream(): InputStream = ByteArrayInputStream(ByteArray(0))
    }

private fun modificationTracker(): PsiModificationTracker =
    Proxy.newProxyInstance(
        PsiModificationTracker::class.java.classLoader,
        arrayOf(PsiModificationTracker::class.java),
        InvocationHandler { _, method, _ ->
            when (method.name) {
                "getModificationCount", "getOutOfCodeBlockModificationCount" -> 0L
                else -> {
                    if (Boolean::class.javaPrimitiveType == method.returnType) false else null
                }
            }
        },
    ) as PsiModificationTracker

private fun applicationWithLocalFileSystem(): Application =
    Proxy.newProxyInstance(
        Application::class.java.classLoader,
        arrayOf(Application::class.java),
        InvocationHandler { _, method, args ->
            when (method.name) {
                "getService" -> when (args?.firstOrNull()) {
                    LocalFileSystem::class.java -> nullLocalFileSystem()
                    VirtualFileManager::class.java -> nullVirtualFileManager()
                    else -> null
                }

                "isUnitTestMode" -> true
                "isHeadlessEnvironment" -> true
                else -> {
                    if (Boolean::class.javaPrimitiveType == method.returnType) false else null
                }
            }
        },
    ) as Application

private fun nullVirtualFileManager(): VirtualFileManager =
    @Suppress("OVERRIDE_DEPRECATION")
    object : VirtualFileManager() {
        override fun getFileSystem(protocol: String?) = nullLocalFileSystem()
        override fun syncRefresh(): Long = 0L
        override fun asyncRefresh(postAction: Runnable?): Long = 0L
        override fun refreshWithoutFileWatcher(asynchronous: Boolean) = Unit
        override fun refreshAndFindFileByNioPath(path: Path): VirtualFile? = null
        override fun addVirtualFileListener(listener: VirtualFileListener) = Unit
        override fun addVirtualFileListener(
            listener: VirtualFileListener,
            parentDisposable: Disposable
        ) = Unit

        override fun removeVirtualFileListener(listener: VirtualFileListener) = Unit
        override fun addAsyncFileListener(
            listener: AsyncFileListener,
            parentDisposable: Disposable
        ) = Unit

        override fun addAsyncFileListenerBackgroundable(
            listener: AsyncFileListener,
            parentDisposable: Disposable
        ) = Unit

        override fun addAsyncFileListener(
            coroutineScope: kotlinx.coroutines.CoroutineScope,
            listener: AsyncFileListener
        ) = Unit

        override fun addVirtualFileManagerListener(
            listener: VirtualFileManagerListener,
            parentDisposable: Disposable
        ) = Unit

        override fun removeVirtualFileManagerListener(listener: VirtualFileManagerListener) = Unit
        override fun notifyPropertyChanged(file: VirtualFile, property: String, oldValue: Any?, newValue: Any?) = Unit
        override fun getModificationCount(): Long = 0L
        override fun getStructureModificationCount(): Long = 0L
        override fun storeName(name: String): Int = 0
        override fun getVFileName(nameId: Int): CharSequence = ""
    }

private fun nullLocalFileSystem(): LocalFileSystem =
    object : LocalFileSystem() {
        override fun getProtocol(): String = PROTOCOL
        override fun findFileByPath(path: String): VirtualFile? = null
        override fun refreshAndFindFileByPath(path: String): VirtualFile? = null
        override fun refreshAndFindFileByNioFile(file: Path): VirtualFile? = null
        override fun refresh(asynchronous: Boolean) = Unit
        override fun addVirtualFileListener(listener: VirtualFileListener) = Unit
        override fun removeVirtualFileListener(listener: VirtualFileListener) = Unit
        override fun refreshIoFiles(
            files: Iterable<File>,
            async: Boolean,
            recursive: Boolean,
            postRunnable: Runnable?
        ) = Unit

        override fun refreshNioFiles(
            files: Iterable<Path>,
            async: Boolean,
            recursive: Boolean,
            postRunnable: Runnable?
        ) = Unit

        override fun refreshFiles(
            files: Iterable<VirtualFile>,
            async: Boolean,
            recursive: Boolean,
            postRunnable: Runnable?
        ) = Unit

        override fun addRootToWatch(rootPath: String, watchRecursively: Boolean): WatchRequest? = null
        override fun addRootsToWatch(rootPaths: Collection<String>, watchRecursively: Boolean): Set<WatchRequest> =
            emptySet()

        override fun removeWatchedRoot(watchRequest: WatchRequest) = Unit
        override fun removeWatchedRoots(watchRequests: Collection<WatchRequest>) = Unit
        override fun replaceWatchedRoot(
            watchRequest: WatchRequest?,
            rootPath: String,
            watchRecursively: Boolean
        ): WatchRequest? = null

        override fun replaceWatchedRoots(
            watchRequests: Collection<WatchRequest>,
            recursiveRoots: Collection<String>?,
            flatRoots: Collection<String>?,
        ): Set<WatchRequest> = emptySet()

        override fun registerAuxiliaryFileOperationsHandler(handler: LocalFileOperationsHandler) = Unit
        override fun unregisterAuxiliaryFileOperationsHandler(handler: LocalFileOperationsHandler) = Unit
        override fun extractRootPath(path: String): String = path
        override fun findFileByPathIfCached(path: String): VirtualFile? = null
        override fun getRank(): Int = 0
        override fun getAttributes(file: VirtualFile): FileAttributes? = null
        override fun exists(file: VirtualFile): Boolean = false
        override fun list(file: VirtualFile): Array<String> = emptyArray()
        override fun isDirectory(file: VirtualFile): Boolean = false
        override fun getTimeStamp(file: VirtualFile): Long = 0L
        override fun setTimeStamp(file: VirtualFile, timeStamp: Long) = Unit
        override fun isWritable(file: VirtualFile): Boolean = false
        override fun setWritable(file: VirtualFile, writableFlag: Boolean) = Unit
        override fun contentsToByteArray(file: VirtualFile): ByteArray = ByteArray(0)
        override fun getInputStream(file: VirtualFile): InputStream = ByteArrayInputStream(ByteArray(0))
        override fun getOutputStream(
            file: VirtualFile,
            requestor: Any?,
            modStamp: Long,
            timeStamp: Long
        ): OutputStream =
            ByteArrayOutputStream()

        override fun getLength(file: VirtualFile): Long = 0L
        override fun copyFile(
            requestor: Any?,
            virtualFile: VirtualFile,
            newParent: VirtualFile,
            copyName: String
        ): VirtualFile =
            throw IOException("Unsupported")

        override fun createChildDirectory(requestor: Any?, virtualFile: VirtualFile, dirName: String): VirtualFile =
            throw IOException("Unsupported")

        override fun createChildFile(requestor: Any?, virtualFile: VirtualFile, fileName: String): VirtualFile =
            throw IOException("Unsupported")

        override fun deleteFile(requestor: Any?, virtualFile: VirtualFile) = Unit
        override fun moveFile(requestor: Any?, virtualFile: VirtualFile, newParent: VirtualFile) = Unit
        override fun renameFile(requestor: Any?, virtualFile: VirtualFile, newName: String) = Unit
    }
