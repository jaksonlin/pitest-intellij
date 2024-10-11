package com.github.jaksonlin.pitestintellij.context

import com.github.jaksonlin.pitestintellij.util.ProcessResult
import com.intellij.openapi.vfs.VirtualFile
import java.nio.file.Path

data class PitestContext(
    var testFilePath: String? = null,
    var fullyQualifiedTargetTestClassName: String? = null,
    var javaHome: String? = null,
    var sourceRoots: List<String>? = null,
    var targetClassFullyQualifiedName: String? = null,
    var targetClassPackageName: String? = null,
    var targetClassSourceRoot:String?=null,
    var targetClassFilePath: String?=null,
    var targetClassName:String?=null,
    var reportDirectory: String? = null,
    var classpathFile: String? = null,
    var classpathFileDirectory: String? = null,
    var command: List<String> = emptyList(),
    @Transient var processResult: ProcessResult? = null,
    var pitestDependencies: String? = null,
    var resourceDirectories: List<String>? = null,
    val timestamp: Long,
)



fun dumpPitestContext(context: PitestContext): String {
    return """
        testVirtualFile: ${context.testFilePath}
        fullyQualifiedTargetTestClassName: ${context.fullyQualifiedTargetTestClassName}
        javaHome: ${context.javaHome}
        sourceRoots: ${context.sourceRoots}
        fullyQualifiedTargetClassName: ${context.targetClassFullyQualifiedName}
        targetClassSourceRoot: ${context.targetClassSourceRoot}
        reportDirectory: ${context.reportDirectory}
        classpathFile: ${context.classpathFile}
        command: ${context.command}
        processResult: ${context.processResult}
        pitestDependencies: ${context.pitestDependencies}
    """.trimIndent()
}