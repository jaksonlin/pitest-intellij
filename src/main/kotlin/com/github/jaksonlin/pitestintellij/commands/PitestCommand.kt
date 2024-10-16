import com.github.jaksonlin.pitestintellij.commands.CommandCancellationException
import com.github.jaksonlin.pitestintellij.ui.PitestOutputDialog
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.Messages
import com.github.jaksonlin.pitestintellij.context.PitestContext
import com.github.jaksonlin.pitestintellij.context.dumpPitestContext
import com.github.jaksonlin.pitestintellij.services.PitestService
import com.github.jaksonlin.pitestintellij.services.RunHistoryManager
import com.intellij.openapi.application.ModalityState
import com.intellij.openapi.components.service
import com.intellij.openapi.ui.DialogWrapper
import java.util.concurrent.CompletableFuture
import java.util.concurrent.atomic.AtomicReference

abstract class PitestCommand(protected val project: Project, protected val context: PitestContext) {
    abstract fun execute()
    protected val runHistoryManager = project.service<RunHistoryManager>()
    protected fun showInputDialog(message: String, title: String): String? {
        val result = AtomicReference<String?>()
        ApplicationManager.getApplication().invokeAndWait({
            result.set(Messages.showInputDialog(project, message, title, Messages.getQuestionIcon()))
        }, ModalityState.defaultModalityState())
        return result.get()
    }

    protected fun showOutput(output: String, title: String) {
        val future = CompletableFuture<Void>()
        ApplicationManager.getApplication().invokeLater {
            val dialog = PitestOutputDialog(project, output, title)
            dialog.show()
            if (dialog.exitCode == DialogWrapper.CANCEL_EXIT_CODE) {
                future.completeExceptionally(CommandCancellationException("User cancelled the dialog"))
            } else {
                future.complete(null)
            }
        }
        future.get()
    }
    
    protected fun showError(message: String) {
        ApplicationManager.getApplication().invokeLater {
            val contextState = dumpPitestContext(context)
            val messageWithContextState = "$message\n\n$contextState"
            Messages.showErrorDialog(project, messageWithContextState, "Pitest Error")
        }
    }
}