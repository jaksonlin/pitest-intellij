package com.github.jaksonlin.pitestintellij.services

import com.github.jaksonlin.pitestintellij.commands.*
import com.github.jaksonlin.pitestintellij.context.PitestContext
import com.intellij.openapi.components.Service
import com.intellij.openapi.project.Project
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.Task

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
                for (command in commands) {
                    if (indicator.isCanceled) { break }
                    command.execute()
                }
            }
        }.queue()
    }
}