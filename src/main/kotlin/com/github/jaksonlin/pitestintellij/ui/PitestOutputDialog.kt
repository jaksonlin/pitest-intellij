package com.github.jaksonlin.pitestintellij.ui

import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import java.awt.BorderLayout
import java.awt.Dimension
import java.awt.Desktop
import java.io.File
import javax.swing.*

class PitestOutputDialog(
    project: Project,
    private val output: String,
    dialogTitle: String,
    private val reportFile: File? = null
) : DialogWrapper(project) {

    init {
        title = dialogTitle
        init()
    }

    override fun createCenterPanel(): JComponent {
        val panel = JPanel(BorderLayout())
        val textArea = JTextArea(output).apply {
            isEditable = false
            lineWrap = true
            wrapStyleWord = true
        }

        val scrollPane = JScrollPane(textArea)
        scrollPane.preferredSize = Dimension(800, 600)
        panel.add(scrollPane, BorderLayout.CENTER)

        if (reportFile != null) {
            val viewReportButton = JButton("HTML Report").apply {
                addActionListener {
                    try {
                        Desktop.getDesktop().browse(reportFile.toURI())
                    } catch (e: Exception) {
                        JOptionPane.showMessageDialog(
                            panel,
                            "Error opening report: ${e.message}",
                            "Error",
                            JOptionPane.ERROR_MESSAGE
                        )
                    }
                }
            }
            panel.add(viewReportButton, BorderLayout.SOUTH)
        }

        return panel
    }
}