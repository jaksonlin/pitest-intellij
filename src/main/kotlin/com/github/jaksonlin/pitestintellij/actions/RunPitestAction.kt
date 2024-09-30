package com.github.jaksonlin.pitestintellij.actions
import com.github.jaksonlin.pitestintellij.ui.PitestOutputDialog
import com.github.jaksonlin.pitestintellij.util.*
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.PlatformDataKeys
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.PathManager
import com.intellij.openapi.diagnostic.thisLogger
import com.intellij.openapi.module.ModuleManager
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.Task
import com.intellij.openapi.roots.ModuleRootManager
import com.intellij.openapi.roots.ProjectRootManager
import com.intellij.openapi.ui.Messages
import java.awt.Desktop
import java.io.File
import java.net.URI
import java.nio.file.Paths

class RunPitestAction : AnAction() {
    override fun actionPerformed(e: AnActionEvent) {
        val targetProject = e.project ?: return
        val testVirtualFile = e.getData(PlatformDataKeys.VIRTUAL_FILE) ?: return
        val targetTestPath = testVirtualFile.path
        val javaFileProcessor = JavaFileProcessor()
        val fullyQualifiedTargetTestClassName = javaFileProcessor.getFullyQualifiedName(targetTestPath) ?: return

        val projectModule = ProjectRootManager.getInstance(targetProject).fileIndex.getModuleForFile(testVirtualFile)
        if (projectModule == null) {
            Messages.showMessageDialog(targetProject, "The file is not in a module", "Pitest Error", Messages.getErrorIcon())
            return
        }
        val moduleRootManager = ModuleRootManager.getInstance(projectModule)
        val javaHome = moduleRootManager.sdk?.homePath
        if (javaHome == null) {
            Messages.showMessageDialog(targetProject, "The module does not have a JDK", "Pitest Error", Messages.getErrorIcon())
            return
        }

        val sourceRoots = ModuleManager.getInstance(targetProject).modules.flatMap { module ->
            ModuleRootManager.getInstance(module).contentRoots.map { contentRoot ->
                Paths.get(contentRoot.path)
            }
        }
        val targetClass = Messages.showInputDialog(targetProject, "Please input the target class", "Run Pitest", Messages.getQuestionIcon())
        if (targetClass.isNullOrBlank()) {
            return
        }
        val targetClassInfo = FileUtils.findTargetClassFile(sourceRoots, targetClass) ?: return
        val fullyQualifiedTargetClassName = javaFileProcessor.getFullyQualifiedName(targetClassInfo.file.toString()) ?: return

        val reportDirectory = Paths.get(targetProject.basePath!!, "build", "reports", "pitest").toString()
        File(reportDirectory).mkdirs()
        val classpathFile = Paths.get(targetProject.basePath!!, "build", "reports", "pitest", "classpath.txt").toString()

        val classpath = GradleUtils.getClasspath(targetProject.basePath!!)
        File(classpathFile).writeText(classpath.joinToString("\n"))

        val pluginLibDir = PathManager.getPluginsPath() + "/pitest-intellij/lib"
        val pitestDependencies = File(pluginLibDir).listFiles { _, name -> name.endsWith(".jar") }?.joinToString(File.pathSeparator) { it.absolutePath } ?: ""

        val command = listOf(
            "$javaHome/bin/java",
            "-cp",
            pitestDependencies,
            "org.pitest.mutationtest.commandline.MutationCoverageReport",
            "--reportDir",
            reportDirectory,
            "--targetClasses",
            fullyQualifiedTargetClassName,
            "--sourceDirs",
            targetClassInfo.sourceRoot.toString(),
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

        thisLogger().info("Run pitest with command: ${command.joinToString(" ")}")
        File(Paths.get(targetProject.basePath!!, "build", "reports", "pitest", "command.txt").toString()).writeText(command.joinToString(" "))

        object : Task.Backgroundable(targetProject, "Running Pitest", true) {
            override fun run(indicator: ProgressIndicator) {
                val result = ProcessExecutor.executeProcess(command)
                
                ApplicationManager.getApplication().invokeLater {
                    when (result.exitCode) {
                        0 -> {
                            PitestOutputDialog(targetProject, result.output).show()
                            try {
                                val reportFile = File(reportDirectory, "index.html")
                                if (reportFile.exists()) {
                                    Desktop.getDesktop().browse(reportFile.toURI())
                                } else {
                                    Messages.showErrorDialog(targetProject, "Report file not found: ${reportFile.absolutePath}", "Pitest Error")
                                }
                            } catch (e: Exception) {
                                thisLogger().error("Error opening report", e)
                                Messages.showErrorDialog(targetProject, "Error opening report: ${e.message}", "Pitest Error")
                            }
                        }
                        -1 -> {
                            PitestOutputDialog(targetProject, "Error running Pitest:\n\n${result.errorOutput}").show()
                        }
                        else -> {
                            PitestOutputDialog(targetProject, result.errorOutput).show()
                        }
                    }
                }
            }
        }.queue()
    }
}