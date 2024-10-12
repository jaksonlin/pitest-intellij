package com.github.jaksonlin.pitestintellij.ui

import com.github.jaksonlin.pitestintellij.components.ObservableTree
import com.github.jaksonlin.pitestintellij.viewmodels.MutationToolWindowViewModel
import com.intellij.openapi.project.Project
import javax.swing.JButton
import javax.swing.JPanel
import javax.swing.JTextField
import javax.swing.tree.DefaultMutableTreeNode
import com.github.jaksonlin.pitestintellij.MyBundle
import java.awt.*

class MutationToolWindowUI(
    project: Project)
{
    private val clearButton = JButton(MyBundle.message("clear.button"))
    private val searchInput = JTextField(20)
    private val resultTree = ObservableTree()
    private val vm = MutationToolWindowViewModel(project, resultTree)


    init {
        registerListener()
        // set the placeholder text
        searchInput.toolTipText = MyBundle.message("search.placeholder")
    }

    private fun registerListener() {

        clearButton.addActionListener {
            vm.handleTreeClear()
        }

        searchInput.addActionListener {
            val searchText = searchInput.text
            if (searchText.isNotEmpty()) {
                val rootNode = resultTree.model.root as DefaultMutableTreeNode
                val path = vm.handleSearchInTree(searchText, rootNode)
                if (path != null) {
                    resultTree.scrollPathToVisible(path)
                    resultTree.selectionPath = path
                    resultTree.requestFocusInWindow() // release focus from searchInput so that the key enter event can be captured by the tree
                }
            }
        }

        resultTree.addMouseListener(object : java.awt.event.MouseAdapter() {
            override fun mouseClicked(e: java.awt.event.MouseEvent) {
                if (e.clickCount == 2) {
                    val selectedNode = resultTree.lastSelectedPathComponent as DefaultMutableTreeNode
                    vm.handleOpenSelectedNode(selectedNode)
                }
            }
        })

        // handle key enter event, when user press enter key, open the selected node
        resultTree.addKeyListener(object : java.awt.event.KeyAdapter() {
            override fun keyPressed(e: java.awt.event.KeyEvent) {
                if (e.keyCode == java.awt.event.KeyEvent.VK_ENTER) {
                    val selectedNode = resultTree.lastSelectedPathComponent as? DefaultMutableTreeNode
                    if (selectedNode != null) {
                        vm.handleOpenSelectedNode(selectedNode)
                    }
                }
            }
        })
    }

    private fun createControlPanel():JPanel{
        val controlPanel = JPanel(GridBagLayout())
        val gbc = GridBagConstraints().apply {
            fill = GridBagConstraints.HORIZONTAL
            gridx = 0
            gridy = 0
            weightx = 0.0
        }
        controlPanel.add(clearButton, gbc)

        gbc.apply {
            gridx = 1
            weightx = 1.0
        }
       controlPanel.add(searchInput, gbc)

        return controlPanel
    }

    private fun createToolWindowPanel(): JPanel {
        val panel = JPanel(BorderLayout())
        val controlPanel = createControlPanel()
        panel.add(controlPanel, BorderLayout.NORTH)
        panel.add(resultTree, BorderLayout.CENTER)
        return panel
    }

    fun getPanel(): JPanel {
        return createToolWindowPanel()
    }

}