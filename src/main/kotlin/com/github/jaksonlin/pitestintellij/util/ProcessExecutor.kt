package com.github.jaksonlin.pitestintellij.util


data class ProcessResult(
    val exitCode: Int,
    val output: String,
    val errorOutput: String
)

object ProcessExecutor {
    fun executeProcess(command: List<String>): ProcessResult {
        try {
            val process = ProcessBuilder(command).start()
            val output = StringBuilder()
            val errorOutput = StringBuilder()
            val outputReader = process.inputStream.bufferedReader()
            val errorReader = process.errorStream.bufferedReader()

            val outputThread = Thread {
                outputReader.useLines { lines -> lines.forEach { output.appendLine(it) } }
            }
            val errorThread = Thread {
                errorReader.useLines { lines -> lines.forEach { errorOutput.appendLine(it) } }
            }
            outputThread.start()
            errorThread.start()

            val exitCode = process.waitFor()
            outputThread.join()
            errorThread.join()

            return ProcessResult(exitCode, output.toString(), errorOutput.toString())
        } catch (e: Exception) {
            return ProcessResult(-1, "", e.message ?: "Unknown error occurred")
        }
    }
}