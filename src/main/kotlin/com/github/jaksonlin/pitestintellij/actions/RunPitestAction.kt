package com.github.jaksonlin.pitestintellij.actions
import PrepareEnvironmentCommand
import com.github.jaksonlin.pitestintellij.commands.BuildPitestCommandCommand
import com.github.jaksonlin.pitestintellij.commands.HandlePitestResultCommand
import com.github.jaksonlin.pitestintellij.commands.PitestContext
import com.github.jaksonlin.pitestintellij.commands.RunPitestCommand
import com.github.jaksonlin.pitestintellij.ui.PitestOutputDialog
import com.github.jaksonlin.pitestintellij.util.*
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.PlatformDataKeys
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.PathManager
import com.intellij.openapi.diagnostic.thisLogger
import com.intellij.openapi.module.ModuleManager
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.Task
import com.intellij.openapi.roots.ModuleRootManager
import com.intellij.openapi.roots.ProjectRootManager
import com.intellij.openapi.ui.Messages
import java.awt.Desktop
import java.io.File
import java.net.URI
import java.nio.file.Paths

class RunPitestAction : AnAction() {
    override fun actionPerformed(e: AnActionEvent) {
        val targetProject = e.project ?: return
        val testVirtualFile = e.getData(PlatformDataKeys.VIRTUAL_FILE) ?: return

        val context = PitestContext(testVirtualFile = testVirtualFile)

        val commands = listOf(
            PrepareEnvironmentCommand(targetProject, context),
            BuildPitestCommandCommand(targetProject, context),
            RunPitestCommand(targetProject, context),
            HandlePitestResultCommand(targetProject, context),
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