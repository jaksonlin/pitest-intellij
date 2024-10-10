package com.github.jaksonlin.pitestintellij.mediator

import com.github.jaksonlin.pitestintellij.util.Mutation
import com.github.jaksonlin.pitestintellij.util.MutationReportParser

interface MutationMediator {
    fun processMutations(mutationTargetClassFilePath:String, mutationReportFilePath: String)
    fun registerUI(ui: MutationUI)
}

interface MutationUI {
    fun updateUI(mutationClassFilePath:String, mutations: List<Mutation>)
}


