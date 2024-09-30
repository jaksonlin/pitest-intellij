package com.github.jaksonlin.pitestintellij.actions

import com.github.jaksonlin.pitestintellij.util.JavaFileProcessor
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.PlatformDataKeys
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.PathManager
import com.intellij.openapi.diagnostic.thisLogger
import com.intellij.openapi.module.ModuleManager
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.Task
import com.intellij.openapi.roots.CompilerModuleExtension
import com.intellij.openapi.roots.ModuleRootManager
import com.intellij.openapi.roots.ProjectRootManager
import com.intellij.openapi.ui.Messages
import com.intellij.platform.ide.progress.ModalTaskOwner.project
import org.gradle.tooling.GradleConnector
import org.gradle.tooling.model.idea.IdeaProject
import org.gradle.tooling.model.idea.IdeaSingleEntryLibraryDependency
import java.awt.Desktop
import java.io.File
import java.net.URI
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths


data class TargetClassInfo(val file: Path, val sourceRoot: Path)

class RunPitestAction : AnAction() {
    override fun actionPerformed(e: AnActionEvent) {
        val targetProject = e.project ?: return
        // 1. find the file that the user right click that fire this action
        val testVirtualFile = e.getData(PlatformDataKeys.VIRTUAL_FILE) ?: return
        // 2. get the path of the file
        val targetTestPath = testVirtualFile.path
        val javaFileProcessor = JavaFileProcessor()
        val fullyQualifiedTargetTestClassName = javaFileProcessor.getFullyQualifiedName(targetTestPath) ?: return
        // 3. get the testfile module
        val projectModule = ProjectRootManager.getInstance(targetProject).fileIndex.getModuleForFile(testVirtualFile)
        if (projectModule == null) {
            Messages.showMessageDialog(targetProject, "The file is not in a module", "Pitest Error", Messages.getErrorIcon())
            return
        }
        val moduleRootManager = ModuleRootManager.getInstance(projectModule)
        val javaHome = moduleRootManager.sdk!!.homePath
        if (javaHome == null) {
            Messages.showMessageDialog(targetProject, "The module does not have a JDK", "Pitest Error", Messages.getErrorIcon())
            return
        }

        // 3. prompt user to input the target class that this test file test against
        val targetClassInfo = findTargetClassFile(targetProject) ?: return
        val targetClassFile = targetClassInfo.file
        val sourceRoot = targetClassInfo.sourceRoot
        val fullyQualifiedTargetClassName = javaFileProcessor.getFullyQualifiedName(targetClassFile.toString()) ?: return

        // 3.3 determine the report directory, should align with junit report directory
        val reportDirectory = Paths.get(targetProject.basePath!!, "build", "reports", "pitest").toString()
        // create directory if not exist
        File(reportDirectory).mkdirs()
        // 3.4 determine the classpath to run the test, or say the test dependencies, and put it in a classpath file, because it can be huge that commandline will fail to handle
        val classpathFile = Paths.get(targetProject.basePath!!, "build", "reports", "pitest", "classpath.txt").toString()
        // retrieve the test dependencies classpath for the test, this can be tracked by the gradle test task
        val classpath = mutableListOf<String>()
        val connector = GradleConnector.newConnector().forProjectDirectory(File(targetProject.basePath!!))
        val connection = connector.connect()
        try {
            val ideaProject = connection.getModel(IdeaProject::class.java)
            ideaProject.modules.forEach { ideaModule ->
                ideaModule.dependencies.forEach { dependency ->
                    if (dependency is IdeaSingleEntryLibraryDependency) {
                        classpath.add(dependency.file.absolutePath)
                    }
                }
                ideaModule.gradleProject.buildDirectory?.let { buildDir ->
                    classpath.add(File(buildDir, "classes/java/main").absolutePath)
                    classpath.add(File(buildDir, "classes/java/test").absolutePath)
                }
            }

        } finally {
            connection.close()
        }

        // write the classpath to the classpath file, pitest classpath uses the newline to separate the classpath
        File(classpathFile).writeText(classpath.joinToString("\n"))

        try {
            // 4. prepare to run pitest
            // 4.1 we have placed the pitest jar files in the lib directory in this project, and it will bundle with the plugin,
            // use it to run the pitest so that we can have full control on what to run
            val pluginLibDir = PathManager.getPluginsPath() + "/pitest-intellij/lib"
            val pitestDependencies = File(pluginLibDir).listFiles { _, name -> name.endsWith(".jar") }?.joinToString(File.pathSeparator) { it.absolutePath } ?: ""
            // 4.2 prepare the command to run pitest
            // Run Pitest in the background
            // Run Pitest in the background
            val command = listOf(
                "java",
                "-cp",
                pitestDependencies,
                "org.pitest.mutationtest.commandline.MutationCoverageReport",
                "--reportDir",
                reportDirectory,
                "--targetClasses",
                fullyQualifiedTargetClassName,
                "--sourceDirs",
                sourceRoot.toString(),
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
            // 4.2 prepare the command to run pitest
            // log the command to run so that we know what happen
            thisLogger().info("Run pitest with command: ${command.joinToString(" ")}")
            // write the command to a file so that we can debug it
            File(Paths.get(targetProject.basePath!!, "build", "reports", "pitest", "command.txt").toString()).writeText(command.joinToString(" "))

            object : Task.Backgroundable(targetProject, "Running Pitest", true) {
                override fun run(indicator: ProgressIndicator) {
                    try {
                       // 4.3 run the command
                        val process = ProcessBuilder(command).start()
                        val output = StringBuilder()
                        val errorOutput = StringBuilder()
                        val outputReader = process.inputStream.bufferedReader()
                        val errorReader = process.errorStream.bufferedReader()
                        // Read the output and error streams
                        val outputThread = Thread {
                            outputReader.useLines { lines -> lines.forEach { output.appendLine(it) } }
                        }
                        val errorThread = Thread {
                            errorReader.useLines { lines -> lines.forEach { errorOutput.appendLine(it) } }
                        }
                        outputThread.start()
                        errorThread.start()

                        // Wait for the process to complete
                        val exitCode = process.waitFor()
                        outputThread.join()
                        errorThread.join()

                        // Handle the process output
                        ApplicationManager.getApplication().invokeLater {
                            if (exitCode == 0) {
                                // 4.5 show the output to the user
                                Messages.showMessageDialog(targetProject, output.toString(), "Pitest Output", Messages.getInformationIcon())
                                // 4.6 show the report to the user
                                Desktop.getDesktop().browse(URI.create("file://$reportDirectory/index.html"))
                            } else {
                                // 4.8 show the error to the user
                                Messages.showMessageDialog(targetProject, errorOutput.toString(), "Pitest Error", Messages.getErrorIcon())
                            }
                        }
                        // 4.7 log the command that we run
                        thisLogger().info("Run pitest with command: ${command.joinToString(" ")}")
                    } catch (e: Exception) {
                        // 4.9 log the error
                        thisLogger().error("Error when running pitest", e)
                        // 4.10 show the error to the user
                        ApplicationManager.getApplication().invokeLater {
                            Messages.showMessageDialog(targetProject, e.message, "Pitest Error", Messages.getErrorIcon())
                        }
                    }
                }
            }.queue()

        } catch (e: Exception) {
            // 4.8 show the error to the user
            Messages.showMessageDialog(targetProject, e.message, "Pitest Error", Messages.getErrorIcon())
            // 4.9 log the error
            thisLogger().error("Error when running pitest", e)
        }

    }

    fun findTargetClassFile(project: com.intellij.openapi.project.Project): TargetClassInfo? {
        // Get the target class name from the user input
        val targetClass = Messages.showInputDialog(project, "Please input the target class", "Run Pitest", Messages.getQuestionIcon())
        if (targetClass.isNullOrBlank()) {
            return null
        }

        // Get the source roots
        val sourceRoots = ModuleManager.getInstance(project).modules.flatMap { module ->
            ModuleRootManager.getInstance(module).contentRoots.map { contentRoot ->
                Paths.get(contentRoot.path)
            }
        }
        // Search for the target class file recursively in the source directories
        for (sourceRoot in sourceRoots) {
            val targetClassFile = findFileRecursively(sourceRoot, targetClass)
            if (targetClassFile != null) {
                return targetClassFile
            }
        }

        // If the target class file is not found, return null
        return null
    }

    fun findFileRecursively(directory: Path, targetClass: String): TargetClassInfo? {
        if (!Files.exists(directory) || !Files.isDirectory(directory)) {
            return null
        }

        val targetFileName = "$targetClass.java"
        for (file in Files.walk(directory).filter { path -> Files.isRegularFile(path) }.toList()) {
            if (file.fileName.toString() == targetFileName) {
                val directoryPath = file.parent
                // sourcePath is the path till src\main\java
                val indexToSrcMainJava = directoryPath.toString().indexOf("src${File.separator}main${File.separator}java")
                val sourceRootPath = directoryPath.toString().substring(0, indexToSrcMainJava)
                return TargetClassInfo(file, Paths.get(sourceRootPath))
            }
        }
        return null
    }
}