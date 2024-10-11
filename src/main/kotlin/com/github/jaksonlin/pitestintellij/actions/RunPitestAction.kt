package com.github.jaksonlin.pitestintellij.actions

import com.github.jaksonlin.pitestintellij.services.PitestService
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.PlatformDataKeys
import com.intellij.openapi.components.service

class RunPitestAction : AnAction() {
    private val pitestService = service<PitestService>()
    override fun actionPerformed(e: AnActionEvent) {
        val targetProject = e.project ?: return
        val testVirtualFile = e.getData(PlatformDataKeys.VIRTUAL_FILE) ?: return
        pitestService.runPitest(targetProject, testVirtualFile.path)
    }
}