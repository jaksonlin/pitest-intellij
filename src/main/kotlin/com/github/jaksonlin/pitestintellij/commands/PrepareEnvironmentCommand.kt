package com.github.jaksonlin.pitestintellij.commands

import PitestCommand
import com.github.jaksonlin.pitestintellij.context.PitestContext
import com.github.jaksonlin.pitestintellij.util.FileUtils
import com.github.jaksonlin.pitestintellij.util.GradleUtils
import com.github.jaksonlin.pitestintellij.util.JavaFileProcessor
import com.intellij.openapi.application.PathManager
import com.intellij.openapi.application.ReadAction
import com.intellij.openapi.module.ModuleManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.ModuleRootManager
import com.intellij.openapi.roots.ProjectRootManager
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.openapi.vfs.VirtualFile
import java.io.File
import java.nio.file.Paths

class PrepareEnvironmentCommand(project: Project, context: PitestContext) : PitestCommand(project, context) {
    private val javaFileProcessor = JavaFileProcessor()

    override fun execute() {
        val testVirtualFile = ReadAction.compute<VirtualFile?, Throwable> {
            LocalFileSystem.getInstance().findFileByPath(context.testFilePath!!)
        }
        if (testVirtualFile == null) {
            showError("Cannot find test file")
            throw IllegalStateException("Cannot find test file")
        }

        collectTargetTestClassName(context.testFilePath!!)
        collectJavaInfo(testVirtualFile)
        collectSourceRoots()
        collectResourceDirectories()
        
        collectTargetClassThatWeTest(context.sourceRoots!!)
        prepareReportDirectory(testVirtualFile, context.targetClassFullyQualifiedName!!)

        setupPitestLibDependencies(context.resourceDirectories!!)
        collectClassPathFileForPitest(context.reportDirectory!!, context.targetClassPackageName!!, context.resourceDirectories)
    }

    private fun collectTargetTestClassName(targetTestClassFilePath:String) {

        val testClassInfo = javaFileProcessor.getFullyQualifiedName(targetTestClassFilePath)

        if (testClassInfo == null) {
            showError("Cannot get fully qualified name for target test class")
            throw IllegalStateException("Cannot get fully qualified name for target test class")
        }

        context.fullyQualifiedTargetTestClassName = testClassInfo.fullyQualifiedName
    }

    private fun collectJavaInfo(testVirtualFile: VirtualFile) {
        ReadAction.run<Throwable> {
            val projectModule = ProjectRootManager.getInstance(project).fileIndex.getModuleForFile(testVirtualFile)
            if (projectModule == null) {
                showError("Cannot find module for test file")
                throw IllegalStateException("Cannot find module for test file")
            }

            val moduleRootManager = ModuleRootManager.getInstance(projectModule)
            context.javaHome = moduleRootManager.sdk?.homePath
        }
        if (context.javaHome.isNullOrBlank()) {
            showError("Cannot find java home")
            throw IllegalStateException("Cannot find java home")
        }
    }

    private fun collectSourceRoots() {
        context.sourceRoots = ReadAction.compute<List<String>, Throwable> {
            ModuleManager.getInstance(project).modules.flatMap { module ->
                ModuleRootManager.getInstance(module).contentRoots.map { contentRoot ->
                    Paths.get(contentRoot.path).toString()
                }
            }
        }
    }

    private fun collectResourceDirectories(){
        val resourceDirectories = ReadAction.compute<List<String>, Throwable> {
            GradleUtils.getResourceDirectories(project)
        }
        context.resourceDirectories = resourceDirectories
    }

    private fun collectTargetClassThatWeTest(sourceRoots:List<String>) {
        // The user input dialog and file operations don't need to be in ReadAction
        val targetClass = showInputDialog("Please enter the name of the class that you want to test", "Enter target class")
        if (targetClass.isNullOrBlank()) {
            return
        }
        val targetClassInfo = FileUtils.findTargetClassFile(sourceRoots, targetClass)
        if (targetClassInfo == null) {
            showError("Cannot find target class file")
            throw IllegalStateException("Cannot find target class file")
        }
        val classInfo = javaFileProcessor.getFullyQualifiedName(targetClassInfo.file.toString())

        if (classInfo == null) {
            showError("Cannot get fully qualified name for target class")
            throw IllegalStateException("Cannot get fully qualified name for target class")
        }
        context.targetClassFullyQualifiedName = classInfo.fullyQualifiedName
        context.targetClassPackageName = classInfo.packageName
        context.targetClassSourceRoot = targetClassInfo.sourceRoot.toString()
    }

    private fun prepareReportDirectory(testVirtualFile: VirtualFile, className: String){
        // prepare the report directory
        val parentModulePath = ReadAction.compute<String, Throwable> {

            val projectModule = ProjectRootManager.getInstance(project).fileIndex.getModuleForFile(testVirtualFile)
            if (projectModule == null) {
                showError("Cannot find module for test file")
                throw IllegalStateException("Cannot find module for test file")
            }

            GradleUtils.getUpperModulePath(project, projectModule)
        }
        context.reportDirectory = Paths.get(parentModulePath, "build", "reports", "pitest", className).toString()
        File(context.reportDirectory!!).mkdirs()

    }



    private fun collectClassPathFileForPitest(reportDirectory:String, targetPackageName:String, resourceDirectories: List<String>?){
        val classPathFileContent = ReadAction.compute<String, Throwable> {
            val classpath = GradleUtils.getCompilationOutputPaths(project)
            val testDependencies = GradleUtils.getTestRunDependencies(project)
            val allDependencies = ArrayList<String>()
            allDependencies.addAll(classpath)
            if (resourceDirectories != null) {
                allDependencies.addAll(resourceDirectories)
            }
            allDependencies.addAll(testDependencies)
            allDependencies.joinToString("\n")
        }
        showOutput("Classpath file content: $classPathFileContent", "Classpath file content")
        context.classpathFileDirectory  = Paths.get(reportDirectory, targetPackageName).toString()
        File(context.classpathFileDirectory!!).mkdirs()
        context.classpathFile = Paths.get(context.classpathFileDirectory!!, "classpath.txt").toString()
        File(context.classpathFile!!).writeText(classPathFileContent)
    }

    private fun setupPitestLibDependencies(resourceDirectories: List<String>) {

        val pluginLibDir = ReadAction.compute<String, Throwable> {
            PathManager.getPluginsPath() + "/pitest-intellij/lib"
        }
        val dependencies = mutableListOf<String>()
        for (file in File(pluginLibDir).listFiles()!!) {
            if (file.name.startsWith("pitest-intellij-")) {
                continue
            }
            if (file.name.endsWith(".jar")) {
                if (file.name.startsWith("pitest") || file.name.startsWith("commons")) {
                    dependencies.add(file.absolutePath)
                }
            }
        }
        dependencies.addAll(resourceDirectories)
        if (dependencies.isEmpty()) {
            Messages.showErrorDialog("Cannot find pitest dependencies", "Error")
            throw IllegalStateException("Cannot find pitest dependencies")
        }
        context.pitestDependencies = dependencies.joinToString(File.pathSeparator)
    }
}