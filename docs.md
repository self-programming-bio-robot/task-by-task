# Структура проекта `task-by-task`

Проект реализует кроссплатформенное приложение для управления задачами с поддержкой Android, iOS, Desktop и серверной части.

---

## Корень проекта
- `.git`, `.idea`, `.gradle`, `.kotlin` — служебные директории для git, IDE и сборки.
- `README.md` — краткое описание проекта.
- `build.gradle.kts`, `settings.gradle.kts`, `gradle.properties` — файлы конфигурации сборки Gradle.
- `gradlew`, `gradlew.bat` — скрипты для запуска Gradle.

---

## Модули

### composeApp
Клиентское приложение на Jetpack Compose Multiplatform.

- `build.gradle.kts` — настройки модуля.
- `src/`
  - `commonMain/`
    - `kotlin/dev/zhdanov/apps/composeApp/`
      - `App.kt` — точка входа UI-приложения.
      - `components/` — переиспользуемые UI-компоненты:
        - `layout/` — адаптивные layout-компоненты.
        - `timer/`, `settings/`, `history/`, `topBar/` — тематические UI-блоки.
      - `screens/` — экраны приложения:
        - `home/`, `tasks/`, `settings/`, `history/`, `feedback/`, `finishedDay/` — отдельные экраны.
      - `navigation/` — навигация между экранами (`MainNavGraph.kt`, `Screen.kt`).
      - `services/` — сервисы для бизнес-логики UI (например, `DaySummaryService.kt`, `TimerSettingsService.kt`).
      - `notification/` — работа с локальными уведомлениями.
      - `di/` — DI-модули (Koin/Hilt и пр.).
  - `androidMain/`, `iosMain/`, `desktopMain/` — платформо-специфичные реализации.

### shared
Общий модуль с бизнес-логикой, моделями и репозиториями.

- `build.gradle.kts` — настройки модуля.
- `src/`
  - `commonMain/`
    - `kotlin/dev/zhdanov/apps/shared/`
      - `Constants.kt`, `Platform.kt` — константы и платформенные утилиты.
      - `model/` — бизнес-модели:
        - `task.kt`, `focus.kt`, `summary.kt`, `timerSettings.kt` — основные сущности.
        - `timer/` — логика таймеров (`PomodoroTimer.kt`, `InfiniteTimer.kt`, `Timer.kt`).
      - `cache/` — кэш, репозитории, работа с БД:
        - `Database.kt`, `mappers.kt` — база и преобразования.
        - `repository/` — репозитории (`TaskRepository.kt`, `TimerSettingRepository.kt`).
      - `prompts/` — шаблоны для генерации подсказок.
      - `utils/` — утилиты (например, работа с датами).
    - `sqldelight/` — схемы для SQLDelight.
  - `androidMain/`, `iosMain/`, `jvmMain/` — платформо-специфичные части.
  - `commonTest/` — общие тесты.

### iosApp
Оболочка для сборки приложения под iOS (Xcode-проект).

- `iosApp.xcodeproj/` — настройки Xcode.
- `iosApp/` — исходники и ресурсы iOS-приложения:
  - `ContentView.swift`, `iOSApp.swift` — точка входа и главный UI.
  - `Assets.xcassets/` — ассеты.
  - `Info.plist` — настройки приложения.
  - `Preview Content/` — предпросмотр.
- `Configuration/` — конфигурационные файлы.

### server
Серверная часть на Kotlin (Ktor или аналог).

- `build.gradle.kts` — настройки модуля.
- `src/main/kotlin/dev/zhdanov/apps/server/`
  - `Application.kt` — точка входа сервера, настройка маршрутов и инициализация.
- `src/main/resources/` — ресурсы сервера (конфиги, статические файлы и пр.).

---

## Кратко о назначении модулей
- **composeApp** — UI и клиентская логика (Jetpack Compose Multiplatform).
- **shared** — бизнес-логика, модели, репозитории, кэш.
- **iosApp** — интеграция с iOS и запуск через Xcode.
- **server** — серверная API и логика (Ktor).

Для подробностей см. README.md и комментарии в исходном коде.
