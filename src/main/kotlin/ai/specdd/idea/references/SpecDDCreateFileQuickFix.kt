package ai.specdd.idea.references

import com.intellij.codeInsight.intention.IntentionAction
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.psi.PsiFile
import java.nio.file.Files
import java.nio.file.Path

internal class SpecDDCreateFileQuickFix(
    private val displayPath: String,
    private val targetPath: Path,
    private val projectRoot: Path,
    private val targetKind: SpecDDCreatePathKind,
) : IntentionAction {
    override fun getText(): String = "Create ${targetKind.label} '$displayPath'"

    override fun getFamilyName(): String = "Create SpecDD referenced path"

    override fun isAvailable(project: Project, editor: Editor?, file: PsiFile?): Boolean =
        canCreatePath(targetPath, projectRoot)

    override fun invoke(project: Project, editor: Editor?, file: PsiFile?) {
        if (!canCreatePath(targetPath, projectRoot)) return

        val application = ApplicationManager.getApplication()
        var actionExecuted = false
        application.runWriteAction {
            createPathAndRefresh()
            actionExecuted = true
        }
        if (!actionExecuted) {
            createPathAndRefresh()
        }
    }

    private fun createPathAndRefresh() {
        createPath()
        try {
            if (null != ApplicationManager.getApplication()) {
                LocalFileSystem.getInstance().refreshAndFindFileByNioFile(targetPath)
            }
        } catch (_: IllegalStateException) {
            return
        }
    }

    override fun startInWriteAction(): Boolean = false

    private fun createPath() {
        if (SpecDDCreatePathKind.DIRECTORY == targetKind) {
            Files.createDirectories(targetPath)
            return
        }

        val parent = targetPath.parent
        if (null != parent) {
            Files.createDirectories(parent)
        }
        Files.createFile(targetPath)
    }
}

internal fun createFileQuickFix(
    candidate: SpecDDPathCandidate,
    context: SpecDDPathResolutionContext,
): SpecDDCreateFileQuickFix? {
    if (candidate.text.any { character -> character in GLOB_CHARS }) return null

    val projectRoot = context.projectRoot.toAbsolutePath().normalize()
    val specDirectory = context.specDirectory.toAbsolutePath().normalize()
    if (!specDirectory.startsWith(projectRoot)) return null

    val targetPath = specDirectory.resolve(candidate.text).normalize()
    if (!canCreatePath(targetPath, projectRoot)) return null

    return SpecDDCreateFileQuickFix(candidate.text, targetPath, projectRoot, createPathKind(candidate.text))
}

private fun canCreatePath(targetPath: Path, projectRoot: Path): Boolean {
    val target = targetPath.toAbsolutePath().normalize()
    val root = projectRoot.toAbsolutePath().normalize()
    if (!target.startsWith(root)) return false
    if (Files.exists(target)) return false

    val parent = target.parent ?: return false
    return parent.startsWith(root)
}

private fun createPathKind(candidateText: String): SpecDDCreatePathKind {
    val normalizedText = candidateText.trimEnd('/')
    val name = Path.of(normalizedText).fileName?.toString() ?: return SpecDDCreatePathKind.DIRECTORY
    if (!name.contains(".")) return SpecDDCreatePathKind.DIRECTORY
    return SpecDDCreatePathKind.FILE
}

internal enum class SpecDDCreatePathKind(val label: String) {
    FILE("file"),
    DIRECTORY("directory"),
}
