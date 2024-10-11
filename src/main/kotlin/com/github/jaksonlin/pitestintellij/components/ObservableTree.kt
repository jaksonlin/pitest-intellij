package com.github.jaksonlin.pitestintellij.components
import com.github.jaksonlin.pitestintellij.MyBundle
import com.github.jaksonlin.pitestintellij.observers.RunHistoryObserver
import com.intellij.ui.treeStructure.Tree
import javax.swing.SwingUtilities
import javax.swing.tree.DefaultMutableTreeNode
import javax.swing.tree.DefaultTreeModel
import javax.swing.tree.TreePath

class ObservableTree:Tree(), RunHistoryObserver {

    override fun onRunHistoryChanged(eventObj:Any?) {
        when (eventObj) {
            null -> initializeMutationTree(emptyList())
            is Pair<*, *> -> updateMutationTree(eventObj as? Pair<String, String> ?: return)
            is List<*> -> initializeMutationTree(eventObj as? List<Pair<String, String>> ?: return)
            else -> return
        }
    }

    private fun initializeMutationTree(nodeNameList: List<Pair<String, String>>) {
        val treeModel = buildTreeModel(nodeNameList)
        model = treeModel
    }

    private fun buildTreeModel(nodeNameList: List<Pair<String, String>>): DefaultTreeModel {
        val root = DefaultMutableTreeNode(MyBundle.message("mutation.tree.root"))

        nodeNameList.forEach {
            val packageName = it.first
            val packageNode = getOrCreatePackageNode(root, packageName)
            val newNode = DefaultMutableTreeNode(it.second)
            packageNode.add(newNode)
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

    private fun updateMutationTree(pair: Pair<String, String>) {
        val root = model.root as DefaultMutableTreeNode
        val packageName = pair.first
        val packageNode = getOrCreatePackageNode(root, packageName)
        val newNode = DefaultMutableTreeNode(pair.second)
        packageNode.add(newNode)
        SwingUtilities.invokeLater {
            expandPath(TreePath(packageNode.path)) // expand the package node
            updateUI()
        }
    }
}