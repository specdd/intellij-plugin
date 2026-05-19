package ai.specdd.idea

import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.VirtualFileListener
import com.intellij.openapi.vfs.VirtualFileSystem
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.io.OutputStream

internal class TestVirtualFile(
    private val fileName: String,
    private val directory: Boolean,
    private val parentFile: TestVirtualFile? = null,
) : VirtualFile() {
    private val childFiles = linkedMapOf<String, TestVirtualFile>()
    private val contents = ByteArrayOutputStream()

    override fun getName(): String = fileName

    override fun getFileSystem(): VirtualFileSystem = TEST_VIRTUAL_FILE_SYSTEM

    override fun getPath(): String =
        parentFile?.let { parent -> "${parent.path}/$fileName" } ?: "/$fileName"

    override fun isWritable(): Boolean = true

    override fun isDirectory(): Boolean = directory

    override fun isValid(): Boolean = true

    override fun getParent(): VirtualFile? = parentFile

    override fun getChildren(): Array<VirtualFile> = childFiles.values.toTypedArray()

    override fun findChild(name: String): VirtualFile? = childFiles[name]

    override fun createChildDirectory(requestor: Any?, name: String): VirtualFile =
        childFiles.getOrPut(name) { TestVirtualFile(name, true, this) }

    override fun createChildData(requestor: Any?, name: String): VirtualFile =
        childFiles.getOrPut(name) { TestVirtualFile(name, false, this) }

    override fun getOutputStream(requestor: Any?, newModificationStamp: Long, newTimeStamp: Long): OutputStream =
        contents

    override fun contentsToByteArray(): ByteArray = contents.toByteArray()

    override fun getTimeStamp(): Long = 0L

    override fun getLength(): Long = contents.size().toLong()

    override fun refresh(asynchronous: Boolean, recursive: Boolean, postRunnable: Runnable?) {
        postRunnable?.run()
    }

    override fun getInputStream(): InputStream = ByteArrayInputStream(contents.toByteArray())
}

internal fun testVirtualRoot(name: String = "project"): TestVirtualFile =
    TestVirtualFile(name, true)

internal fun TestVirtualFile.directory(path: String): TestVirtualFile {
    var current = this
    path.split('/').filter { segment -> segment.isNotEmpty() }.forEach { segment ->
        current = current.findChild(segment) as? TestVirtualFile
            ?: current.createChildDirectory(this, segment) as TestVirtualFile
    }
    return current
}

internal fun TestVirtualFile.file(path: String): TestVirtualFile {
    val parts = path.split('/').filter { segment -> segment.isNotEmpty() }
    val parent = parts.dropLast(1).fold(this) { current, segment -> current.directory(segment) }
    return parent.createChildData(this, parts.last()) as TestVirtualFile
}

internal fun TestVirtualFile.exists(path: String): Boolean =
    resolve(path) != null

internal fun TestVirtualFile.resolve(path: String): TestVirtualFile? {
    var current: VirtualFile = this
    path.split('/').filter { segment -> segment.isNotEmpty() }.forEach { segment ->
        current = current.findChild(segment) ?: return null
    }
    return current as? TestVirtualFile
}

private val TEST_VIRTUAL_FILE_SYSTEM: VirtualFileSystem =
    object : VirtualFileSystem() {
        override fun getProtocol(): String = "specdd-test"

        override fun isReadOnly(): Boolean = false

        override fun findFileByPath(path: String): VirtualFile? = null

        override fun refresh(asynchronous: Boolean) = Unit

        override fun refreshAndFindFileByPath(path: String): VirtualFile? = null

        override fun addVirtualFileListener(listener: VirtualFileListener) = Unit

        override fun removeVirtualFileListener(listener: VirtualFileListener) = Unit

        override fun deleteFile(requestor: Any?, file: VirtualFile) = Unit

        override fun moveFile(requestor: Any?, file: VirtualFile, newParent: VirtualFile) = Unit

        override fun renameFile(requestor: Any?, file: VirtualFile, newName: String) = Unit

        override fun createChildFile(requestor: Any?, file: VirtualFile, fileName: String): VirtualFile =
            file.createChildData(requestor, fileName)

        override fun createChildDirectory(requestor: Any?, file: VirtualFile, dirName: String): VirtualFile =
            file.createChildDirectory(requestor, dirName)

        override fun copyFile(
            requestor: Any?,
            virtualFile: VirtualFile,
            newParent: VirtualFile,
            copyName: String,
        ): VirtualFile =
            newParent.createChildData(requestor, copyName)
    }
