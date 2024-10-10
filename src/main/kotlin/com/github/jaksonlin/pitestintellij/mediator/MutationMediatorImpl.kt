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
            val renderedFormat = convertResultToUIRenderFormat(mutations)
            withContext(Dispatchers.Main) {
                ui?.updateUI(mutationTargetClassFilePath, renderedFormat)
            }
        }
    }

    private fun convertResultToUIRenderFormat(mutations: List<Mutation>): Map<Int, Pair<String, Boolean>> {
        // when the multiple mutation is in the same line, we need to merge them, all killed or not
        val result = mutableMapOf<Int, List<Pair<String, Boolean>>>()

        mutations.forEach {
            val line = it.lineNumber
            val mutationMessage = mutationMessageFormat(it, result[line]?.size?.plus(1) ?: 1)
            val mutationPair = Pair(mutationMessage, it.status == "KILLED")

            if (result.containsKey(line)) {
                result[line] = result[line]!!.plus(mutationPair)
            } else {
                result[line] = listOf(mutationPair)
            }
        }
        return result.mapValues {
            val isAllKilled = it.value.all { pair -> pair.second }
            val message = it.value.joinToString("\n") { pair -> pair.first }
            Pair(message, isAllKilled)
        }
    }

    private fun mutationMessageFormat(mutation : Mutation, groupNumber:Int) : String {
        return "$groupNumber ${mutation.description} -> ${mutation.status}"
    }

    override fun registerUI(ui: MutationUI) {
        this.ui = ui
    }
}