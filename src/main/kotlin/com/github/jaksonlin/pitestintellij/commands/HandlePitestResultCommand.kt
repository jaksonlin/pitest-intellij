package com.github.jaksonlin.pitestintellij.commands

import PitestCommand
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.diagnostic.thisLogger
import com.intellij.openapi.project.Project
import com.github.jaksonlin.pitestintellij.ui.PitestOutputDialog
import java.io.File

class HandlePitestResultCommand(project: Project, context: PitestContext) : PitestCommand(project, context) {
    override fun execute() {
        val result = context.processResult ?: throw IllegalStateException("Process result not available")
        when (result.exitCode) {
            0 -> {
                val reportFile = File(context.reportDirectory!!, "index.html")
                if (reportFile.exists()) {
                    showOutputWithReportButton(result.output, "Pitest Output", reportFile)
                } else {
                    showOutput(result.output, "Pitest Output")
                    showError("Report file not found: ${reportFile.absolutePath}")
                }
            }
            -1 -> showOutput("Error running Pitest:\n\n${result.errorOutput}", "Pitest Error")
            else -> showOutput("Pitest exited with code ${result.exitCode}:\n\n${result.errorOutput}", "Pitest Error")
        }
    }

    private fun showOutputWithReportButton(output: String, title: String, reportFile: File) {
        ApplicationManager.getApplication().invokeLater {
            PitestOutputDialog(project, output, title, reportFile).show()
        }
    }
}