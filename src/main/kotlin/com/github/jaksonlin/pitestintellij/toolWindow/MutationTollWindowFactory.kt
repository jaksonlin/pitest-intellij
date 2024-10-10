package com.github.jaksonlin.pitestintellij.toolWindow
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory
import com.intellij.ui.components.JBList
import com.intellij.ui.content.ContentFactory
import javax.swing.JPanel
import com.intellij.openapi.project.Project
import com.github.jaksonlin.pitestintellij.util.Mutation
import com.github.jaksonlin.pitestintellij.util.MutationReportParser

class MutationToolWindowFactory : ToolWindowFactory {
    override fun createToolWindowContent(project: Project, toolWindow: ToolWindow) {
        val mutationList = JBList<String>() // List to display mutation-tested class files
        val contentFactory = ContentFactory.getInstance()
        val content = contentFactory.createContent(createToolWindowPanel(mutationList), "", false)
        toolWindow.contentManager.addContent(content)

        // Load mutation data and populate the list
        val mutations = loadMutations("path/to/mutation-report.xml")
        mutationList.setListData(mutations.map { it.mutatedClass ?: "Unknown Class" }.toTypedArray())

        // Add double-click listener
        mutationList.addListSelectionListener {
            val selectedClass = mutationList.selectedValue
            if (selectedClass != null) {
                openClassFileAndAnnotate(project, selectedClass, mutations)
            }
        }
    }

    private fun createToolWindowPanel(mutationList: JBList<String>): JPanel {
        val panel = JPanel()
        panel.add(mutationList)
        return panel
    }

    private fun loadMutations(filePath: String): List<Mutation> {
        return MutationReportParser.parseMutationsFromXml(filePath).mutations
    }

    private fun openClassFileAndAnnotate(project: Project, className: String, mutations: List<Mutation>) {
        // Logic to open the class file and annotate with icons
        // For each mutation, check if it survived and add an icon at the line number
    }
}