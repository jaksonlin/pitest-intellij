package com.github.jaksonlin.pitestintellij.viewmodels

import com.github.jaksonlin.pitestintellij.context.PitestContext
import com.github.jaksonlin.pitestintellij.mediators.IMutationMediator
import com.github.jaksonlin.pitestintellij.mediators.IMutationReportUI
import com.github.jaksonlin.pitestintellij.services.RunHistoryManager
import com.intellij.icons.AllIcons
import com.intellij.openapi.components.service
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.markup.GutterIconRenderer
import com.intellij.openapi.editor.markup.MarkupModel
import com.intellij.openapi.editor.markup.RangeHighlighter
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.fileEditor.FileEditorManagerListener
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.openapi.vfs.VirtualFile
import java.nio.file.Paths
import javax.swing.Icon
import javax.swing.tree.DefaultMutableTreeNode

class MutationTreeMediatorViewModel(
    private val project: Project,
    private val mediator: IMutationMediator,
) : IMutationReportUI {
    private val runHistoryManager = service<RunHistoryManager>()
    private val annotatedNodes = HashMap<String, Unit>()


    init {
        mediator.register(this)
        registerEditorListener(project)
    }

    private fun registerEditorListener(project: Project) {
        project.messageBus.connect().subscribe(FileEditorManagerListener.FILE_EDITOR_MANAGER, object : FileEditorManagerListener {
            override fun fileClosed(source: FileEditorManager, file: VirtualFile) {
                // Remove the node from the annotatedNodes set when the editor is closed
                if (annotatedNodes.contains(file.path)) {
                    annotatedNodes.remove(file.path)
                }
            }
        })
    }

    override fun updateMutationResult(mutationClassFilePath:String, mutationTestResult: Map<Int, Pair<String, Boolean>>) {
        val virtualFile = openClassFile(mutationClassFilePath)
        if (virtualFile != null) {
            val fileEditor = FileEditorManager.getInstance(project).getSelectedEditor(virtualFile)
            if (fileEditor is com.intellij.openapi.fileEditor.TextEditor) {
                val editor = fileEditor.editor
                addMutationMarkers(editor, mutationTestResult)
            }
        }
    }

    fun handleOpenSelectedNode(selectedNode: DefaultMutableTreeNode) {
        val treePath = selectedNode.path
        val selectedClass = treePath.drop(1).joinToString(".") { it.toString() } // drop the root node and join the rest

        val context = runHistoryManager.getRunHistoryForClass(selectedClass) ?: return // if for any reason the class is not in the history, we do nothing
        if (annotatedNodes.contains(context.targetClassFilePath)) {
            switchToSelectedFile(context.targetClassFilePath!!)
            return
        }
        openClassFileAndAnnotate(context)
        annotatedNodes[context.targetClassFilePath!!] = Unit
    }

    private fun switchToSelectedFile(targetFilePath:String){
        val fileEditorManager = FileEditorManager.getInstance(project)
        val currentFile = fileEditorManager.selectedFiles.firstOrNull()
        if (currentFile == null || currentFile.path != targetFilePath) {
            val targetFile = LocalFileSystem.getInstance().findFileByPath(targetFilePath)
            if (targetFile != null){
                fileEditorManager.openFile(targetFile, true)
            }
        }
    }


    private fun isEditorOpen(className: String): Boolean {
        val fileEditorManager = FileEditorManager.getInstance(project)
        val openFiles = fileEditorManager.openFiles
        val fileNameToCheck = runHistoryManager.getRunHistoryForClass(className)?.targetClassFilePath ?: return false
        val virtualFile = LocalFileSystem.getInstance().findFileByPath(fileNameToCheck) ?: return false
        return openFiles.any { it.path.contains(virtualFile.path) }
    }

    private fun openClassFileAndAnnotate(context: PitestContext) {
        val xmlReport = Paths.get(context.reportDirectory!!, "mutations.xml").toString()
        mediator.processMutationResult(context.targetClassFilePath!!, xmlReport)
    }

    private fun openClassFile(filePath: String): VirtualFile? {
        val virtualFile = LocalFileSystem.getInstance().findFileByPath(filePath)
        if (virtualFile != null) {
            FileEditorManager.getInstance(project).openFile(virtualFile, true)
        }
        return virtualFile
    }

    private fun addMutationMarkers(editor: Editor, mutationTestResult: Map<Int, Pair<String, Boolean>>) {
        val markupModel: MarkupModel = editor.markupModel


        for ((lineNumber, mutationData) in mutationTestResult) {
            val (mutationDescription, allKilled) = mutationData
            val icon = when {
                allKilled -> AllIcons.General.InspectionsOK
                mutationDescription.contains("KILL") -> AllIcons.General.Warning
                else -> AllIcons.General.Error
            }

            val highlighter: RangeHighlighter = markupModel.addLineHighlighter(lineNumber - 1, 0, null)

            highlighter.gutterIconRenderer = object : GutterIconRenderer() {
                override fun getIcon(): Icon = icon
                override fun getTooltipText(): String = mutationDescription
                override fun isNavigateAction(): Boolean = true
                override fun equals(other: Any?): Boolean {
                    if (this === other) return true
                    if (other !is GutterIconRenderer) return false
                    return icon == other.icon && tooltipText == other.tooltipText
                }

                override fun hashCode(): Int {
                    var result = icon.hashCode()
                    result = 31 * result + (tooltipText.hashCode() ?: 0)
                    return result
                }
            }
        }
    }
}