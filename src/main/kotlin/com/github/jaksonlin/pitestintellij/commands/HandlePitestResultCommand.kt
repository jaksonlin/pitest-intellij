package com.github.jaksonlin.pitestintellij.commands

import PitestCommand
import com.intellij.openapi.diagnostic.thisLogger
import com.intellij.openapi.project.Project
import java.awt.Desktop
import java.io.File

class HandlePitestResultCommand(project: Project, context: PitestContext) : PitestCommand(project, context) {
    override fun execute() {
        val result = context.processResult ?: throw IllegalStateException("Process result not available")
        when (result.exitCode) {
            0 -> {
                showOutput(result.output, "Pitest Output")
                try {
                    val reportFile = File(context.reportDirectory!!, "index.html")
                    if (reportFile.exists()) {
                        Desktop.getDesktop().browse(reportFile.toURI())
                    } else {
                        showError("Report file not found: ${reportFile.absolutePath}")
                    }
                } catch (e: Exception) {
                    thisLogger().error("Error opening report", e)
                    showError("Error opening report: ${e.message}")
                }
            }
            -1 -> showOutput("Error running Pitest:\n\n${result.errorOutput}", "Pitest Error")
            else -> showOutput("Pitest exited with code ${result.exitCode}:\n\n${result.errorOutput}", "Pitest Error")
        }
    }
}