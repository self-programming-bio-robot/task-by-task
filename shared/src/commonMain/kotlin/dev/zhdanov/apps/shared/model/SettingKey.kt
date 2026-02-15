package dev.zhdanov.apps.shared.model

enum class SettingKey(val id: Long) {
    OPENAI_TOKEN(1),
    THEME(2),
    FOCUSED_TASK_ID(3),
    START_OF_DAY(4),
}
