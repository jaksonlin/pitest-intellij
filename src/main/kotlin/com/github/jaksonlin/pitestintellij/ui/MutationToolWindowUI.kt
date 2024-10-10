package com.github.jaksonlin.pitestintellij.ui

import com.github.jaksonlin.pitestintellij.context.PitestContext
import com.github.jaksonlin.pitestintellij.context.RunHistoryManager
import com.github.jaksonlin.pitestintellij.mediator.MutationMediator
import com.github.jaksonlin.pitestintellij.mediator.MutationUI
import com.github.jaksonlin.pitestintellij.observer.RunHistoryObserver
import com.github.jaksonlin.pitestintellij.util.Mutation
import com.intellij.codeInsight.hint.HintManager
import com.intellij.icons.AllIcons
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.markup.GutterIconRenderer
import com.intellij.openapi.editor.markup.MarkupModel
import com.intellij.openapi.editor.markup.RangeHighlighter
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.ui.awt.RelativePoint
import com.intellij.ui.components.JBList
import com.intellij.util.ui.UIUtil
import java.awt.event.MouseEvent
import java.nio.file.Paths
import javax.swing.Icon
import javax.swing.JLabel
import javax.swing.JPanel

class MutationToolWindowUI(
    private val project: Project,
    private val mediator: MutationMediator,
    private val mutationList: JBList<String>
) : MutationUI, RunHistoryObserver {

    private var previouslySelectedClass: String? = null

    override fun updateUI(mutationClassFilePath:String, mutations: List<Mutation>) {
        val virtualFile = openClassFile(mutationClassFilePath)
        if (virtualFile != null) {
            val fileEditor = FileEditorManager.getInstance(project).getSelectedEditor(virtualFile)
            if (fileEditor is com.intellij.openapi.fileEditor.TextEditor) {
                val editor = fileEditor.editor
                addMutationMarkers(editor, mutations)
            }
        }
    }

    override fun onRunHistoryChanged() {
        updateMutationList()
    }

    fun updateMutationList() {
        val mutations = RunHistoryManager.getRunHistory()
        mutationList.setListData(mutations.map { it.key }.toTypedArray())
    }

    fun addDoubleClickListener() {
        mutationList.addMouseListener(object : java.awt.event.MouseAdapter() {
            override fun mouseClicked(e: java.awt.event.MouseEvent) {
                if (e.clickCount == 2) {
                    val selectedClass = mutationList.selectedValue ?: return
                    if (!isEditorOpen(selectedClass)) {
                        openClassFileAndAnnotate(RunHistoryManager.getRunHistoryForClass(selectedClass)!!)
                        previouslySelectedClass = selectedClass
                    } else {
                        if (selectedClass != previouslySelectedClass) {
                            previouslySelectedClass = selectedClass
                            openClassFileAndAnnotate(RunHistoryManager.getRunHistoryForClass(selectedClass)!!)
                        }
                    }
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

    private fun addMutationMarkers(editor: Editor, mutations: List<Mutation>) {
        val markupModel: MarkupModel = editor.markupModel
        val icon = AllIcons.General.Balloon

        for (mutation in mutations) {
            if (mutation.status != "KILLED") {
                val line = mutation.lineNumber - 1
                val highlighter: RangeHighlighter = markupModel.addLineHighlighter(line, 0, null)

                highlighter.gutterIconRenderer = object : GutterIconRenderer() {
                    override fun getIcon(): Icon = icon
                    override fun getTooltipText(): String = mutation.description!!
                    override fun isNavigateAction(): Boolean = true
                    fun navigate(e: MouseEvent) {
                        UIUtil.invokeLaterIfNeeded {
                            val point = RelativePoint(e)
                            showTooltip(point, mutation.description!!)
                        }
                    }
                    override fun equals(other: Any?): Boolean {
                        return other is GutterIconRenderer && other.icon == icon
                    }
                    override fun hashCode(): Int {
                        return icon.hashCode()
                    }
                }
            }
        }
    }

    private fun showTooltip(point: RelativePoint, text: String) {
        val label = JLabel(text)
        val panel = JPanel()
        panel.add(label)
        HintManager.getInstance().showHint(panel, point, HintManager.HIDE_BY_ANY_KEY or HintManager.HIDE_BY_TEXT_CHANGE or HintManager.HIDE_BY_SCROLLING, 0)
    }
}