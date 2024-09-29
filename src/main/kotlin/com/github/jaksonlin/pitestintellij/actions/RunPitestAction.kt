package com.github.jaksonlin.pitestintellij.actions

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.PlatformDataKeys
import com.intellij.openapi.module.ModuleManager
import com.intellij.openapi.roots.ModuleRootManager
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.diagnostic.thisLogger
import org.gradle.tooling.GradleConnector
import org.gradle.tooling.model.idea.IdeaProject
import org.gradle.tooling.model.idea.IdeaSingleEntryLibraryDependency
import java.awt.Desktop
import java.io.File
import java.net.URI
import java.nio.file.Paths

class RunPitestAction : AnAction() {
    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        // 1. find the file that the user right click that fire this action
        val file = e.getData(PlatformDataKeys.VIRTUAL_FILE) ?: return
        // 2. get the path of the file
        val targetTestPath = file.path
        val fullyQualifiedTargetClassName = targetTestPath.substring(targetTestPath.indexOf("src/main/java/") + "src/main/java/".length, targetTestPath.length - ".java".length).replace("/", ".")

        // 3. prompt user to input the target class that this test file test against
        // 3.1 we can use the Messages.showInputDialog to prompt user to input the target class
        val targetClass = Messages.showInputDialog(project, "Please input the target class", "Run Pitest", Messages.getQuestionIcon())
        if (targetClass.isNullOrBlank()) {
            return
        }
        // 3.2 find the project's java source file path
        val sourceRoots = ModuleManager.getInstance(project).modules.flatMap { module ->
            ModuleRootManager.getInstance(module).sourceRoots.toList()
        }
        // 3.3 determine the report directory, should align with junit report directory
        val reportDirectory = Paths.get(project.basePath!!, "build", "reports", "pitest").toString()
        // 3.4 determine the classpath to run the test, or say the test dependencies, and put it in a classpath file, because it can be huge that commandline will fail to handle
        val classpathFile = Paths.get(project.basePath!!, "build", "reports", "pitest", "classpath.txt").toString()
        // retrieve the test dependencies classpath for the test, this can be tracked by the gradle test task
        val classpath = mutableListOf<String>()
        val connector = GradleConnector.newConnector().forProjectDirectory(File(project.basePath!!))
        val connection = connector.connect()
        try {
            val ideaProject = connection.getModel(IdeaProject::class.java)
            ideaProject.modules.forEach { module ->
                module.dependencies.forEach { dependency ->
                    if (dependency is IdeaSingleEntryLibraryDependency) {
                        classpath.add(dependency.file.absolutePath)
                    }
                }
            }
        } finally {
            connection.close()
        }
        // write the classpath to the classpath file
        File(classpathFile).writeText(classpath.joinToString(File.pathSeparator))
        // 4. prepare to run pitest
        // 4.1 we have placed the pitest jar files in the lib directory in this project, and it will bundle with the plugin,
        // use it to run the pitest so that we can have full control on what to run
        val pitestDependencies = Paths.get(project.basePath!!, "lib").toFile().listFiles { _, name -> name.endsWith(".jar") }?.joinToString(File.pathSeparator)
        // 4.2 prepare the command to run pitest
        val command = listOf(
                "java",
                "-cp",
                "$classpathFile${File.pathSeparator}$pitestDependencies",
                "org.pitest.mutationtest.commandline.MutationCoverageReport",
                "--reportDir",
                reportDirectory,
                "--targetClasses",
                targetClass,
                "--sourceDirs",
                sourceRoots.joinToString(","),
                "--classPathFile",
                classpathFile,
                "--targetTests",
                fullyQualifiedTargetClassName,
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
        // 4.3 run the command
        val process = ProcessBuilder(command).start()
        // 4.4 read the output of the command
        val output = process.inputStream.bufferedReader().readText()
        // 4.5 show the output to the user
        Messages.showMessageDialog(project, output, "Pitest Output", Messages.getInformationIcon())
        // 4.6 show the report to the user
        Desktop.getDesktop().browse(URI.create("file://$reportDirectory/index.html"))
        // 4.7 log the command that we run
        thisLogger().info("Run pitest with command: ${command.joinToString(" ")}")
    }
}