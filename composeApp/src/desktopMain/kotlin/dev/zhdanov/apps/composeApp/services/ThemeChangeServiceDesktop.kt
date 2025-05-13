package dev.zhdanov.apps.composeApp.services

import java.util.concurrent.CopyOnWriteArraySet
import com.jthemedetecor.OsThemeDetector

class ThemeChangeServiceDesktop : ThemeChangeService {

    private val listeners = CopyOnWriteArraySet<ThemeChangeService.ThemeChangeListener>()

    init {
        val detector = OsThemeDetector.getDetector()
        detector.registerListener { isDark ->
            notifyThemeChanged(
                if (isDark)
                    ThemeChangeService.SystemTheme.DARK
                else
                    ThemeChangeService.SystemTheme.LIGHT
            )
        }
    }

    override fun registerListener(listener: ThemeChangeService.ThemeChangeListener) {
        listeners.add(listener)
    }

    override fun removeListener(listener: ThemeChangeService.ThemeChangeListener) {
        listeners.remove(listener)
    }

    fun notifyThemeChanged(theme: ThemeChangeService.SystemTheme) {
        listeners.forEach { it.onThemeChanged(theme) }
    }
}
