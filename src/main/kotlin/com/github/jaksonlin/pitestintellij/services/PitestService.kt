package com.github.jaksonlin.pitestintellij.services

import com.github.jaksonlin.pitestintellij.commands.*
import com.github.jaksonlin.pitestintellij.context.PitestContext
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.Service
import com.intellij.openapi.project.Project
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.Task
import com.intellij.openapi.ui.Messages
import com.jetbrains.rd.util.ExecutionException

@Service(Service.Level.APP)
class PitestService {

    fun runPitest(targetProject: Project, testFilePath: String) {
        val context = PitestContext(testFilePath = testFilePath, timestamp = System.currentTimeMillis())

        val commands = listOf(
            PrepareEnvironmentCommand(targetProject, context),
            BuildPitestCommandCommand(targetProject, context),
            RunPitestCommand(targetProject, context),
            HandlePitestResultCommand(targetProject, context),
            StoreHistoryCommand(targetProject, context),
        )

        object : Task.Backgroundable(targetProject, "Running pitest", true) {
            override fun run(indicator: ProgressIndicator) {
                try {
                    for (command in commands) {
                        if (indicator.isCanceled) {
                            Messages.showInfoMessage("Pitest run was canceled", "Canceled")
                            break
                        }
                        command.execute()
                    }
                } catch(e: CommandCancellationException) {
                    ApplicationManager.getApplication().invokeLater {
                        Messages.showInfoMessage("Pitest run was canceled", "Canceled")
                    }
                } catch (e: ExecutionException) {
                    if (e.cause is CommandCancellationException) {
                        ApplicationManager.getApplication().invokeLater {
                            Messages.showInfoMessage("Pitest run was canceled", "Canceled")
                        }
                    } else {
                        ApplicationManager.getApplication().invokeLater {
                            Messages.showErrorDialog("Error executing Pitest command: ${e.message}", "Error")
                        }
                    }
                } catch (e: Exception) {
                    ApplicationManager.getApplication().invokeLater {
                        Messages.showErrorDialog("Error executing Pitest command: ${e.message}", "Error")
                    }
                }
            }
        }.queue()
    }
}