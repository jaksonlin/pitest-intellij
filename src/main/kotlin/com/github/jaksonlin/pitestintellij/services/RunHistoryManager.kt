package com.github.jaksonlin.pitestintellij.services
import com.github.jaksonlin.pitestintellij.context.PitestContext
import com.github.jaksonlin.pitestintellij.observers.ObserverBase
import com.intellij.openapi.application.PathManager
import com.intellij.openapi.components.Service
import com.google.gson.Gson
import com.google.gson.JsonSyntaxException
import com.google.gson.reflect.TypeToken
import java.io.File


@Service(Service.Level.APP)
class RunHistoryManager: ObserverBase() {
    private val gson = Gson()
    private val historyFile = File(PathManager.getConfigPath(), "run-history.json")
    private val history:MutableMap<String, PitestContext> = loadRunHistory()

    fun getRunHistoryForClass(targetClassFullyQualifiedName: String): PitestContext? {
        if (!history.containsKey(targetClassFullyQualifiedName)) {
            return null
        }
        return history[targetClassFullyQualifiedName]
    }

    fun clearRunHistory() {
        history.clear()
        historyFile.delete()
        notifyObservers(null)
    }

    fun getRunHistory():Map<String, PitestContext> {
        return history.toMap()
    }

    fun saveRunHistory(entry: PitestContext) {
        history[entry.targetClassFullyQualifiedName!!] = entry
        historyFile.writeText(gson.toJson(history))
        notifyObservers(entry)
    }

    private fun loadRunHistory(): MutableMap<String, PitestContext> {
        if (!historyFile.exists()) {
            return HashMap()
        }
        return try {
            val type = object : TypeToken<HashMap<String, PitestContext>>() {}.type
            gson.fromJson(historyFile.readText(), type)
        } catch (e: JsonSyntaxException) {
            historyFile.delete()
            HashMap()
        }
    }
}