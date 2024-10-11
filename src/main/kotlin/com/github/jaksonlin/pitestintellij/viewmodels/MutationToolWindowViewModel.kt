package com.github.jaksonlin.pitestintellij.viewmodels

import com.github.jaksonlin.pitestintellij.mediators.IMutationMediator
import com.github.jaksonlin.pitestintellij.mediators.MutationMediatorImpl
import com.github.jaksonlin.pitestintellij.services.RunHistoryManager
import com.github.jaksonlin.pitestintellij.components.ObservableTree
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import javax.swing.tree.DefaultMutableTreeNode
import javax.swing.tree.TreePath

class MutationToolWindowViewModel(
    project: Project,
    mutationTree: ObservableTree
) {
    private val runHistoryManager = service<RunHistoryManager>()
    private val mutationReportMediator:IMutationMediator = MutationMediatorImpl()
    private val mutationTreeMediatorVM = MutationTreeMediatorViewModel(project, mutationReportMediator)

    init {
        runHistoryManager.addObserver(mutationTree)
    }

    fun handleOpenSelectedNode(selectedNode:DefaultMutableTreeNode){
        mutationTreeMediatorVM.handleOpenSelectedNode(selectedNode)
    }

    fun handleTreeClear() {
        runHistoryManager.clearRunHistory()
    }

    fun handleSearchInTree(searchText: String, rootNode: DefaultMutableTreeNode):TreePath? {
        if (searchText.isEmpty()) {
            return null
        }
        val node = findNode(rootNode, searchText) ?: return null
        return TreePath(node.path)
    }

    private fun findNode(root: DefaultMutableTreeNode, searchText: String): DefaultMutableTreeNode? {
        val enumeration = root.depthFirstEnumeration()
        while (enumeration.hasMoreElements()) {
            val node = enumeration.nextElement() as DefaultMutableTreeNode
            if (node.userObject.toString().contains(searchText, ignoreCase = true)) {
                return node
            }
        }
        return null
    }

}