package com.github.jaksonlin.pitestintellij.mediator

import com.github.jaksonlin.pitestintellij.util.Mutation
import com.github.jaksonlin.pitestintellij.util.MutationReportParser
import kotlinx.coroutines.*

class MutationMediatorImpl : MutationMediator {
    private var ui: MutationUI? = null

    @OptIn(DelicateCoroutinesApi::class)
    override fun processMutations(mutationTargetClassFilePath:String, mutationReportFilePath: String) {
        GlobalScope.launch(Dispatchers.IO) {
            val mutations = MutationReportParser.parseMutationsFromXml(mutationReportFilePath).mutation
            withContext(Dispatchers.Main) {
                ui?.updateUI(mutationTargetClassFilePath, mutations)
            }
        }
    }

    override fun registerUI(ui: MutationUI) {
        this.ui = ui
    }
}