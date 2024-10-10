package com.github.jaksonlin.pitestintellij.actions
import com.github.jaksonlin.pitestintellij.commands.*
import com.github.jaksonlin.pitestintellij.context.PitestContext
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.PlatformDataKeys
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.Task

class RunPitestAction : AnAction() {
    override fun actionPerformed(e: AnActionEvent) {
        val targetProject = e.project ?: return
        val testVirtualFile = e.getData(PlatformDataKeys.VIRTUAL_FILE) ?: return

        val context = PitestContext(testFilePath = testVirtualFile.path, timestamp = System.currentTimeMillis())

        val commands = listOf(
            PrepareEnvironmentCommand(targetProject, context),
            BuildPitestCommandCommand(targetProject, context),
            RunPitestCommand(targetProject, context),
            HandlePitestResultCommand(targetProject, context),
            StoreHistoryCommand(targetProject, context),
        )

        object : Task.Backgroundable(targetProject, "Running pitest", true) {
            override fun run(indicator: ProgressIndicator) {
                for (command in commands) {
                    if (indicator.isCanceled) { break }
                    command.execute()
                }
            }
        }.queue()
    }
}