package com.github.jaksonlin.pitestintellij.toolWindow

import com.github.jaksonlin.pitestintellij.context.RunHistoryManager
import com.github.jaksonlin.pitestintellij.mediator.MutationMediatorImpl
import com.github.jaksonlin.pitestintellij.ui.MutationToolWindowUI
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory
import com.intellij.ui.components.JBList
import com.intellij.ui.content.ContentFactory
import javax.swing.JPanel
import com.intellij.openapi.project.Project

class MutationToolWindowFactory : ToolWindowFactory {

    override fun createToolWindowContent(project: Project, toolWindow: ToolWindow) {
        val mediator = MutationMediatorImpl()
        val mutationList = JBList<String>()
        // standard code to create tool window content
        val contentFactory = ContentFactory.getInstance()
        val content = contentFactory.createContent(createToolWindowPanel(mutationList), "", false)
        toolWindow.contentManager.addContent(content)

        val toolWindowUI = MutationToolWindowUI(project, mediator, mutationList)
        mediator.registerUI(toolWindowUI)
        RunHistoryManager.addObserver(toolWindowUI)
        toolWindowUI.updateMutationList()
        toolWindowUI.addDoubleClickListener()
    }

    private fun createToolWindowPanel(mutationList: JBList<String>): JPanel {
        val panel = JPanel()
        panel.add(mutationList)
        return panel
    }
}

