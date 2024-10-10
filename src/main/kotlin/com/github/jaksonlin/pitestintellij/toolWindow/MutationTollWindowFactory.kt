package com.github.jaksonlin.pitestintellij.toolWindow
import com.github.jaksonlin.pitestintellij.context.PitestContext
import com.github.jaksonlin.pitestintellij.context.RunHistoryManager
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory
import com.intellij.ui.components.JBList
import com.intellij.ui.content.ContentFactory
import javax.swing.JPanel
import com.intellij.openapi.project.Project
import com.github.jaksonlin.pitestintellij.util.Mutation
import com.github.jaksonlin.pitestintellij.util.MutationReportParser
import com.intellij.codeInsight.hint.HintManager
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.markup.GutterIconRenderer
import com.intellij.openapi.editor.markup.MarkupModel
import com.intellij.openapi.editor.markup.RangeHighlighter
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.ui.awt.RelativePoint
import com.intellij.util.ui.UIUtil
import java.awt.event.MouseEvent
import java.nio.file.Paths
import javax.swing.Icon
import javax.swing.ImageIcon
import com.intellij.icons.AllIcons
import javax.swing.JLabel

class MutationToolWindowFactory : ToolWindowFactory {
    private var previouslySelectedClass: String? = null
    override fun createToolWindowContent(project: Project, toolWindow: ToolWindow) {
        val mutationList = JBList<String>() // List to display mutation-tested class files
        val contentFactory = ContentFactory.getInstance()
        val content = contentFactory.createContent(createToolWindowPanel(mutationList), "", false)
        toolWindow.contentManager.addContent(content)

        // Load mutation data and populate the list
        val mutations = RunHistoryManager.getRunHistory()
        mutationList.setListData(mutations.map { it.key }.toTypedArray())



        // Add double-click listener
        mutationList.addListSelectionListener {
            val selectedClass = mutationList.selectedValue
            if (selectedClass != null && selectedClass != previouslySelectedClass) {
                previouslySelectedClass = selectedClass
                openClassFileAndAnnotate(project, selectedClass, mutations[selectedClass]!!)
            }
        }
    }

    private fun createToolWindowPanel(mutationList: JBList<String>): JPanel {
        val panel = JPanel()
        panel.add(mutationList)
        return panel
    }

    private fun loadMutations(filePath: String): List<Mutation> {
        return MutationReportParser.parseMutationsFromXml(filePath).mutation
    }

    private fun openClassFileAndAnnotate(project: Project, className: String, context: PitestContext) {
        // Logic to open the class file and annotate with icons
        // For each mutation, check if it survived and add an icon at the line number
        val xmlReport = Paths.get(context.reportDirectory!!, "mutations.xml").toString()
        val mutations = loadMutations(xmlReport)
        val targetClassFilePath = context.targetClassFilePath

        val virtualFile = openClassFile(project, targetClassFilePath!!)
        if (virtualFile != null) {
            val editor = FileEditorManager.getInstance(project).selectedTextEditor
            if (editor != null) {
                addMutationMarkers(editor, mutations)
            }
        }
    }

    private fun openClassFile(project: Project, filePath: String): VirtualFile? {
        val virtualFile = LocalFileSystem.getInstance().findFileByPath(filePath)
        if (virtualFile != null) {
            FileEditorManager.getInstance(project).openFile(virtualFile, true)
        }
        return virtualFile
    }

    private fun addMutationMarkers(editor: Editor, mutations: List<Mutation>) {
        val markupModel: MarkupModel = editor.markupModel
        val icon = AllIcons.General.Balloon // Example built-in icon

        for (mutation in mutations) {
            if (mutation.status == "SURVIVED") {
                val line = mutation.lineNumber - 1
                val highlighter: RangeHighlighter = markupModel.addLineHighlighter(line, 0, null)

                highlighter.gutterIconRenderer = object : GutterIconRenderer() {
                    override fun getIcon(): Icon = icon
                    override fun getTooltipText(): String = mutation.description!!
                    override fun isNavigateAction(): Boolean = true
                    fun navigate(e: MouseEvent) {
                        UIUtil.invokeLaterIfNeeded {
                            val point = RelativePoint(e)
                            // Show tooltip with mutation details
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