package dev.zhdanov.apps.composeApp.notification

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class NotificationFormTest {

    private enum class Mood { GREAT, TIRED }

    private data class Feedback(val note: String, val mood: Mood)

    @Test
    fun `text input returns raw value or empty string`() {
        val input = TextInput("reply")

        assertEquals("hello", input.parse(mapOf("reply" to "hello")))
        assertEquals("", input.parse(emptyMap()))
    }

    @Test
    fun `form builder maps raw values into result type`() {
        val form = notificationForm {
            val note = text("note")
            val mood = selection("mood", Mood.entries)
            result { values -> Feedback(values[note], values[mood]) }
        }

        assertEquals(listOf("note", "mood"), form.inputs.map { it.id })
        assertEquals(Feedback("focused", Mood.TIRED), form.parse(mapOf("note" to "focused", "mood" to "1")))
    }

    @Test
    fun `selection falls back to default for missing or unknown value`() {
        val input = SelectionInput("mood", Mood.entries, default = Mood.TIRED)

        assertEquals(Mood.TIRED, input.parse(emptyMap()))
        assertEquals(Mood.TIRED, input.parse(mapOf("mood" to "42")))
        assertEquals(Mood.GREAT, input.parse(mapOf("mood" to input.items.first().id)))
    }

    @Test
    fun `form rejects duplicate input ids`() {
        assertFailsWith<IllegalArgumentException> {
            notificationForm {
                val first = text("same")
                val second = text("same")
                result { values -> values[first] + values[second] }
            }
        }
    }
}
