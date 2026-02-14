# **Эволюция Архитектуры и Миграция на Navigation Multiplatform 3: Исчерпывающее Техническое Руководство**

## **Аннотация**

В данном отчете представлен глубокий технический анализ библиотеки Navigation 3 (Nav3) в контексте экосистемы Kotlin Multiplatform (KMP). Документ служит всеобъемлющим руководством по миграции с предыдущих версий навигационных библиотек (в частности, Navigation 2.x), детально рассматривая архитектурные сдвиги, внедрение типобезопасности, управление состоянием и адаптивные интерфейсы. Отчет предназначен для старших разработчиков и архитекторов мобильных систем, стремящихся к построению масштабируемых, кроссплатформенных приложений с общим UI и бизнес-логикой. Мы исследуем, как Nav3 трансформирует парадигму навигации от императивного управления контроллерами к декларативному управлению состоянием, где разработчик получает полный контроль над стеком возврата (BackStack).

## ---

**1\. Введение: Смена Парадигм в Навигации Мобильных Приложений**

Развитие инструментов разработки пользовательских интерфейсов за последнее десятилетие прошло путь от жестко связанных императивных структур (Activity, Fragments, UIViewController) к гибким декларативным системам (Jetpack Compose, SwiftUI). Однако навигационные библиотеки долгое время оставались "черными ящиками", скрывающими состояние переходов и стека экранов внутри себя. Выход **Navigation 3** знаменует собой фундаментальный сдвиг в этой области, предлагая модель, где навигация является чистой функцией от состояния приложения.1

### **1.1. От Navigation 2 к Navigation 3**

Традиционная библиотека Jetpack Navigation (Nav2) и её адаптации для Compose сыграли важную роль в стандартизации навигации. Однако они страдали от ряда архитектурных ограничений:

1. **Централизация власти в NavController:** Разработчик запрашивал навигацию, но не владел состоянием стека напрямую. Это затрудняло сложные манипуляции с историей переходов.  
2. **Проблемы с адаптивностью:** NavHost в Nav2 был спроектирован для отображения одного активного пункта назначения, что создавало значительные трудности при реализации паттернов List-Detail на планшетах и десктопах.1  
3. **Позднее внедрение типобезопасности:** Типобезопасные маршруты появились только в версии 2.8.0, в то время как Nav3 построен на них изначально.3

Navigation 3 решает эти проблемы, вводя концепцию "Вы владеете стеком возврата" (You own the back stack). Это означает, что стек навигации — это просто коллекция данных (обычно список), изменением которой управляет само приложение, а библиотека лишь реактивно отображает это состояние.5

### **1.2. Контекст Kotlin Multiplatform**

В среде KMP потребность в гибкой навигации ощущается особенно остро. Приложения должны корректно работать не только на Android и iOS, но и на Desktop (Windows, macOS, Linux) и Web (Wasm/JS). Navigation 3, поддерживаемая JetBrains (через форк androidx.navigation3), предоставляет унифицированный API для всех этих платформ, позволяя переиспользовать до 99% кода навигации.7

## ---

**2\. Архитектурные Примитивы Navigation 3**

Прежде чем приступать к миграции, необходимо детально разобрать строительные блоки Nav3. Понимание этих примитивов критически важно, так как они заменяют привычные концепции NavGraph и NavDestination.

### **2.1. NavKey: Единица Идентификации**

В основе Nav3 лежит интерфейс NavKey. Это маркер, который идентифицирует уникальное состояние или экран в приложении. В отличие от строковых маршрутов в ранних версиях Nav2, NavKey — это строго типизированный объект.

Для обеспечения сохранения состояния при смерти процесса (Android) или перезагрузке страницы (Web), реализации NavKey должны быть сериализуемыми. Это достигается с помощью плагина kotlinx.serialization.3

**Пример архитектурного определения:**

Kotlin

import androidx.navigation3.runtime.NavKey  
import kotlinx.serialization.Serializable

@Serializable  
sealed interface AppRoute : NavKey {  
    // Простой маршрут без аргументов  
    @Serializable  
    data object Dashboard : AppRoute

    // Маршрут с аргументами (примитивы и сложные типы)  
    @Serializable  
    data class TransactionDetails(  
        val transactionId: String,  
        val amount: Double,  
        val currency: String  
    ) : AppRoute  
}

*Анализ:* Использование sealed interface позволяет компилятору Kotlin проверять исчерпываемость (exhaustiveness) веток when при рендеринге UI, что исключает возможность появления "необработанных" экранов в runtime.10

### **2.2. NavEntry: Контейнер Контента**

NavEntry — это сущность, которая связывает NavKey с конкретным UI-контентом (Composable функцией). NavEntry также служит точкой привязки для Lifecycle, ViewModelStore и SavedStateRegistry. Это критически важный момент: именно NavEntry определяет область видимости (scope) для ViewModels. Если ViewModel привязана к NavEntry, она будет очищена из памяти ровно в тот момент, когда соответствующий ключ будет удален из стека.11

### **2.3. EntryProvider: Фабрика Экранов**

Вместо монолитного NavHost, Nav3 использует EntryProvider. Это функциональный интерфейс (или DSL), который принимает NavKey и возвращает NavEntry. Это позволяет децентрализовать логику создания экранов, разбивая её на модули.13

Kotlin

val featureProvider \= entryProvider {  
    entry\<AppRoute.Dashboard\> {  
        DashboardScreen()  
    }  
    entry\<AppRoute.TransactionDetails\> { key \-\>  
        // Аргументы доступны напрямую из ключа, типизировано  
        TransactionScreen(id \= key.transactionId, amount \= key.amount)  
    }  
}

### **2.4. NavDisplay: Реактивный Рендерер**

NavDisplay заменяет NavHost. Его задача — отображать состояние. Он принимает список ключей (back stack) и EntryProvider. При изменении списка NavDisplay автоматически перестраивает UI, применяя анимации переходов.2

## ---

**3\. Подготовка Инфраструктуры KMP**

Переход на Nav3 требует тщательной настройки системы сборки Gradle, особенно учитывая, что библиотека находится в активной разработке и распространяется через специфические артефакты JetBrains для мультиплатформенной поддержки.

### **3.1. Управление Зависимостями (Version Catalog)**

Рекомендуется использовать libs.versions.toml для централизованного управления версиями. На текущий момент (по состоянию на конец 2025 года) актуальны Alpha/Beta версии библиотек JetBrains.14

**Таблица 1: Необходимые артефакты для Navigation 3 в KMP**

| Библиотека | Координаты Maven | Назначение |
| :---- | :---- | :---- |
| **Nav3 Runtime** | org.jetbrains.androidx.navigation3:navigation3-runtime | Базовые интерфейсы NavKey, EntryProvider |
| **Nav3 UI** | org.jetbrains.androidx.navigation3:navigation3-ui | Компонент NavDisplay |
| **ViewModel Support** | org.jetbrains.androidx.lifecycle:lifecycle-viewmodel-navigation3 | Скоупинг ViewModel к NavEntry 15 |
| **Adaptive UI** | org.jetbrains.compose.material3.adaptive:adaptive-navigation3 | Поддержка адаптивных лейаутов |
| **Serialization** | org.jetbrains.kotlinx:kotlinx-serialization-json | Сериализация ключей навигации |

**Конфигурация TOML файла:**

Ini, TOML

\[versions\]  
kotlin \= "2.1.0"  
compose-multiplatform \= "1.10.0"  
navigation3 \= "1.0.0-alpha06"   
lifecycle \= "2.10.0-alpha06"

\[libraries\]  
androidx-navigation3-runtime \= { module \= "org.jetbrains.androidx.navigation3:navigation3-runtime", version.ref \= "navigation3" }  
androidx-navigation3-ui \= { module \= "org.jetbrains.androidx.navigation3:navigation3-ui", version.ref \= "navigation3" }  
androidx-lifecycle-viewmodel-nav3 \= { module \= "org.jetbrains.androidx.lifecycle:lifecycle-viewmodel-navigation3", version.ref \= "navigation3" }

### **3.2. Настройка Gradle модуля**

В файле build.gradle.kts вашего общего модуля (composeApp или shared) необходимо подключить зависимости в commonMain source set. Обратите внимание, что для корректной работы на Android может потребоваться повышение compileSdk до 35 или 36\.9

Kotlin

kotlin {  
    androidTarget {  
        compilerOptions {  
            jvmTarget.set(JvmTarget.JVM\_17)  
        }  
    }  
      
    sourceSets {  
        commonMain.dependencies {  
            implementation(compose.runtime)  
            implementation(compose.foundation)  
              
            // Navigation 3  
            implementation(libs.androidx.navigation3.runtime)  
            implementation(libs.androidx.navigation3.ui)  
            implementation(libs.androidx.lifecycle.viewmodel.nav3)  
              
            // Serialization plugin is mandatory  
            implementation(libs.kotlinx.serialization.json)  
        }  
    }  
}

android {  
    compileSdk \= 36  
    //...  
}

## ---

**4\. Стратегия Миграции: Пошаговый Туториал**

Миграция с Navigation 2 (даже с версии 2.8.0 с Type Safe) на Navigation 3 — это процесс, требующий рефакторинга управления состоянием. Мы разделим этот процесс на логические этапы.

### **Этап 1: Трансформация Маршрутов (Routes Refactoring)**

Если вы использовали строковые маршруты ("home/{id}"), вам необходимо полностью переписать их на классы. Если вы уже использовали Type Safe Navigation (Nav 2.8+), изменения минимальны: нужно добавить наследование от интерфейса NavKey.

**До (Nav 2.8 Type Safe):**

Kotlin

@Serializable  
data object Home

@Serializable  
data class Profile(val id: String)

**После (Nav 3):**

Kotlin

import androidx.navigation3.runtime.NavKey

@Serializable  
data object Home : NavKey

@Serializable  
data class Profile(val id: String) : NavKey

*Важно:* Убедитесь, что все классы маршрутов аннотированы @Serializable. Это требование обязательно для работы механизма сохранения состояния.9

### **Этап 2: Создание Владельца Состояния (State Holder Implementation)**

В Nav2 состояние хранилось внутри NavHostController. В Nav3 вы создаете его сами. Для KMP приложений лучшей практикой является вынос управления навигацией в ViewModel или специальный класс Navigator, который находится в общем коде (commonMain).

Используйте mutableStateListOf или SnapshotStateList для хранения стека. Это обеспечит реактивность UI при изменении навигации.5

Kotlin

class RootNavigationViewModel : ViewModel() {  
    // Начальное состояние  
    val backStack \= mutableStateListOf\<NavKey\>(AppRoute.Home)

    fun navigateTo(key: NavKey) {  
        // Простой push в стек  
        backStack.add(key)  
    }

    fun goBack() {  
        // Удаление верхнего элемента  
        backStack.removeLastOrNull()  
    }

    fun replaceRoot(key: NavKey) {  
        // Полная замена стека (например, после логина)  
        backStack.clear()  
        backStack.add(key)  
    }  
      
    // Поддержка "Up" навигации для Deep Links  
    fun handleDeepLink(chain: List\<NavKey\>) {  
        backStack.clear()  
        backStack.addAll(chain)  
    }  
}

*Инсайт:* Такой подход делает навигацию абсолютно тестируемой. Вы можете написать Unit-тест, который вызывает navigateTo и проверяет содержимое списка backStack, не запуская эмуляторы и UI-тесты.

### **Этап 3: Замена UI-слоя (From NavHost to NavDisplay)**

Это самый визуально заметный этап миграции. NavHost заменяется на комбинацию NavDisplay и entryProvider.

**Сравнение реализаций:**

| Компонент | Navigation 2 Code | Navigation 3 Code |
| :---- | :---- | :---- |
| **Определение** | NavHost(navController, startDestination \= Home) {... } | NavDisplay(backStack \= viewModel.backStack, entryProvider \=...) {... } |
| **Экраны** | composable\<Home\> {... } | entry\<Home\> {... } |
| **Аргументы** | val args \= entry.toRoute\<Profile\>() | entry\<Profile\> { key \-\>... } (key уже типизирован) |

**Реализация в Compose Multiplatform:**

Kotlin

@Composable  
fun AppContent() {  
    val viewModel \= viewModel\<RootNavigationViewModel\> { RootNavigationViewModel() }  
      
    // Используем rememberNavBackStack для автоматического сохранения состояния  
    // Если мы не используем ViewModel для хранения, то:  
    // val backStack \= rememberNavBackStack\<NavKey\>(AppRoute.Home)  
      
    NavDisplay(  
        backStack \= viewModel.backStack,  
        onBack \= { viewModel.goBack() },  
        entryProvider \= entryProvider {  
            entry\<AppRoute.Home\> {  
                HomeScreen(  
                    onNavigateToProfile \= { id \-\> viewModel.navigateTo(AppRoute.Profile(id)) }  
                )  
            }  
              
            entry\<AppRoute.Profile\> { key \-\>  
                // Прямой доступ к полям data class'а  
                ProfileScreen(userId \= key.id)  
            }  
        },  
        // Важно: декораторы для корректной работы ViewModel и SaveableState  
        entryDecorators \= listOf(  
            rememberSaveableStateHolderNavEntryDecorator(),  
            rememberViewModelStoreNavEntryDecorator()  
        )  
    )  
}

*Критическое замечание:* Без добавления rememberViewModelStoreNavEntryDecorator в список entryDecorators, ViewModels, вызываемые внутри экранов, не будут привязаны к жизненному циклу навигационной записи. Это приведет к тому, что ViewModels не будут очищаться при нажатии "Назад", вызывая утечки памяти.12

## ---

**5\. Глубокие Ссылки (Deep Links): Ручное Управление**

Одной из самых сложных областей при переходе на Nav3 является обработка Deep Links. В Nav2 библиотека брала на себя парсинг URI и сопоставление его с графом. В Nav3, поскольку "вы владеете стеком", вы также несете ответственность за парсинг внешних событий в список ключей.18

Это изменение дает огромную гибкость, но требует написания дополнительного кода.

### **5.1. Алгоритм Обработки Deep Link**

Процесс можно разбить на четыре шага:

1. **Определение паттернов:** Создание маппинга между URI-шаблонами и классами NavKey.  
2. **Парсинг:** Преобразование входящего URI (строки) в структурированные данные.  
3. **Матчинг (Matching):** Сопоставление данных с паттернами.  
4. **Синтез стека (Stack Synthesis):** Создание целого списка ключей для воссоздания истории переходов.19

### **5.2. Реализация Парсера**

В KMP проекте парсинг URI должен происходить в общем коде, но получение самого URI зависит от платформы.

Kotlin

// Common Code  
object DeepLinkHandler {  
    fun parse(uri: String): List\<NavKey\> {  
        val segments \= uri.split("/").filter { it.isNotEmpty() }  
          
        return when {  
            // Пример: myapp://profile/123  
            segments.size \>= 2 && segments \== "profile" \-\> {  
                val userId \= segments  
                // Синтетический стек: Главная \-\> Профиль  
                listOf(AppRoute.Home, AppRoute.Profile(userId))  
            }  
            // По умолчанию \- на главную  
            else \-\> listOf(AppRoute.Home)  
        }  
    }  
}

### **5.3. Интеграция с Платформами**

Android:  
Перехват Intent в MainActivity.

Kotlin

override fun onCreate(savedInstanceState: Bundle?) {  
    super.onCreate(savedInstanceState)  
    val startStack \= intent?.data?.toString()?.let { DeepLinkHandler.parse(it) }   
                    ?: listOf(AppRoute.Home)  
      
    setContent {  
        val backStack \= rememberNavBackStack(startStack)  
        //... NavDisplay  
    }  
}

Web (Kotlin/Wasm & JS):  
Для Web-приложений Nav3 позволяет интегрироваться с историей браузера. Используйте bindToBrowserNavigation (если доступно в версии Nav3) или ручную подписку на события window.onpopstate. Nav3 позволяет транслировать маршруты в URL браузера для поддержки кнопок "Вперед/Назад" в браузере.4  
iOS:  
В iOS необходимо передать URL из AppDelegate или SceneDelegate в Compose через ComposeUIViewController.

Swift

func scene(\_ scene: UIScene, openURLContexts URLContexts: Set\<UIOpenURLContext\>) {  
    guard let url \= URLContexts.first?.url else { return }  
    // Передача URL в Kotlin код через мост  
    AppKt.handleDeepLink(url: url.absoluteString)  
}

## ---

**6\. Адаптивные Интерфейсы и Поддержка Многооконности**

Самое мощное преимущество Nav3 перед Nav2 — это нативная поддержка адаптивных интерфейсов. NavDisplay может отображать несколько записей из стека одновременно, используя SceneStrategy.1

### **6.1. Реализация List-Detail Паттерна**

В Nav2 для реализации List-Detail (когда список и детали видны одновременно на планшете) приходилось использовать SlidingPaneLayout или сложные вложенные графы. В Nav3 это решается на уровне стратегии рендеринга.

Вы можете определить логику: "Если экран широкий, показать два последних элемента стека. Если узкий — только последний".

Kotlin

val adaptiveStrategy \= object : SceneStrategy {  
    override fun display(  
        keys: List\<NavKey\>,  
        entryProvider: EntryProvider  
    ): List\<Scene\> {  
        val lastKey \= keys.lastOrNull()?: return emptyList()  
          
        return if (isWideScreen && keys.size \> 1) {  
            // Берем два последних ключа  
            val listKey \= keys\[keys.size \- 2\]  
            val detailKey \= lastKey  
              
            // Возвращаем две сцены, которые NavDisplay отрисует рядом (через Row или Custom Layout)  
            listOf(  
                Scene(key \= listKey, content \= entryProvider.getEntry(listKey)),  
                Scene(key \= detailKey, content \= entryProvider.getEntry(detailKey))  
            )  
        } else {  
            // Стандартное поведение  
            listOf(Scene(key \= lastKey, content \= entryProvider.getEntry(lastKey)))  
        }  
    }  
}

Библиотека adaptive-navigation3 предоставляет готовые реализации таких стратегий, включая поддержку Material 3 Adaptive Layouts.15

## ---

**7\. Внедрение Зависимостей (DI) и Интеграция с Koin**

В современных KMP проектах стандарт де\-факто для DI — это **Koin**. Nav3 упрощает передачу параметров во ViewModels, так как параметры теперь являются частью типизированного ключа.

### **7.1. Отказ от SavedStateHandle для аргументов**

В Nav2 аргументы приходилось доставать из SavedStateHandle внутри ViewModel, что делало ViewModel зависимой от Android-фреймворка и усложняло тестирование. В Nav3 аргументы передаются через конструктор ViewModel или фабрику, используя данные из NavKey.21

### **7.2. Пример с Koin**

Использование функции koinViewModel с блоком parametersOf позволяет элегантно передать данные.

Kotlin

entry\<AppRoute.ProductDetails\> { key \-\>  
    // key.id \- это String, часть data class  
    val viewModel \= koinViewModel\<ProductViewModel\> { parametersOf(key.id) }  
      
    ProductScreen(  
        state \= viewModel.state.collectAsState(),  
        onBack \= { backStack.removeLast() }  
    )  
}

// Модуль Koin  
val appModule \= module {  
    viewModel { (productId: String) \-\> ProductViewModel(productId, get()) }  
}

Такой подход делает ViewModel "чистой" и полностью независимой от библиотеки навигации.22

## ---

**8\. Тестирование и Отладка**

Переход на управление состоянием через List\<NavKey\> открывает возможности для чистого Unit-тестирования навигации.

### **8.1. Unit-тесты логики навигации**

Вам больше не нужны UI-тесты или TestNavHostController для проверки переходов.

Kotlin

@Test  
fun \`when navigating to profile, stack should contain home and profile\`() {  
    val viewModel \= RootNavigationViewModel()  
    // Начальное состояние  
    assertEquals(listOf(AppRoute.Home), viewModel.backStack)

    viewModel.navigateTo(AppRoute.Profile("user1"))

    assertEquals(  
        listOf(AppRoute.Home, AppRoute.Profile("user1")),   
        viewModel.backStack  
    )  
}

### **8.2. Инструментальные тесты**

Для UI-тестов в Nav3 можно подменять EntryProvider. Вы можете передать в NavDisplay фейковый провайдер, который рендерит заглушки (Text placeholders) вместо реальных сложных экранов, проверяя только факт переключения экранов.

## ---

**9\. Заключение**

Переход на **Navigation Multiplatform 3** представляет собой стратегическую инвестицию в архитектуру приложения. Несмотря на то, что библиотека находится в стадии Alpha (в экосистеме KMP), её преимущества перевешивают риски для новых проектов.

**Ключевые выводы:**

1. **Типобезопасность:** Ошибки навигации становятся ошибками компиляции, а не времени выполнения.  
2. **Прозрачность:** Стек навигации — это просто список, который вы контролируете. Это упрощает отладку и логгирование.  
3. **Адаптивность:** Nav3 — единственное на текущий момент нативное решение в экосистеме Compose, которое позволяет элегантно решать задачи адаптивного UI без "костылей".  
4. **Унификация KMP:** Единый код навигации для Android, iOS, Web и Desktop значительно снижает стоимость разработки и поддержки.

Рекомендация по миграции:  
Для существующих крупных проектов рекомендуется поэтапный подход: сначала мигрировать на Type Safe Routes (доступно в Nav 2.8+), а затем, по мере стабилизации Nav3, заменять NavHost на NavDisplay. Для новых проектов Nav3 рекомендуется к использованию с первого дня, при условии тщательного управления версиями зависимостей.

#### **Works cited**

1. Modern Android Navigation Made Simple with Navigation 3 | by Mahesa Iqbal Ridwansyah | Medium | CodeElevation, accessed December 14, 2025, [https://medium.com/codeelevation/modern-android-navigation-made-simple-with-navigation-3-10fa375b6571](https://medium.com/codeelevation/modern-android-navigation-made-simple-with-navigation-3-10fa375b6571)  
2. Announcing Jetpack Navigation 3 \- Android Developers Blog, accessed December 14, 2025, [https://android-developers.googleblog.com/2025/05/announcing-jetpack-navigation-3-for-compose.html](https://android-developers.googleblog.com/2025/05/announcing-jetpack-navigation-3-for-compose.html)  
3. Type safety in Kotlin DSL and Navigation Compose | App architecture \- Android Developers, accessed December 14, 2025, [https://developer.android.com/guide/navigation/design/type-safety](https://developer.android.com/guide/navigation/design/type-safety)  
4. Navigation and routing | Kotlin Multiplatform Documentation, accessed December 14, 2025, [https://kotlinlang.org/docs/multiplatform/compose-navigation-routing.html](https://kotlinlang.org/docs/multiplatform/compose-navigation-routing.html)  
5. Jetpack Navigation 3 is stable \- Android Developers Blog, accessed December 14, 2025, [https://android-developers.googleblog.com/2025/11/jetpack-navigation-3-is-stable.html](https://android-developers.googleblog.com/2025/11/jetpack-navigation-3-is-stable.html)  
6. Migrating from Navigation Compose (v2) to Navigation 3 in Jetpack Compose | by Varun Chandran | Dec, 2025 | Medium, accessed December 14, 2025, [https://medium.com/@varunchandran333/migrating-from-navigation-compose-v2-to-navigation-3-in-jetpack-compose-5a275a98d1f7](https://medium.com/@varunchandran333/migrating-from-navigation-compose-v2-to-navigation-3-in-jetpack-compose-5a275a98d1f7)  
7. What's new in Compose Multiplatform 1.9.3 \- Kotlin, accessed December 14, 2025, [https://kotlinlang.org/docs/multiplatform/whats-new-compose-190.html](https://kotlinlang.org/docs/multiplatform/whats-new-compose-190.html)  
8. Using Navigation 3 with Compose Multiplatform \- John O'Reilly, accessed December 14, 2025, [https://johnoreilly.dev/posts/navigation3-cmp/](https://johnoreilly.dev/posts/navigation3-cmp/)  
9. Migrate from Navigation 2 to Navigation 3 | App architecture \- Android Developers, accessed December 14, 2025, [https://developer.android.com/guide/navigation/navigation-3/migration-guide](https://developer.android.com/guide/navigation/navigation-3/migration-guide)  
10. Mastering Jetpack Compose Navigation 3: A Step-by-Step Guide | by Jay Patel | Medium, accessed December 14, 2025, [https://impateljay.medium.com/mastering-jetpack-compose-navigation-3-a-step-by-step-guide-93ef33dad19b](https://impateljay.medium.com/mastering-jetpack-compose-navigation-3-a-step-by-step-guide-93ef33dad19b)  
11. Android Compose Navigation 3 — Complete Overview | by Ahmed Ally \- Medium, accessed December 14, 2025, [https://medium.com/@ahmed.ally2/android-compose-navigation-3-complete-overview-c33228f1bbad](https://medium.com/@ahmed.ally2/android-compose-navigation-3-complete-overview-c33228f1bbad)  
12. Mastering Compose Navigation 3: A Deep Dive into Navigation 3 (Part 1: The Basics) | by Sunil Kumar | Medium, accessed December 14, 2025, [https://medium.com/@sunildhiman90/mastering-compose-navigation-3-a-deep-dive-into-navigation-3-part-1-the-basics-e1f45668d638](https://medium.com/@sunildhiman90/mastering-compose-navigation-3-a-deep-dive-into-navigation-3-part-1-the-basics-e1f45668d638)  
13. Modularize navigation code | App architecture \- Android Developers, accessed December 14, 2025, [https://developer.android.com/guide/navigation/navigation-3/modularize](https://developer.android.com/guide/navigation/navigation-3/modularize)  
14. navigation3 | Jetpack \- Android Developers, accessed December 14, 2025, [https://developer.android.com/jetpack/androidx/releases/navigation3](https://developer.android.com/jetpack/androidx/releases/navigation3)  
15. What's new in Compose Multiplatform 1.10.0-rc02 \- Kotlin, accessed December 14, 2025, [https://kotlinlang.org/docs/multiplatform/whats-new-compose-110.html](https://kotlinlang.org/docs/multiplatform/whats-new-compose-110.html)  
16. Navigation 3 — The new navigation system for Jetpack Compose | by Nicos Nicolaou, accessed December 14, 2025, [https://medium.com/@nicosnicolaou/navigation-3-the-new-navigation-system-for-jetpack-compose-6dd26313aed6](https://medium.com/@nicosnicolaou/navigation-3-the-new-navigation-system-for-jetpack-compose-6dd26313aed6)  
17. Save and manage navigation state | App architecture \- Android Developers, accessed December 14, 2025, [https://developer.android.com/guide/navigation/navigation-3/save-state](https://developer.android.com/guide/navigation/navigation-3/save-state)  
18. Deep Link Handling in Jetpack Navigation 3 | by Kadirtas | Dec, 2025 | Medium, accessed December 14, 2025, [https://medium.com/@kadirtas02/deep-link-handling-in-jetpack-navigation-3-e1c6383d2dd4](https://medium.com/@kadirtas02/deep-link-handling-in-jetpack-navigation-3-e1c6383d2dd4)  
19. deeplink-guide.md \- android/nav3-recipes \- GitHub, accessed December 14, 2025, [https://github.com/android/nav3-recipes/blob/main/docs/deeplink-guide.md](https://github.com/android/nav3-recipes/blob/main/docs/deeplink-guide.md)  
20. How to Deep Link Into iOS & Android With Compose Multiplatform \- YouTube, accessed December 14, 2025, [https://www.youtube.com/watch?v=9XMN2neHyOw](https://www.youtube.com/watch?v=9XMN2neHyOw)  
21. How to pass arguments with Navigation3 using SavedStateHandle? \- Stack Overflow, accessed December 14, 2025, [https://stackoverflow.com/questions/79763944/how-to-pass-arguments-with-navigation3-using-savedstatehandle](https://stackoverflow.com/questions/79763944/how-to-pass-arguments-with-navigation3-using-savedstatehandle)  
22. App architecture | Android Developers, accessed December 14, 2025, [https://developer.android.com/guide/navigation/navigation-3/recipes/passingarguments](https://developer.android.com/guide/navigation/navigation-3/recipes/passingarguments)