package com.github.jaksonlin.pitestintellij.commands

import com.github.jaksonlin.pitestintellij.util.ProcessResult
import com.intellij.openapi.vfs.VirtualFile
import java.nio.file.Path

data class PitestContext(
    var testVirtualFile: VirtualFile? = null,
    var fullyQualifiedTargetTestClassName: String? = null,
    var javaHome: String? = null,
    var sourceRoots: List<Path> = emptyList(),
    var fullyQualifiedTargetClassName: String? = null,
    var targetClassSourceRoot:String?=null,
    var reportDirectory: String? = null,
    var classpathFile: String? = null,
    var command: List<String> = emptyList(),
    var processResult: ProcessResult? = null,
    var pitestDependencies: String? = null,
)

fun dumpPitestContext(context: PitestContext): String {
    return """
        testVirtualFile: ${context.testVirtualFile}
        fullyQualifiedTargetTestClassName: ${context.fullyQualifiedTargetTestClassName}
        javaHome: ${context.javaHome}
        sourceRoots: ${context.sourceRoots}
        fullyQualifiedTargetClassName: ${context.fullyQualifiedTargetClassName}
        targetClassSourceRoot: ${context.targetClassSourceRoot}
        reportDirectory: ${context.reportDirectory}
        classpathFile: ${context.classpathFile}
        command: ${context.command}
        processResult: ${context.processResult}
        pitestDependencies: ${context.pitestDependencies}
    """.trimIndent()
}