import com.github.jaksonlin.pitestintellij.ui.PitestOutputDialog
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.Messages
import com.github.jaksonlin.pitestintellij.context.PitestContext
import com.github.jaksonlin.pitestintellij.context.dumpPitestContext
import com.intellij.openapi.application.ModalityState
import java.util.concurrent.atomic.AtomicReference

abstract class PitestCommand(protected val project: Project, protected val context: PitestContext) {
    abstract fun execute()

    protected fun showInputDialog(message: String, title: String): String? {
        val result = AtomicReference<String?>()
        ApplicationManager.getApplication().invokeAndWait({
            result.set(Messages.showInputDialog(project, message, title, Messages.getQuestionIcon()))
        }, ModalityState.defaultModalityState())
        return result.get()
    }

    protected fun showOutput(output: String, title: String) {
        ApplicationManager.getApplication().invokeLater {
            PitestOutputDialog(project, output, title).show()
        }
    }
    
    protected fun showError(message: String) {
        ApplicationManager.getApplication().invokeLater {
            val contextState = dumpPitestContext(context)
            val messageWithContextState = "$message\n\n$contextState"
            Messages.showErrorDialog(project, messageWithContextState, "Pitest Error")
        }
    }
}