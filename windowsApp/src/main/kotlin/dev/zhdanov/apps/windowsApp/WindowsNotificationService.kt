package dev.zhdanov.apps.windowsApp

import com.diamondedge.logging.logging
import dev.nucleusframework.notification.windows.DismissalReason
import dev.nucleusframework.notification.windows.ShortcutPolicy
import dev.nucleusframework.notification.windows.ToastActionsBuilder
import dev.nucleusframework.notification.windows.ToastContent
import dev.nucleusframework.notification.windows.ToastNotificationListener
import dev.nucleusframework.notification.windows.WindowsNotificationCenter
import dev.nucleusframework.notification.windows.toast
import dev.zhdanov.apps.composeApp.notification.NotificationForm
import dev.zhdanov.apps.composeApp.notification.NotificationInput
import dev.zhdanov.apps.composeApp.notification.NotificationResponse
import dev.zhdanov.apps.composeApp.notification.NotificationService
import dev.zhdanov.apps.composeApp.notification.SelectionInput
import dev.zhdanov.apps.composeApp.notification.TextInput
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

/**
 * Native WinRT toasts via Nucleus (AWT tray balloons are dead on Windows 11),
 * with inputs and Action Center support. Falls back to [fallback] when native
 * toasts are unavailable or fail to show.
 */
class WindowsNotificationService(
    private val fallback: NotificationService,
) : NotificationService, AutoCloseable {

    private val scope = CoroutineScope(
        SupervisorJob() + Dispatchers.Default + CoroutineExceptionHandler { _, error ->
            logger.e(error) { "Notification result callback failed" }
        }
    )

    /** Toasts awaiting an answer, keyed by toast tag. Removal guarantees a single answer. */
    private val pending = ConcurrentHashMap<String, PendingToast>()

    private val listener = object : ToastNotificationListener {
        override fun onActivated(
            tag: String,
            group: String,
            arguments: String,
            userInputs: Map<String, String>,
        ) {
            val toast = pending.remove(tag) ?: return
            // Clicking the toast body (not the submit button) closes it without an answer.
            if (arguments == SUBMIT_ARGUMENTS) toast.onSubmitted(userInputs) else toast.onDismissed()
        }

        override fun onDismissed(tag: String, group: String, reason: DismissalReason) {
            // TIMED_OUT: the toast moved to Action Center and can still be answered.
            if (reason != DismissalReason.TIMED_OUT) pending.remove(tag)?.onDismissed()
        }

        override fun onFailed(tag: String, group: String, errorCode: Int) {
            logger.w { "Windows toast failed: tag=$tag errorCode=$errorCode" }
            pending.remove(tag)?.onFailed()
        }
    }

    private val ready: Boolean = runCatching {
        WindowsNotificationCenter.initialize(
            aumid = AUMID,
            appName = APP_NAME,
            shortcutPolicy = ShortcutPolicy.REQUIRE_CREATE,
        ).also { if (it) WindowsNotificationCenter.addListener(listener) }
    }.onFailure { logger.w(it) { "Windows notification init failed" } }
        .getOrDefault(false)
        .also { if (!it) logger.w { "Windows notifications unavailable, falling back to tray" } }

    override suspend fun addNotification(text: String, title: String) {
        if (!ready || !show(toastContent(title, text, form = null, submitLabel = ""), tag = "")) {
            fallback.addNotification(text, title)
        }
    }

    override suspend fun <R> addNotification(
        text: String,
        title: String,
        form: NotificationForm<R>,
        submitLabel: String,
        onResult: suspend (NotificationResponse<R>) -> Unit,
    ) {
        val showFallback = suspend { fallback.addNotification(text, title, form, submitLabel, onResult) }
        if (!ready) return showFallback()

        val tag = UUID.randomUUID().toString()
        pending[tag] = PendingToast(
            onSubmitted = { values -> scope.launch { onResult(NotificationResponse.Submitted(form.parse(values))) } },
            onDismissed = { scope.launch { onResult(NotificationResponse.Dismissed) } },
            onFailed = { scope.launch { showFallback() } },
        )
        if (!show(toastContent(title, text, form, submitLabel), tag) && pending.remove(tag) != null) {
            showFallback()
        }
    }

    override fun close() {
        if (ready) {
            runCatching { WindowsNotificationCenter.removeListener(listener) }
            runCatching { WindowsNotificationCenter.uninitialize() }
        }
    }

    private fun show(content: ToastContent, tag: String): Boolean = runCatching {
        WindowsNotificationCenter.show(content, tag = tag) { error ->
            if (error != null) {
                logger.w { "Windows toast failed: $error" }
                pending.remove(tag)?.onFailed()
            }
        }
    }.isSuccess

    private fun toastContent(title: String, text: String, form: NotificationForm<*>?, submitLabel: String) =
        toast {
            visual {
                text(title)
                text(text)
            }
            if (form != null) {
                actions {
                    form.inputs.forEach { input(it) }
                    button(
                        submitLabel,
                        arguments = SUBMIT_ARGUMENTS,
                        // A lone text box gets the button inline, like a chat reply.
                        inputId = (form.inputs.singleOrNull() as? TextInput)?.id,
                    )
                }
            }
        }

    private fun ToastActionsBuilder.input(input: NotificationInput<*>) = when (input) {
        is TextInput -> textBox(input.id, title = input.title, placeholder = input.placeholder)
        is SelectionInput<*> -> selectionBox(
            input.id,
            title = input.title,
            defaultSelectionId = input.defaultItemId,
        ) {
            input.items.forEach { item(it.id, it.label) }
        }
    }

    private class PendingToast(
        val onSubmitted: (Map<String, String>) -> Unit,
        val onDismissed: () -> Unit,
        val onFailed: () -> Unit,
    )

    private companion object {
        const val AUMID = "dev.zhdanov.TaskByTask"
        const val APP_NAME = "TaskByTask"
        const val SUBMIT_ARGUMENTS = "action=submit"
        val logger = logging(WindowsNotificationService::class.qualifiedName)
    }
}
