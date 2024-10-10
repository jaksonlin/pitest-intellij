package com.github.jaksonlin.pitestintellij.mediator

interface MutationMediator {
    fun processMutations(mutationTargetClassFilePath:String, mutationReportFilePath: String)
    fun registerUI(ui: MutationUI)
}

interface MutationUI {
    fun updateUI(mutationClassFilePath:String, mutationTestResult: Map<Int, Pair<String, Boolean>>)
}


