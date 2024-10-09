import com.github.jaksonlin.pitestintellij.commands.PitestContext
import com.github.jaksonlin.pitestintellij.util.FileUtils
import com.github.jaksonlin.pitestintellij.util.GradleUtils
import com.github.jaksonlin.pitestintellij.util.JavaFileProcessor
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.PathManager
import com.intellij.openapi.application.ReadAction
import com.intellij.openapi.application.WriteAction
import com.intellij.openapi.module.ModuleManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.ModuleRootManager
import com.intellij.openapi.roots.ProjectRootManager
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.vfs.VirtualFile
import java.io.File
import java.nio.file.Path
import java.nio.file.Paths

class PrepareEnvironmentCommand(project: Project, context: PitestContext) : PitestCommand(project, context) {
    private val javaFileProcessor = JavaFileProcessor()

    override fun execute() {
        val testVirtualFile = ReadAction.compute<VirtualFile?, Throwable> {
            context.testVirtualFile
        }
        if (testVirtualFile == null) {
            showError("Cannot find test file")
            throw IllegalStateException("Cannot find test file")
        }
        val targetTestClassFilePath = testVirtualFile.path

        collectTargetTestClassName(targetTestClassFilePath)
        collectJavaInfo(testVirtualFile)
        collectSourceRoots()
        collectResourceDirectories()
        
        collectTargetClassThatWeTest()
        prepareReportDirectory()
        setupPitestLibDependencies()
        collectClassPathFileForPitest()
    }

    private fun collectTargetTestClassName(targetTestCalssFilePath:String) {

        context.fullyQualifiedTargetTestClassName = javaFileProcessor.getFullyQualifiedName(targetTestCalssFilePath)

        if (context.fullyQualifiedTargetTestClassName.isNullOrBlank()) {
            showError("Cannot get fully qualified name for target test class")
            throw IllegalStateException("Cannot get fully qualified name for target test class")
        }
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
        context.sourceRoots = ReadAction.compute<List<Path>, Throwable> {
            ModuleManager.getInstance(project).modules.flatMap { module ->
                ModuleRootManager.getInstance(module).contentRoots.map { contentRoot ->
                    Paths.get(contentRoot.path)
                }
            }
        }
    }

    private fun collectTargetClassThatWeTest() {
        // The user input dialog and file operations don't need to be in ReadAction
        val targetClass = showInputDialog("Please enter the name of the class that you want to test", "Enter target class")
        if (targetClass.isNullOrBlank()) {
            return
        }
        val targetClassInfo = FileUtils.findTargetClassFile(context.sourceRoots, targetClass)
       if (targetClassInfo == null) {
           showError("Cannot find target class file")
           throw IllegalStateException("Cannot find target class file")
       }
        context.fullyQualifiedTargetClassName = javaFileProcessor.getFullyQualifiedName(targetClassInfo.file.toString())
        if (context.fullyQualifiedTargetClassName.isNullOrBlank()) {
            showError("Cannot get fully qualified name for target class")
            throw IllegalStateException("Cannot get fully qualified name for target class")
        }
        context.targetClassSourceRoot = targetClassInfo.sourceRoot.toString()
    }
    private fun prepareReportDirectory(){
        // prepare the report directory
        ApplicationManager.getApplication().runReadAction {
            context.reportDirectory = Paths.get(project.basePath!!, "build", "reports", "pitest").toString()
            File(context.reportDirectory!!).mkdirs()
        }
    }

    private fun collectResourceDirectories(){
        val resourceDirectories = ReadAction.compute<List<String>, Throwable> {
            GradleUtils.getResourceDirectories(project)
        }
        context.resourceDirectories = resourceDirectories
    }

    private fun collectClassPathFileForPitest(){
        if (context.resourceDirectories == null) {
            collectResourceDirectories()
        }
        val classPathFileContent = ReadAction.compute<String, Throwable> {
            val classpath = GradleUtils.getCompilationOutputPaths(project)
            val testDependencies = GradleUtils.getTestRunDependencies(project)
            val allDependencies = ArrayList<String>()
            allDependencies.addAll(classpath)
            if (context.resourceDirectories != null) {
                allDependencies.addAll(context.resourceDirectories!!)
            }
            allDependencies.addAll(testDependencies)
            val classpathFile = Paths.get(project.basePath!!, "build", "reports", "pitest", "classpath.txt").toString()
            context.classpathFile = classpathFile
            allDependencies.joinToString("\n")
        }
        showOutput("Classpath file content: $classPathFileContent", "Classpath file content")
        File(context.classpathFile!!).writeText(classPathFileContent)
    }

    private fun setupPitestLibDependencies(){
        if (context.resourceDirectories == null) {
            collectResourceDirectories()
        }
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
        dependencies.addAll(context.resourceDirectories!!)
        if (dependencies.isEmpty()) {
            Messages.showErrorDialog("Cannot find pitest dependencies", "Error")
            throw IllegalStateException("Cannot find pitest dependencies")
        }
        context.pitestDependencies = dependencies.joinToString(File.pathSeparator)
    }
}