package com.github.jaksonlin.pitestintellij.mediators

interface IMutationMediator {
    fun processMutationResult(mutationTargetClassFilePath:String, mutationReportFilePath: String)
    fun register(clientUI: IMutationReportUI)
}

interface IMutationReportUI {
    fun updateMutationResult(mutationClassFilePath:String, mutationTestResult: Map<Int, Pair<String, Boolean>>)
}


