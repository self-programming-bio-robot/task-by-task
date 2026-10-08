# AGENTS.md

This file provides guidance to Codex (Codex.ai/code) when working with code in this repository.

## Project Overview

Task-by-Task is a Kotlin Multiplatform (KMP) productivity application with timer functionality, task management, and AI-powered daily reviews. It targets Android, iOS, and Desktop platforms using Compose Multiplatform.

## Build Commands

```bash
# Build all modules
./gradlew build

# Android
./gradlew :composeApp:assembleDebug
./gradlew :composeApp:assembleRelease

# Desktop (each OS is a separate application module)
./gradlew :windowsApp:run          # run on Windows
./gradlew :macosApp:run            # run on macOS
./gradlew :linuxApp:run            # run on Linux
./gradlew :windowsApp:packageMsi   # Windows installer
./gradlew :macosApp:packageDmg     # macOS installer
./gradlew :linuxApp:packageDeb     # Linux installer

# Run tests
./gradlew test

# Clean build
./gradlew clean
```

## Technology Stack

Managed via `gradle/libs.versions.toml`:
- Kotlin 2.2.21 (K2 compiler)
- Compose Multiplatform 1.9.3
- Compose Compiler Plugin 2.2.21 (must match Kotlin version)
- Navigation Multiplatform 2.9.1 (type-safe with Kotlin Serialization)
- SQLDelight 2.0.2
- Ktor 3.0.1
- Koin 4.0.0 (DI)
- OpenAI Client 3.8.2 (AI feedback)

## Module Architecture

```
composeApp/     - Shared client library (Android, iOS, Desktop)
  commonMain/   - Shared Compose UI and business logic
  androidMain/  - Android-specific implementations
  desktopMain/  - Shared desktop code + OS contracts (platform/ package)
  iosMain/      - iOS-specific implementations
shared/         - Shared data layer (models, repositories, database)
server/         - Ktor backend server (optional)
androidApp/     - Android application
iosApp/         - Native iOS app wrapper
windowsApp/     - Windows desktop application (MSI, native notifications)
macosApp/       - macOS desktop application (DMG)
linuxApp/       - Linux desktop application (DEB)
```

Desktop OS modules are independent `kotlin("jvm")` applications: each has its own
`main()` calling `runDesktopApp(...)` from `composeApp/desktopMain` and passes a
`DesktopOsServices` implementation (Koin module + `PlatformUi`). OS-specific code
never crosses module boundaries — no runtime OS detection.

### composeApp Structure
- `components/` - Reusable UI components (timer, settings, history)
- `screens/` - Main application screens
- `services/` - Business logic and platform services
- `navigation/` - Type-safe navigation with @Serializable routes

### shared Structure
- `model/` - Business entities (Task, Focus, Timer, Summary)
- `cache/` - Database operations and repositories
- Platform-specific drivers for SQLDelight (Android, iOS, JVM)

## Key Architectural Patterns

- **MVVM** with `lifecycle-viewmodel-compose`
- **Repository Pattern** for data access via SQLDelight
- **Dependency Injection** with Koin (modules in commonMain)
- **Type-Safe Navigation**: Routes are `@Serializable` data classes, not strings
- **expect/actual** for platform-specific implementations

## Navigation

Uses Navigation Multiplatform 2.9.1 with type-safe routes:

```kotlin
@Serializable
data class ProfileRoute(val userId: Int)

// Navigate
navController.navigate(ProfileRoute(123))

// Receive
val route = backStackEntry.toRoute<ProfileRoute>()
```

Requires `kotlinx-serialization` plugin.

## Platform-Specific Notes

### Desktop
- Uses `jSystemThemeDetector` for system theme detection
- One application module per OS: `windowsApp`, `macosApp`, `linuxApp`
- `compose.desktop.application` configured in each app module (Msi/Dmg/Deb)
- Main classes: `dev.zhdanov.apps.<os>App.MainKt`
- OS contracts (`PlatformUi`, `DesktopOsServices`) live in `composeApp/src/desktopMain/.../platform/`
- `NotificationService` is a contract in `commonMain`; each OS module binds its own implementation in `koinModule` (`WindowsNotificationService`, reusable `TrayNotificationService` from `desktopMain`)

### iOS
- Static framework output (`isStatic = true`)
- No bitcode embedding (removed in Kotlin 2.2.x)
- `iosApp/` contains the native iOS application entry point

### Android
- minSdk 24, targetSdk 34, compileSdk 34
- JVM target 11

## KMP Version Constraints

When updating versions:
- Compose Compiler Plugin version must **exactly match** Kotlin version
- Kotlin Serialization Plugin version must match Kotlin version
- Language version must be 1.8+ (recommended 2.0+)
- `embedBitcode` is not supported (removed in Kotlin 2.2.0+)

## Dependencies

All dependencies are centralized in `gradle/libs.versions.toml`. Use version references (`libs.xxx`) rather than hardcoded versions in module build files.