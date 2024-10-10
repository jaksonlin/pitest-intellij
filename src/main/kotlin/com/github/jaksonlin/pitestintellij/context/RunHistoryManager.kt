package com.github.jaksonlin.pitestintellij.context
import com.intellij.openapi.application.PathManager
import com.google.gson.Gson
import com.google.gson.JsonSyntaxException
import com.google.gson.reflect.TypeToken
import com.jetbrains.rd.util.addUnique
import org.pitest.testapi.execute.Pitest
import java.io.File

object RunHistoryManager {
    private val gson = Gson()
    private val historyFile = File(PathManager.getConfigPath(), "run-history.json")
    private val history:MutableMap<String,PitestContext> = loadRunHistory()

    fun clearRunHistory() {
        history.clear()
        historyFile.delete()
    }

    fun saveRunHistory(entry: PitestContext) {
        history[entry.targetClassFullyQualifiedName!!] = entry
        historyFile.writeText(gson.toJson(history))
    }

    fun loadRunHistory(): MutableMap<String,PitestContext> {
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