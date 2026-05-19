package ai.specdd.idea.references

import com.intellij.codeInsight.intention.IntentionAction
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiFile

internal class SpecDDCreateFileQuickFix(
    private val displayPath: String,
    private val projectRoot: VirtualFile,
    private val targetSegments: List<String>,
    private val targetKind: SpecDDCreatePathKind,
) : IntentionAction {
    override fun getText(): String = "Create ${targetKind.label} '$displayPath'"

    override fun getFamilyName(): String = "Create SpecDD referenced path"

    override fun isAvailable(project: Project, editor: Editor?, file: PsiFile?): Boolean =
        canCreatePath(projectRoot, targetSegments)

    override fun invoke(project: Project, editor: Editor?, file: PsiFile?) {
        if (!canCreatePath(projectRoot, targetSegments)) return

        val application = ApplicationManager.getApplication()
        var actionExecuted = false
        application.runWriteAction {
            createPath()
            actionExecuted = true
        }
        if (!actionExecuted) {
            createPath()
        }
    }

    override fun startInWriteAction(): Boolean = false

    private fun createPath() {
        val finalName = targetSegments.lastOrNull() ?: return
        var directory = projectRoot
        targetSegments.dropLast(1).forEach { segment ->
            val child = directory.findChild(segment)
            directory = when {
                null == child -> directory.createChildDirectory(this, segment)
                child.isDirectory -> child
                else -> return
            }
        }

        if (null != directory.findChild(finalName)) return
        if (SpecDDCreatePathKind.DIRECTORY == targetKind) {
            directory.createChildDirectory(this, finalName)
            return
        }

        directory.createChildData(this, finalName)
    }
}

internal fun createFileQuickFix(
    candidate: SpecDDPathCandidate,
    context: SpecDDPathResolutionContext,
): SpecDDCreateFileQuickFix? {
    if (candidate.text.any { character -> character in GLOB_CHARS }) return null
    if (!isInRoot(context.specDirectory, context.projectRoot)) return null

    val targetSegments = targetSegments(context.projectRoot, context.specDirectory, candidate.text) ?: return null
    if (!canCreatePath(context.projectRoot, targetSegments)) return null

    return SpecDDCreateFileQuickFix(candidate.text, context.projectRoot, targetSegments, createPathKind(candidate.text))
}

private fun canCreatePath(projectRoot: VirtualFile, targetSegments: List<String>): Boolean {
    if (targetSegments.isEmpty()) return false
    if (null != findBySegments(projectRoot, targetSegments)) return false

    var current = projectRoot
    targetSegments.dropLast(1).forEach { segment ->
        val child = current.findChild(segment) ?: return true
        if (!child.isDirectory) return false
        current = child
    }
    return true
}

private fun createPathKind(candidateText: String): SpecDDCreatePathKind {
    val normalizedText = candidateText.trimEnd('/')
    val name = normalizedText.substringAfterLast('/', missingDelimiterValue = normalizedText)
    if (!name.contains(".")) return SpecDDCreatePathKind.DIRECTORY
    return SpecDDCreatePathKind.FILE
}

private fun targetSegments(projectRoot: VirtualFile, specDirectory: VirtualFile, text: String): List<String>? {
    val normalizedText = text.replace('\\', '/')
    val rootRelative = normalizedText.startsWith("/")
    val rootToSpec = if (rootRelative) "" else relativePath(projectRoot, specDirectory)
    val candidateText = when {
        normalizedText.startsWith("/") -> normalizedText.removePrefix("/")
        else -> normalizedText
    }
    val segments = if (rootToSpec.isBlank()) {
        mutableListOf()
    } else {
        rootToSpec.split('/').filterTo(mutableListOf()) { segment -> segment.isNotBlank() }
    }

    candidateText.split('/').forEach { segment ->
        when (segment) {
            "", "." -> Unit
            ".." -> if (segments.isNotEmpty()) segments.removeAt(segments.lastIndex) else return null
            else -> segments.add(segment)
        }
    }

    return segments
}

private fun findBySegments(projectRoot: VirtualFile, segments: List<String>): VirtualFile? {
    var current: VirtualFile = projectRoot
    segments.forEach { segment ->
        current = current.findChild(segment) ?: return null
    }
    return current
}

internal enum class SpecDDCreatePathKind(val label: String) {
    FILE("file"),
    DIRECTORY("directory"),
}
