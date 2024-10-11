package com.github.jaksonlin.pitestintellij.toolWindow

import com.github.jaksonlin.pitestintellij.services.RunHistoryManager
import com.github.jaksonlin.pitestintellij.mediators.MutationMediatorImpl
import com.github.jaksonlin.pitestintellij.ui.MutationToolWindowUI
import com.intellij.openapi.components.service
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory
import com.intellij.ui.content.ContentFactory
import com.intellij.openapi.project.Project
import com. intellij.ui.treeStructure.Tree
import javax.swing.JPanel

class MutationToolWindowFactory : ToolWindowFactory {

    override fun createToolWindowContent(project: Project, toolWindow: ToolWindow) {
        val contentFactory = ContentFactory.getInstance()
            val content = contentFactory.createContent(createToolWindowPanel(project), "", false)
        toolWindow.contentManager.addContent(content)
    }

    private fun createToolWindowPanel(project:Project): JPanel {
        val mutationToolWindowUI = MutationToolWindowUI(project)
        return mutationToolWindowUI.getPanel()
    }
}

