package com.github.jaksonlin.pitestintellij.ui

import com.github.jaksonlin.pitestintellij.context.PitestContext
import com.github.jaksonlin.pitestintellij.mediators.IMutationMediator
import com.github.jaksonlin.pitestintellij.services.RunHistoryManager
import com.intellij.openapi.components.service
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.ui.treeStructure.Tree
import java.awt.BorderLayout
import java.awt.FlowLayout
import java.nio.file.Paths
import javax.swing.JButton
import javax.swing.JPanel
import javax.swing.JTextField
import javax.swing.tree.DefaultMutableTreeNode
import javax.swing.tree.TreePath

class MutationToolWindowUI(
    project: Project,
    private val mediator: IMutationMediator)
{
    private val runHistoryManager = service<RunHistoryManager>()
    private val clearButton = JButton("Clear All History")
    private val searchInput = JTextField(20)
    private val mutationTree: Tree = Tree()

    private val mutationTreeUI : MutationTreeUI = MutationTreeUI(project, mediator, mutationTree, runHistoryManager)

    init {
        registerListener()
        initMutationTreeUI()
    }

    private fun initMutationTreeUI(){
        mediator.register(mutationTreeUI)
        runHistoryManager.addObserver(mutationTreeUI)
        mutationTreeUI.initializeMutationTree()
        mutationTreeUI.addDoubleClickListener()
    }

    private fun registerListener() {
        clearButton.addActionListener {
            runHistoryManager.clearRunHistory()
        }
        searchInput.addActionListener {
            val searchText = searchInput.text
            if (searchText.isNotEmpty()) {
                val root = mutationTree.model.root as DefaultMutableTreeNode
                val node = findNode(root, searchText)
                if (node != null) {
                    val path = TreePath(node.path)
                    mutationTree.scrollPathToVisible(path)
                    mutationTree.selectionPath = path
                }
            }
        }
    }

    private fun createControlPanel():JPanel{
        val controlPanel = JPanel(FlowLayout(FlowLayout.LEFT))
        controlPanel.add(clearButton)
        controlPanel.add(searchInput)
        return controlPanel
    }

    private fun createToolWindowPanel(): JPanel {
        val panel = JPanel(BorderLayout())
        val controlPanel = createControlPanel()
        panel.add(controlPanel, BorderLayout.NORTH)
        panel.add(mutationTree, BorderLayout.CENTER)
        return panel
    }

    fun getPanel(): JPanel {
        return createToolWindowPanel()
    }

}