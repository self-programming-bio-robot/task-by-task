package dev.zhdanov.apps.composeApp.notification

/** Windows toasts accept at most 5 inputs; the shared contract follows the strictest platform. */
private const val MAX_NOTIFICATION_INPUTS = 5

/**
 * Interactive part of a notification: the inputs to render and how the raw
 * values reported by the OS (input id → string) are turned into a typed [R].
 *
 * A single [NotificationInput] is already a form of its own value type; several
 * inputs are combined with [notificationForm].
 */
sealed interface NotificationForm<out R> {
    val inputs: List<NotificationInput<*>>

    fun parse(values: Map<String, String>): R
}

sealed class NotificationInput<out T>(val id: String) : NotificationForm<T> {

    override val inputs: List<NotificationInput<*>> get() = listOf(this)

    override fun parse(values: Map<String, String>): T = parseValue(values[id])

    internal abstract fun parseValue(raw: String?): T
}

class TextInput(
    id: String,
    val placeholder: String? = null,
    val title: String? = null,
) : NotificationInput<String>(id) {

    override fun parseValue(raw: String?): String = raw.orEmpty()
}

/** Pick one of [options]; the result is the option itself, not its label. */
class SelectionInput<E>(
    id: String,
    val options: List<E>,
    val title: String? = null,
    val default: E = options.first(),
    label: (E) -> String = { it.toString() },
) : NotificationInput<E>(id) {

    init {
        require(default in options) { "Default value of '$id' must be one of the options" }
    }

    val items: List<SelectionItem> = options.mapIndexed { index, option ->
        SelectionItem(index.toString(), label(option))
    }

    val defaultItemId: String = options.indexOf(default).toString()

    override fun parseValue(raw: String?): E =
        raw?.toIntOrNull()?.let(options::getOrNull) ?: default
}

data class SelectionItem(val id: String, val label: String)

/**
 * Builds a multi-input form whose result type is inferred from [NotificationFormBuilder.result]:
 *
 * ```
 * val form = notificationForm {
 *     val note = text("note")
 *     val mood = selection("mood", Mood.entries)
 *     result { values -> Feedback(values[note], values[mood]) }
 * } // NotificationForm<Feedback>
 * ```
 */
fun <R> notificationForm(block: NotificationFormBuilder.() -> FormResult<R>): NotificationForm<R> =
    NotificationFormBuilder().run { build(block()) }

class NotificationFormBuilder internal constructor() {

    private val inputs = mutableListOf<NotificationInput<*>>()

    fun text(id: String, placeholder: String? = null, title: String? = null): Field<String> =
        add(TextInput(id, placeholder, title))

    fun <E> selection(
        id: String,
        options: List<E>,
        title: String? = null,
        default: E = options.first(),
        label: (E) -> String = { it.toString() },
    ): Field<E> = add(SelectionInput(id, options, title, default, label))

    fun <R> result(map: (FormValues) -> R): FormResult<R> = FormResult(map)

    private fun <T> add(input: NotificationInput<T>): Field<T> {
        require(inputs.none { it.id == input.id }) { "Duplicate notification input id '${input.id}'" }
        inputs += input
        return Field(input)
    }

    internal fun <R> build(result: FormResult<R>): NotificationForm<R> {
        require(inputs.isNotEmpty()) { "Notification form must have at least one input" }
        require(inputs.size <= MAX_NOTIFICATION_INPUTS) {
            "Notification form supports at most $MAX_NOTIFICATION_INPUTS inputs"
        }
        return BuiltForm(inputs.toList(), result.map)
    }
}

/** Typed handle to an input declared in [NotificationFormBuilder]. */
class Field<out T> internal constructor(internal val input: NotificationInput<T>)

class FormResult<R> internal constructor(internal val map: (FormValues) -> R)

class FormValues internal constructor(
    private val inputs: List<NotificationInput<*>>,
    private val values: Map<String, String>,
) {
    operator fun <T> get(field: Field<T>): T {
        require(field.input in inputs) { "Field '${field.input.id}' doesn't belong to this form" }
        return field.input.parseValue(values[field.input.id])
    }
}

private class BuiltForm<R>(
    override val inputs: List<NotificationInput<*>>,
    private val map: (FormValues) -> R,
) : NotificationForm<R> {

    override fun parse(values: Map<String, String>): R = map(FormValues(inputs, values))
}
