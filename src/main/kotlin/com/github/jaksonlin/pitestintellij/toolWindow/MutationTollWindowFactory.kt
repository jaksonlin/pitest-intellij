package com.github.jaksonlin.pitestintellij.toolWindow

import com.github.jaksonlin.pitestintellij.context.RunHistoryManager
import com.github.jaksonlin.pitestintellij.mediator.MutationMediatorImpl
import com.github.jaksonlin.pitestintellij.ui.MutationToolWindowUI
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory
import com.intellij.ui.content.ContentFactory
import javax.swing.JPanel
import com.intellij.openapi.project.Project
import com. intellij.ui.treeStructure.Tree
import java.awt.BorderLayout

class MutationToolWindowFactory : ToolWindowFactory {

    override fun createToolWindowContent(project: Project, toolWindow: ToolWindow) {
        val mediator = MutationMediatorImpl()
        val mutationTree = Tree()
        // standard code to create tool window content
        val contentFactory = ContentFactory.getInstance()
        val content = contentFactory.createContent(createToolWindowPanel(mutationTree), "", false)
        toolWindow.contentManager.addContent(content)

        val toolWindowUI = MutationToolWindowUI(project, mediator, mutationTree)
        mediator.registerUI(toolWindowUI)
        RunHistoryManager.addObserver(toolWindowUI)
        toolWindowUI.initializeMutationTree()
        toolWindowUI.addDoubleClickListener()
    }

    private fun createToolWindowPanel(mutationTree: Tree): JPanel {
        val panel = JPanel(BorderLayout())
        panel.add(mutationTree, BorderLayout.NORTH)
        return panel
    }
}

