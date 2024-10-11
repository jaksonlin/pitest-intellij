package com.github.jaksonlin.pitestintellij.ui

import com.github.jaksonlin.pitestintellij.context.PitestContext
import com.github.jaksonlin.pitestintellij.context.RunHistoryManager
import com.github.jaksonlin.pitestintellij.mediator.MutationMediator
import com.github.jaksonlin.pitestintellij.mediator.MutationUI
import com.github.jaksonlin.pitestintellij.observer.RunHistoryObserver
import com.intellij.icons.AllIcons
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.markup.GutterIconRenderer
import com.intellij.openapi.editor.markup.MarkupModel
import com.intellij.openapi.editor.markup.RangeHighlighter
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.openapi.vfs.VirtualFile
import java.nio.file.Paths
import javax.swing.Icon
import javax.swing.tree.DefaultMutableTreeNode
import com.intellij.ui.treeStructure.Tree
import javax.swing.tree.DefaultTreeModel
import javax.swing.tree.TreePath

class MutationToolWindowUI(
    private val project: Project,
    private val mediator: MutationMediator,
    private val mutationTree: Tree
) : MutationUI, RunHistoryObserver {

    private var previouslySelectedClass: String? = null

    override fun updateUI(mutationClassFilePath:String, mutationTestResult: Map<Int, Pair<String, Boolean>>) {
        val virtualFile = openClassFile(mutationClassFilePath)
        if (virtualFile != null) {
            val fileEditor = FileEditorManager.getInstance(project).getSelectedEditor(virtualFile)
            if (fileEditor is com.intellij.openapi.fileEditor.TextEditor) {
                val editor = fileEditor.editor
                addMutationMarkers(editor, mutationTestResult)
            }
        }
    }

    override fun onRunHistoryChanged(eventObj:Any?) {
        if (eventObj == null) {
            initializeMutationTree()
            return
        }
        if (eventObj is PitestContext){
            updateMutationTree(eventObj)
            return
        }
    }

    fun initializeMutationTree() {
        val treeModel = buildTreeModel()
        mutationTree.model = treeModel
    }

    private fun updateMutationTree(context:PitestContext) {
        updateMutationTree(mutationTree, context)

    }

    fun addDoubleClickListener() {
        mutationTree.addMouseListener(object : java.awt.event.MouseAdapter() {
            override fun mouseClicked(e: java.awt.event.MouseEvent) {
                if (e.clickCount == 2) {
                    val selectedNode = mutationTree.lastSelectedPathComponent as? DefaultMutableTreeNode ?: return
                    val treePath = selectedNode.path
                    val selectedClass = treePath.drop(1).joinToString(".") { it.toString() } // drop the root node and join the rest
                    val context = RunHistoryManager.getRunHistoryForClass(selectedClass) ?: return // if for any reason the class is not in the history, we do nothing
                    if (previouslySelectedClass == selectedClass && isEditorOpen(selectedClass)) {
                        return
                    }
                    openClassFileAndAnnotate(context)
                    previouslySelectedClass = selectedClass
                }
            }
        })
    }

    private fun isEditorOpen(className: String): Boolean {
        val fileEditorManager = FileEditorManager.getInstance(project)
        val openFiles = fileEditorManager.openFiles
        val fileNameToCheck = RunHistoryManager.getRunHistoryForClass(className)?.targetClassFilePath ?: return false
        val virtualFile = LocalFileSystem.getInstance().findFileByPath(fileNameToCheck) ?: return false
        return openFiles.any { it.path.contains(virtualFile.path) }
    }

    private fun openClassFileAndAnnotate(context: PitestContext) {
        val xmlReport = Paths.get(context.reportDirectory!!, "mutations.xml").toString()
        mediator.processMutations(context.targetClassFilePath!!, xmlReport)
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