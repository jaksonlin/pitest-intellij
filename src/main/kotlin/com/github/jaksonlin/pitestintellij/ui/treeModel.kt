package com.github.jaksonlin.pitestintellij.ui

import com.github.jaksonlin.pitestintellij.context.PitestContext
import com.github.jaksonlin.pitestintellij.services.RunHistoryManager
import com.intellij.openapi.components.service
import javax.swing.tree.DefaultMutableTreeNode
import javax.swing.tree.DefaultTreeModel
import com. intellij.ui.treeStructure.Tree
import javax.swing.SwingUtilities
import javax.swing.tree.TreePath

fun buildTreeModel(): DefaultTreeModel {
    val root = DefaultMutableTreeNode("Mutation History")
    val runHistory = service<RunHistoryManager>().getRunHistory()

    runHistory.forEach { (_, context) ->
        val packageName = context.targetClassPackageName ?: ""
        val packageNode = getOrCreatePackageNode(root, packageName)
        val newNode = DefaultMutableTreeNode(context.targetClassName)
        packageNode.add(newNode)
    }

    return DefaultTreeModel(root)
}

fun updateMutationTree(mutationTree:Tree,context:PitestContext) {
    val root = mutationTree.model.root as DefaultMutableTreeNode
    val packageName = context.targetClassPackageName ?: ""
    val packageNode = getOrCreatePackageNode(root, packageName)
    val newNode = DefaultMutableTreeNode(context.targetClassName)
    packageNode.add(newNode)
    SwingUtilities.invokeLater {
        mutationTree.expandPath(TreePath(packageNode.path)) // expand the package node
        mutationTree.updateUI()
    }
}
// Helper function to find a node by user object
fun findNode(root: DefaultMutableTreeNode, searchText: String): DefaultMutableTreeNode? {
    val enumeration = root.depthFirstEnumeration()
    while (enumeration.hasMoreElements()) {
        val node = enumeration.nextElement() as DefaultMutableTreeNode
        if (node.userObject.toString().contains(searchText, ignoreCase = true)) {
            return node
        }
    }
    return null
}

private fun getOrCreatePackageNode(root: DefaultMutableTreeNode, packageName: String): DefaultMutableTreeNode {
    var currentNode = root
    packageName.split('.').forEach { packagePart ->
        var childNode = currentNode.children().asSequence()
            .filterIsInstance<DefaultMutableTreeNode>()
            .find { it.userObject == packagePart }

        if (childNode == null) {
            childNode = DefaultMutableTreeNode(packagePart)
            currentNode.add(childNode)
        }
        currentNode = childNode
    }
    return currentNode
}
