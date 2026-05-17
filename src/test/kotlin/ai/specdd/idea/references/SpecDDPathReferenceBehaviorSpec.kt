package ai.specdd.idea.references

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
import kotlin.io.path.createFile
import kotlin.io.path.createTempDirectory

class SpecDDPathReferenceBehaviorSpec : BehaviorSpec({
    given("a SpecDD path reference") {
        `when`("a candidate resolves") {
            then("it returns mapped PSI resolve results") {
                val root = createTempDirectory()
                val target = root.resolve("main.sdd").createFile()
                val targetElement = psiElement("target")
                val reference = SpecDDPathReference(
                    element = psiElement("main.sdd"),
                    rangeInElement = TextRange(0, 8),
                    candidate = SpecDDPathCandidate("main.sdd", TextRange(0, 8), true),
                    context = SpecDDPathResolutionContext(root, root),
                    targetMapper = { path, _ -> if (target == path) targetElement else null },
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
                val root = createTempDirectory()
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

        `when`("the default target mapper cannot find a virtual file") {
            then("it returns no PSI element") {
                val root = createTempDirectory()
                val previousApplication = ApplicationManager.getApplication()
                ApplicationManager.setApplication(applicationWithLocalFileSystem())

                try {
                    pathToPsiElement(root.resolve("missing.sdd"), psiElement("missing.sdd")).shouldBeNull()
                } finally {
                    ApplicationManager.setApplication(previousApplication)
                }
            }
        }

        `when`("the default target mapper finds a directory virtual file") {
            then("it returns the mapped PSI directory") {
                val targetDirectory = psiDirectory("docs")
                val psiManager = fakePsiManager(directory = targetDirectory, file = null)
                val project = projectWithPsiManager(psiManager)
                psiManager.projectRef = project

                val resolved = pathToPsiElement(
                    Path.of("docs"),
                    psiElement("docs", project),
                    findVirtualFile = { fakeVirtualFile(directory = true) },
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

                val resolved = pathToPsiElement(
                    Path.of("docs/app.sdd"),
                    psiElement("docs/app.sdd", project),
                    findVirtualFile = { fakeVirtualFile(directory = false) },
                )

                (resolved === targetFile) shouldBe true
            }
        }

        `when`("the default target mapper is used for an existing path that is not in VFS") {
            then("it returns no resolve results") {
                val root = createTempDirectory()
                root.resolve("main.sdd").createFile()
                val previousApplication = ApplicationManager.getApplication()
                ApplicationManager.setApplication(applicationWithLocalFileSystem())
                val reference = SpecDDPathReference(
                    element = psiElement("main.sdd"),
                    rangeInElement = TextRange(0, 8),
                    candidate = SpecDDPathCandidate("main.sdd", TextRange(0, 8), true),
                    context = SpecDDPathResolutionContext(root, root),
                )

                try {
                    reference.multiResolve(false) shouldBe emptyArray()
                } finally {
                    ApplicationManager.setApplication(previousApplication)
                }
            }
        }
    }
})

internal fun psiElement(text: String, project: Project? = null): PsiElement =
    Proxy.newProxyInstance(
        PsiElement::class.java.classLoader,
        arrayOf(PsiElement::class.java),
        InvocationHandler { _, method, _ ->
            when (method.name) {
                "toString" -> "PsiElement($text)"
                "getProject" -> project
                "getText" -> text
                "getTextRange" -> TextRange(0, text.length)
                "isValid" -> true
                else -> {
                    if (Boolean::class.javaPrimitiveType == method.returnType) false else null
                }
            }
        },
    ) as PsiElement

private fun psiDirectory(text: String): PsiDirectory =
    Proxy.newProxyInstance(
        PsiDirectory::class.java.classLoader,
        arrayOf(PsiDirectory::class.java),
        InvocationHandler { _, method, _ ->
            when (method.name) {
                "toString" -> "PsiDirectory($text)"
                "getName" -> text
                "isValid" -> true
                else -> {
                    if (Boolean::class.javaPrimitiveType == method.returnType) false else null
                }
            }
        },
    ) as PsiDirectory

private fun psiFile(text: String): PsiFile =
    Proxy.newProxyInstance(
        PsiFile::class.java.classLoader,
        arrayOf(PsiFile::class.java),
        InvocationHandler { _, method, _ ->
            when (method.name) {
                "toString" -> "PsiFile($text)"
                "getName" -> text
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
