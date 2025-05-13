package dev.zhdanov.apps.composeApp.services

/**
 * Сервис для подписки на изменения системной темы приложения
 */
interface ThemeChangeService {
    /**
     * Тип системной темы
     */
    enum class SystemTheme {
        LIGHT, DARK
    }

    /**
     * Listener для смены темы. theme — текущая системная тема
     */
    fun interface ThemeChangeListener {
        fun onThemeChanged(theme: SystemTheme)
    }

    /**
     * Зарегистрировать слушатель изменений темы
     */
    fun registerListener(listener: ThemeChangeListener)

    /**
     * Удалить слушатель изменений темы
     */
    fun removeListener(listener: ThemeChangeListener)
}
