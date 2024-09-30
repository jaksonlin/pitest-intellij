package com.github.jaksonlin.pitestintellij.ui

import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import java.awt.Dimension
import javax.swing.JComponent
import javax.swing.JPanel
import javax.swing.JScrollPane
import javax.swing.JTextArea

class PitestOutputDialog(project: Project, private val output: String, titleInfo: String) : DialogWrapper(project) {

    init {
        title = titleInfo
        init()
    }

    override fun createCenterPanel(): JComponent {
        val textArea = JTextArea(output).apply {
            isEditable = false
            lineWrap = true
            wrapStyleWord = true
        }

        val scrollPane = JScrollPane(textArea)
        scrollPane.preferredSize = Dimension(800, 600)  // Adjust size as needed

        return JPanel().apply {
            add(scrollPane)
        }
    }
}