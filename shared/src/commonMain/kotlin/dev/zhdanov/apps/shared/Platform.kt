package dev.zhdanov.apps.shared

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform


