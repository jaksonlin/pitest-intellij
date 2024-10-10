package com.github.jaksonlin.pitestintellij.ui

import com.github.jaksonlin.pitestintellij.context.RunHistoryManager
import javax.swing.tree.DefaultMutableTreeNode
import javax.swing.tree.DefaultTreeModel

fun buildTreeModel(): DefaultTreeModel {
    val root = DefaultMutableTreeNode("Mutation History")
    val runHistory = RunHistoryManager.getRunHistory()

    runHistory.forEach { (className, context) ->
        val packageName = context.targetClassPackageName ?: ""
        val packageNode = getOrCreatePackageNode(root, packageName)
        packageNode.add(DefaultMutableTreeNode(className))
    }

    return DefaultTreeModel(root)
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
