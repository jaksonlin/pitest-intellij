package com.github.jaksonlin.pitestintellij.commands

import PitestCommand
import com.github.jaksonlin.pitestintellij.context.PitestContext
import com.github.jaksonlin.pitestintellij.context.RunHistoryManager
import com.intellij.openapi.project.Project

class StoreHistoryCommand  (project: Project, context: PitestContext) : PitestCommand(project, context) {
    override fun execute() {
        RunHistoryManager.saveRunHistory(context)
    }
}