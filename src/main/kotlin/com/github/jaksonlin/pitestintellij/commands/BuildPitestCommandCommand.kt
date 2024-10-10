package com.github.jaksonlin.pitestintellij.commands

import PitestCommand
import com.github.jaksonlin.pitestintellij.context.PitestContext
import com.intellij.openapi.project.Project

class BuildPitestCommandCommand (project: Project, context: PitestContext) : PitestCommand(project, context) {
    override fun execute() {
        val piptestDependencies = context.pitestDependencies ?: throw IllegalStateException("Pitest dependencies not set")
        val reportDirectory = context.reportDirectory ?: throw IllegalStateException("Report directory not set")
        val classpathFile = context.classpathFile ?: throw IllegalStateException("Classpath file not set")
        val fullyQualifiedTargetTestClassName = context.fullyQualifiedTargetTestClassName ?: throw IllegalStateException("Fully qualified target class name not set")
        val fullyQualifiedTargetClassName = context.targetClassFullyQualifiedName ?: throw IllegalStateException("Fully qualified target class name not set")
        val targetClassSourceRoot = context.targetClassSourceRoot ?: throw IllegalStateException("target class source root not set")
        val javaHome = context.javaHome ?: throw IllegalStateException("Java home not set")
        val javaExe = "$javaHome/bin/java"
        context.command = listOf(
            javaExe,
            "-cp",
            piptestDependencies,
            "org.pitest.mutationtest.commandline.MutationCoverageReport",
            "--reportDir",
            reportDirectory,
            "--targetClasses",
            fullyQualifiedTargetClassName,
            "--sourceDirs",
            targetClassSourceRoot,
            "--classPathFile",
            classpathFile,
            "--targetTests",
            fullyQualifiedTargetTestClassName,
            "--outputFormats",
            "HTML,XML",
            "--timeoutConst",
            "10000",
            "--threads",
            "4",
            "--verbose",
            "true",
            "--timeoutFactor",
            "2.0",
            "--mutators",
            "STRONGER",
        )
    }
}