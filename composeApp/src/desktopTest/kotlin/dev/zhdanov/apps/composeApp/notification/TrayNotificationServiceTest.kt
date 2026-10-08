package dev.zhdanov.apps.composeApp.notification

import androidx.compose.ui.window.TrayState
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

class TrayNotificationServiceTest {

    @Test
    fun `form is answered as unsupported exactly once`() = runTest {
        val service = TrayNotificationService(TrayState())
        val responses = mutableListOf<NotificationResponse<String>>()

        service.addNotification(text = "question", form = TextInput("reply")) { responses += it }

        assertEquals(listOf<NotificationResponse<String>>(NotificationResponse.Unsupported), responses)
    }
}
