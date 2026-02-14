# **Глобальный технический отчет: Стратегия модернизации и архитектурная эволюция экосистемы Kotlin Multiplatform (2025-2026)**

## **Аннотация**

Настоящий отчет представляет собой всестороннее исследование и техническое руководство, разработанное для архитекторов программного обеспечения, технических директоров и ведущих мобильных разработчиков. Документ посвящен критическому анализу и методологии обновления технологического стека Kotlin Multiplatform (KMP) до состояния State-of-the-Art по состоянию на декабрь 2025 года. В центре внимания находится миграция на **Kotlin 2.2.21**, интеграция **Compose Multiplatform 1.9.3** и внедрение типобезопасной навигации посредством **Navigation Multiplatform 2.9.1**.

Текущий цикл обновлений экосистемы JetBrains характеризуется не просто инкрементальными улучшениями, а фундаментальным сдвигом парадигмы разработки. Завершение перехода на компилятор K2, внедрение прямой трансляции в Swift (Swift Export), отказ от легаси-механизмов bitcode на iOS и полная переработка системы навигации требуют от команд разработки глубокого переосмысления архитектуры приложений. В отчете детально рассматриваются причинно-следственные связи технических решений, анализ рисков несовместимости ABI, стратегии управления транзитивными зависимостями через Version Catalogs и методы решения специфических платформенных проблем, возникающих при миграции.

## ---

**Глава 1\. Эволюция ядра: Kotlin 2.2.21 и смена парадигмы компиляции**

Фундаментом любой миграции в экосистеме KMP является обновление языка и компилятора. Версия Kotlin 2.2.21 — это не просто очередной патч, а стабилизационный релиз, закрепляющий тектонические сдвиги, начатые в версии 2.0.0. Понимание этих сдвигов критически важно для предотвращения регрессий в производительности и стабильности сборки.

### **1.1. Архитектура K2: От стабилизации к безальтернативности**

С выходом линейки 2.2.x, использование фронтенда компилятора K2 переходит из разряда "рекомендуемых опций" в статус безальтернативного стандарта индустрии. Если в версиях 1.9.x и ранних 2.0.x допускалось использование старого фронтенда для совместимости с некоторыми плагинами, то текущая экосистема Compose Multiplatform 1.9.3 оптимизирована и валидирована исключительно под K2.1

#### **Техническая сущность перехода на K2**

Суть изменения заключается в унификации структур данных, используемых IDE и компилятором. В архитектуре K1 IDE использовала свой собственный анализатор кода (IntelliJ IDEA frontend), а компилятор — свой. Это приводило к печально известным ситуациям, когда IDE подсвечивала код красным (ошибка), но компиляция проходила успешно, и наоборот. В сложных KMP-проектах с многоуровневой иерархией sourceSets и механизмом expect/actual, расхождения в анализе могли достигать критических масштабов, затрудняя рефакторинг.

K2 устраняет этот дуализм, вводя единое промежуточное представление (Frontend IR). Для команд разработки это означает:

1. **Повышение производительности компиляции:** Прирост скорости до 2-х раз на этапе анализа кода, что особенно заметно в крупных мультимодульных проектах.  
2. **Строгость типов:** K2 более педантичен в вопросах вывода типов (type inference) и Smart Casts. Код, который "случайно" работал в 1.9.x благодаря багам в выводе типов, в 2.2.21 может перестать компилироваться. Это следует рассматривать не как регрессию, а как устранение технического долга.  
3. **Совместимость плагинов:** Все плагины компилятора (KSP, Serialization, Compose Compiler, PowerAssert) должны быть пересобраны с учетом API K2. Использование устаревших версий плагинов в Kotlin 2.2.21 приведет к фатальным ошибкам сборки, так как бинарный интерфейс компилятора изменился.

### **1.2. Управление версиями языка: Политика устаревания**

Kotlin 2.2.21 вводит жесткие ограничения на использование устаревших версий языка (language version) и API. Это изменение классифицируется как *source-incompatible* (несовместимое на уровне исходного кода) для проектов, использующих легаси-конфигурации.2

Анализ документации показывает следующую динамику:

* **Отказ от 1.6 и 1.7:** Компилятор версии 2.2.x больше не поддерживает флаги \-language-version 1.6 и \-language-version 1.7. Попытка компиляции модуля с такими настройками приведет к ошибке сборки. Это требует от разработчиков аудита всех модулей проекта, особенно тех, которые редко обновляются (utility-библиотеки, core-модули), и явного повышения версии языка до 1.8 или выше (рекомендуется 2.0+).  
* **Цикл депрекации:** В версии 2.1.0 это вызывало лишь предупреждение, но в 2.2.0 политика ужесточилась до ошибки.2 Это важный сигнал для команд, практикующих "ленивую" миграцию: игнорирование предупреждений компилятора в KMP становится блокирующим фактором гораздо быстрее, чем в классической Android-разработке.

### **1.3. Интероперабельность с Apple: Swift Export и Bitcode**

Наиболее значимые изменения в Kotlin 2.2.21 касаются взаимодействия с экосистемой Apple (iOS/macOS). Здесь наблюдаются два встречных процесса: внедрение новых возможностей (Swift Export) и удаление устаревших (Bitcode).

#### **Swift Export: Новая эра интероперабельности**

Традиционно взаимодействие Kotlin/Native с iOS строилось через генерацию Objective-C заголовков. Swift-код взаимодействовал с Kotlin через Objective-C прослойку. Это накладывало ряд ограничений: отсутствие поддержки современных фич Swift (structs, enums с данными, actors), потеря null-safety нюансов и сложности с generics.

Kotlin 2.2.20 внедряет экспериментальную, но включенную по умолчанию поддержку **Swift Export**.3 Этот механизм генерирует непосредственно Swift-интерфейсы, минуя "узкое горлышко" Objective-C.

**Преимущества для архитектуры:**

* **Прямая поддержка Suspend Functions:** Теперь suspend функции Kotlin транслируются в нативные async/await конструкции Swift более чисто, без необходимости использования вспомогательных оберток (wrappers) или библиотек типа KMP-NativeCoroutines в простых сценариях.  
* **Имена аргументов:** Поддержка именованных аргументов делает вызов Kotlin-функций из Swift идиоматичным.  
* **Пространства имен:** Лучшая изоляция модулей и предотвращение конфликтов имен.

Для активации и тонкой настройки Swift Export может потребоваться модификация build.gradle.kts для передачи специфических флагов компилятору, хотя базовый функционал доступен "из коробки" в 2.2.21.5

#### **Смерть Bitcode**

Вторым критическим изменением является окончательное удаление поддержки встраивания биткода (embedBitcode). Apple официально отказалась от Bitcode начиная с Xcode 14, и Kotlin последовал этому примеру.

В версиях 2.0.x использование DSL embedBitcode вызывало предупреждение. В Kotlin 2.2.0+ (включая 2.2.21) наличие этого вызова в скриптах сборки приведет к ошибке.6

**План действий по миграции:**

1. Провести глобальный поиск по проекту на наличие вызова embedBitcode.  
2. Удалить данные блоки из конфигурации framework (обычно внутри таргетов iosArm64, iosX64).  
3. Если проект использует старые плагины (например, сторонние SDK), которые неявно зависят от биткода, их необходимо обновить или заменить.

## ---

**Глава 2\. Compose Multiplatform 1.9.3: Зрелость UI фреймворка**

Версия 1.9.3 (с рантаймом 1.9.4) знаменует собой переход Compose Multiplatform в стадию промышленной зрелости. Основной фокус релиза сделан на гибкости управления зависимостями и расширении возможностей веб\-таргета.

### **2.1. Стратегическое развязывание версий (Decoupling)**

Исторически одной из главных проблем обновления KMP-проектов была жесткая связка между версией плагина Compose Multiplatform и версией библиотеки Material3. Если Google выпускал новую версию Material3 с критическими исправлениями или новыми компонентами (например, Expressive Design), разработчики KMP были вынуждены ждать обновления всего плагина JetBrains Compose.

В версии 1.9.3 эта проблема решена архитектурно.7 Теперь версии плагина Gradle и библиотеки Material3 независимы.

Архитектурное преимущество:  
Это позволяет командам использовать стабильный инструментарий сборки (плагин 1.9.3), но экспериментировать с новейшими UI-компонентами. Например, для доступа к новым компонентам Material3 1.9.0 (которые соответствуют Jetpack Compose 1.9.x), можно явно указать зависимость, переопределив дефолтную версию 1.4.0:

Kotlin

// build.gradle.kts (commonMain)  
dependencies {  
    // Явное повышение версии Material3 для доступа к новым API  
    implementation("org.jetbrains.compose.material3:material3:1.9.0-alpha04")  
}

Это изменение требует внимательного управления версиями в libs.versions.toml, чтобы избежать конфликтов транзитивных зависимостей.

### **2.2. Новая модель плагина компилятора Compose**

С переходом на Kotlin 2.0+ изменился способ интеграции компилятора Compose. Ранее плагин org.jetbrains.compose неявно конфигурировал компилятор. Теперь, в соответствии с архитектурой K2, ответственность за трансформацию @Composable функций возложена на отдельный плагин компилятора, который поставляется непосредственно командой Kotlin.8

Обязательная конфигурация:  
Для проектов на Kotlin 2.2.21 применение плагина org.jetbrains.kotlin.plugin.compose является обязательным. Отсутствие этого плагина приведет к ошибкам компиляции, указывающим на то, что функции не являются @Composable или плагин не найден.  
Важный нюанс: версия этого плагина должна **строго совпадать** с версией Kotlin (2.2.21), в то время как версия библиотеки UI (org.jetbrains.compose) остается 1.9.3.

**Таблица совместимости версий:**

| Компонент | Версия в проекте | Примечание |
| :---- | :---- | :---- |
| **Kotlin** | **2.2.21** | Базовая технология |
| **Compose Compiler Plugin** | **2.2.21** | Строго соответствует версии Kotlin |
| **Compose Multiplatform Plugin** | **1.9.3** | Управляет зависимостями UI (Runtime, Foundation, UI) |
| **Compose Runtime** | **1.9.4** | Подтягивается плагином 1.9.3 транзитивно |
| **Material 3** | **1.4.0+** | Может быть переопределена вручную |

### **2.3. Прорыв в Web: Гибридный рендеринг и HTML Interop**

Compose for Web (Wasm и JS) долгое время страдал от проблемы изоляции: весь UI рисовался на HTML Canvas, что делало невозможным использование нативных возможностей браузера (например, автозаполнение полей ввода, выделение текста на уровне ОС, интеграция iframe).

Версия 1.9.3 представляет новый API для встраивания HTML-контента.7 Это позволяет создавать "окна" в Canvas-рендеринге, где отображаются реальные DOM-элементы.

**Сценарии использования:**

1. **Карты и Видео:** Встраивание Google Maps, YouTube плееров или других тяжелых Web-компонентов, которые невозможно переписать на Canvas.  
2. **SEO-критичный контент:** Текстовые блоки, которые должны индексироваться поисковиками.  
3. **Нативные формы:** Использование \<input\> элементов для корректной работы менеджеров паролей и автозаполнения браузера.

Пример интеграции (концептуальный):

Kotlin

// commonMain (js/wasm source set)  
AndroidView(factory \= { context \-\>  
    // На Android это View  
    MapView(context)  
}, update \= { view \-\>  
    // Обновление  
})

// На Web это трансформируется в новый HTML Interop API  
WebElementView(  
    elementBuilder \= { document.createElement("div") }  
)

*(Примечание: API может отличаться в деталях, но принцип гибридизации является ключевым нововведением).*

## ---

**Глава 3\. Navigation Multiplatform 2.9.1: Революция типобезопасности**

Навигация традиционно была одним из самых сложных аспектов KMP. До недавнего времени сообщество полагалось на сторонние решения (Decompose, Voyager). Выход официального порта Jetpack Navigation 2.9.1 меняет ландшафт, предлагая стандартизированное решение от Google и JetBrains.

### **3.1. Отказ от строковых маршрутов (String Routes)**

Главным нововведением версии 2.9.1 (следующим за Jetpack Navigation 2.8.0) является поддержка **Type-Safe Navigation**.11 Ранее навигация строилась на строках ("profile/{userId}"), что приводило к runtime-ошибкам при опечатках или неверной передаче аргументов.

Новая система базируется на **Kotlin Serialization**. Маршруты описываются как сериализуемые объекты или дата-классы.

**Механизм работы:**

1. Разработчик помечает класс аннотацией @Serializable.  
2. Плагин Serialization генерирует код для сохранения и восстановления состояния этого объекта.  
3. Библиотека Navigation использует этот механизм для преобразования объекта в URL-подобную строку (для поддержки Deep Links) и обратно, но этот процесс скрыт от разработчика.

**Пример архитектурного сдвига:**

*Legacy (String-based):*

Kotlin

navController.navigate("profile/123")  
// В получателе:  
val id \= backStackEntry.arguments?.getString("userId") // Риск NullPointerException или опечатки в ключе

*Modern (Type-Safe):*

Kotlin

@Serializable  
data class ProfileRoute(val userId: Int)

navController.navigate(ProfileRoute(123))  
// В получателе:  
val route \= backStackEntry.toRoute\<ProfileRoute\>() // Полная типобезопасность

### **3.2. Настройка инфраструктуры навигации**

Для работы Type-Safe Navigation необходимо корректно настроить зависимости, так как они теперь включают библиотеку сериализации.

В файле libs.versions.toml:

Ini, TOML

\[versions\]  
navigation \= "2.9.1"  
serialization \= "1.7.3" \# Совместимая с Kotlin 2.2.21

\[libraries\]  
androidx-navigation-compose \= { module \= "org.jetbrains.androidx.navigation:navigation-compose", version.ref \= "navigation" }  
kotlinx-serialization-json \= { module \= "org.jetbrains.kotlinx:kotlinx-serialization-json", version.ref \= "serialization" }

\[plugins\]  
kotlin-serialization \= { id \= "org.jetbrains.kotlin.plugin.serialization", version.ref \= "kotlin" }

Обратите внимание, что плагин сериализации (kotlin-serialization) должен иметь версию, совпадающую с версией Kotlin (2.2.21). Рассинхронизация версий здесь — одна из самых частых причин сбоя сборки.13

### **3.3. Взгляд в будущее: Navigation 3**

Хотя данный отчет рекомендует обновление до стабильной версии 2.9.1, архитекторам следует учитывать существование **Navigation 3** (на данный момент в Alpha/Beta для KMP).14

Navigation 3 предлагает еще более радикальный подход, отвязывая навигацию от ViewModel и жесткой привязки к графу экранов в том виде, в каком мы к нему привыкли. Однако, для production-систем в 2025 году версия 2.9.1 остается "золотым стандартом" стабильности, обеспечивая баланс между современными API и надежностью. Миграция на 2.9.1 сейчас значительно упростит переход на Navigation 3 в будущем, так как обе системы разделяют философию типобезопасности.

## ---

**Глава 4\. Стратегия управления зависимостями и сборкой**

Успех миграции на новые версии KMP на 50% зависит от качества конфигурации Gradle. Рост сложности графа зависимостей делает ручное управление версиями в build.gradle.kts крайне рискованным.

### **4.1. Философия Version Catalogs (libs.versions.toml)**

Использование libs.versions.toml теперь является стандартом де\-факто для KMP проектов. Это позволяет централизовать управление версиями и гарантировать, что все модули многомодульного проекта используют одни и те же версии библиотек, предотвращая "Dependency Hell".

Ниже приведен эталонный фрагмент каталога версий для описываемого стека (Kotlin 2.2.21 \+ Compose 1.9.3 \+ Nav 2.9.1):

Ini, TOML

\[versions\]  
\# Core  
kotlin \= "2.2.21"  
agp \= "8.7.2" \# Android Gradle Plugin. Хотя 9.0.0 поддерживается, 8.7.x стабильнее для продакшна

\# Compose  
compose-plugin \= "1.9.3"  
androidx-lifecycle \= "2.8.4" \# KMP порт Lifecycle

\# Navigation  
navigation \= "2.9.1"

\# Serialization  
serialization \= "1.7.3"

\# Networking & Async  
ktor \= "3.0.1" \# Ktor 3.x \- актуальный стандарт  
coroutines \= "1.9.0"

\[libraries\]  
\# Compose  
compose-ui \= { module \= "org.jetbrains.compose.ui:ui", version.ref \= "compose-plugin" }  
compose-material3 \= { module \= "org.jetbrains.compose.material3:material3", version.ref \= "compose-plugin" }  
\# Специальная зависимость для Previews  
compose-ui-tooling \= { module \= "org.jetbrains.compose.ui:ui-tooling", version.ref \= "compose-plugin" }  
compose-ui-tooling-preview \= { module \= "org.jetbrains.compose.ui:ui-tooling-preview", version.ref \= "compose-plugin" }

\# Navigation  
navigation-compose \= { module \= "org.jetbrains.androidx.navigation:navigation-compose", version.ref \= "navigation" }

\[plugins\]  
\# Обязательная тройка для KMP \+ Compose  
kotlinMultiplatform \= { id \= "org.jetbrains.kotlin.multiplatform", version.ref \= "kotlin" }  
jetbrainsCompose \= { id \= "org.jetbrains.compose", version.ref \= "compose-plugin" }  
composeCompiler \= { id \= "org.jetbrains.kotlin.plugin.compose", version.ref \= "kotlin" }

### **4.2. Новый плагин Android-KMP**

Следует отметить появление нового плагина com.android.kotlin.multiplatform.library. Google и JetBrains продвигают его как замену классическому com.android.library для KMP-библиотек. Он упрощает конфигурацию, убирая избыточные настройки (варианты сборки, флейворы), которые часто не имеют смысла в чисто мультиплатформенном коде. Однако, его внедрение стоит проводить с осторожностью, так как он может конфликтовать с некоторыми кастомными скриптами сборки, завязанными на старую модель вариантов Android.16

## ---

**Глава 5\. Специфические аспекты платформ и Troubleshooting**

Даже при идеальной конфигурации обновление может вскрыть платформо-специфичные проблемы. Ниже приведен анализ известных рисков и методов их устранения.

### **5.1. Web (Wasm & JS): Проблема BigInt и миграция**

В Kotlin 2.2.20+ для Kotlin/JS и Wasm изменилось представление типа Long. Теперь он компилируется в JavaScript BigInt.3

**Проблема:** Если ваш код взаимодействует с внешними JS-библиотеками, которые ожидают, что Long эмулируется как объект (старое поведение Kotlin), интеграция сломается. BigInt — это примитив современного JS, и старые библиотеки могут не уметь с ним работать.

**Решение:**

1. Обновить внешние JS-зависимости (npm packages) до версий, поддерживающих BigInt.  
2. Если это невозможно, использовать флаг компилятора (временная мера, которая может быть удалена в будущем), но лучше адаптировать код интероперабельности.

Также стоит отметить унификацию задач дистрибуции. Новая задача composeCompatibilityBrowserDistribution позволяет собирать единый артефакт, который пытается запустить Wasm версию, а в случае отсутствия поддержки в браузере (например, старые Safari) — откатывается на JS версию.7

### **5.2. iOS: Мерцание WebView и Фокус**

При интеграции нативных WKWebView в Compose-интерфейс на iOS (через UIKitView) пользователи могут сталкиваться с мерцанием экрана при вводе текста или смене фокуса.18

**Анализ:** Это связано с конфликтом систем рендеринга. Compose использует Skia (Metal), а WebView — нативный UIKit рендеринг. Управление z-index и перехватом событий ввода между этими слоями — нетривиальная задача.

Workaround:  
В версиях 1.9.x были внесены улучшения в "interop blending". Если проблема сохраняется, рекомендуется проверить конфигурацию ComposeUIViewController. В некоторых случаях помогает установка стратегии onFocusBehavior в DoNothing, чтобы предотвратить автоматические сдвиги экрана, и управлять ими вручную.

### **5.3. Отсутствующие компоненты Material3**

После обновления некоторые разработчики могут обнаружить пропажу определенных компонентов, например, специфических индикаторов прогресса или полей ввода, которые были в Beta-версиях.19

Причина: Реструктуризация пакетов в Material3 1.4.0+. Некоторые API были переименованы или перемещены в другие под-пакеты (например, androidx.compose.material3.adaptive).  
Решение: Использовать IDE (IntelliJ IDEA с K2 режимом) для поиска альтернатив. Часто компонент просто переименован (например, добавление/удаление суффикса Indication).

### **5.4. Gradle Sync Issues**

Распространенная ошибка: Conflicting warnings when using AGP 9.0.0-alpha with built-in Kotlin disabled.20  
Решение: На момент написания отчета (декабрь 2025), наиболее стабильной комбинацией является AGP 8.7.2. Использование AGP 9.0.0-alpha рекомендуется только для экспериментов. Если вы вынуждены использовать AGP 9.x, убедитесь, что версия Kotlin строго обновлена до 2.2.21, так как более старые версии Kotlin могут некорректно работать с новым API Android Gradle Plugin.

## ---

**Глава 6\. План миграции (Checklist)**

Для минимизации простоя разработки (downtime) рекомендуется следующий порядок действий:

1. **Подготовка (Audit):**  
   * Проверить проект на наличие вызовов embedBitcode и удалить их.  
   * Убедиться, что в проекте нет жестко заданных languageVersion \= "1.7" (или ниже).  
   * Создать отдельную ветку в git (например, chore/upgrade-kmp-2025).  
2. **Обновление ядра (Core Update):**  
   * Обновить libs.versions.toml: Kotlin \-\> 2.2.21.  
   * Обновить плагин composeCompiler \-\> 2.2.21 (версия Kotlin).  
   * Синхронизировать Gradle. Исправить ошибки компиляции, вызванные строгостью K2.  
3. **Обновление UI (Compose Update):**  
   * Обновить compose-plugin \-\> 1.9.3.  
   * Обновить Material3 (опционально, если нужны новые фичи).  
   * Заменить старые аннотации @Preview на унифицированную androidx.compose.ui.tooling.preview.Preview.  
4. **Обновление Навигации (Nav Update):**  
   * Добавить плагин Kotlin Serialization.  
   * Подключить Navigation 2.9.1.  
   * Постепенно переводить экраны со строковых маршрутов на Type-Safe маршруты.  
5. **Платформенная проверка:**  
   * **iOS:** Запустить на симуляторе iOS 17/18 (Xcode 16). Проверить работу Swift Export.  
   * **Web:** Собрать JS и Wasm таргеты. Проверить работу с большими числами (Long).

## **Заключение**

Обновление до стека **Kotlin 2.2.21 / Compose 1.9.3 / Navigation 2.9.1** является критическим этапом в жизненном цикле любого KMP-приложения. Это не просто "поднятие версий", а инвестиция в долгосрочную поддерживаемость проекта. Переход на K2 и Swift Export устраняет фундаментальные ограничения платформы, делая Kotlin Multiplatform по-настоящему нативным решением для всех поддерживаемых платформ. Несмотря на сложность миграции (особенно в части навигации и конфигурации сборки), полученные преимущества в виде производительности, типобезопасности и стабильности IDE полностью оправдывают затраченные усилия.

#### **Works cited**

1. Compatibility and versions | Kotlin Multiplatform Documentation, accessed December 14, 2025, [https://kotlinlang.org/docs/multiplatform/compose-compatibility-and-versioning.html](https://kotlinlang.org/docs/multiplatform/compose-compatibility-and-versioning.html)  
2. Compatibility guide for Kotlin 2.2, accessed December 14, 2025, [https://kotlinlang.org/docs/compatibility-guide-22.html](https://kotlinlang.org/docs/compatibility-guide-22.html)  
3. What's new in Kotlin 2.2.20, accessed December 14, 2025, [https://kotlinlang.org/docs/whatsnew2220.html](https://kotlinlang.org/docs/whatsnew2220.html)  
4. Kotlin 2.2.20 Released \- The JetBrains Blog, accessed December 14, 2025, [https://blog.jetbrains.com/kotlin/2025/09/kotlin-2-2-20-released/](https://blog.jetbrains.com/kotlin/2025/09/kotlin-2-2-20-released/)  
5. Kotlin 2.2.20: The Features That'll Change Your Code (KMP Swift Export, webMain, and More) | by Sunil Kumar \- Medium, accessed December 14, 2025, [https://medium.com/@sunildhiman90/kotlin-2-2-20-the-features-thatll-change-your-code-kmp-swift-export-webmain-and-more-984f05cdd872](https://medium.com/@sunildhiman90/kotlin-2-2-20-the-features-thatll-change-your-code-kmp-swift-export-webmain-and-more-984f05cdd872)  
6. Compatibility guide for Kotlin Multiplatform, accessed December 14, 2025, [https://kotlinlang.org/docs/multiplatform/multiplatform-compatibility-guide.html](https://kotlinlang.org/docs/multiplatform/multiplatform-compatibility-guide.html)  
7. What's new in Compose Multiplatform 1.9.3 \- Kotlin, accessed December 14, 2025, [https://kotlinlang.org/docs/multiplatform/whats-new-compose-190.html](https://kotlinlang.org/docs/multiplatform/whats-new-compose-190.html)  
8. Updating Compose compiler | Kotlin Multiplatform Documentation, accessed December 14, 2025, [https://kotlinlang.org/docs/multiplatform/compose-compiler.html](https://kotlinlang.org/docs/multiplatform/compose-compiler.html)  
9. Compose Compiler Gradle plugin \- Android Developers, accessed December 14, 2025, [https://developer.android.com/develop/ui/compose/compiler](https://developer.android.com/develop/ui/compose/compiler)  
10. Why am I getting 'Compose Compiler Gradle plugin is required' in Kotlin 1.9 project?, accessed December 14, 2025, [https://stackoverflow.com/questions/79043386/why-am-i-getting-compose-compiler-gradle-plugin-is-required-in-kotlin-1-9-proj](https://stackoverflow.com/questions/79043386/why-am-i-getting-compose-compiler-gradle-plugin-is-required-in-kotlin-1-9-proj)  
11. Safe Args | App architecture \- Android Developers, accessed December 14, 2025, [https://developer.android.com/guide/navigation/use-graph/safe-args](https://developer.android.com/guide/navigation/use-graph/safe-args)  
12. Type safety in Kotlin DSL and Navigation Compose | App architecture \- Android Developers, accessed December 14, 2025, [https://developer.android.com/guide/navigation/design/type-safety](https://developer.android.com/guide/navigation/design/type-safety)  
13. Navigation | Jetpack \- Android Developers, accessed December 14, 2025, [https://developer.android.com/jetpack/androidx/releases/navigation](https://developer.android.com/jetpack/androidx/releases/navigation)  
14. What's new in Compose Multiplatform 1.10.0-rc02 \- Kotlin, accessed December 14, 2025, [https://kotlinlang.org/docs/multiplatform/whats-new-compose-110.html](https://kotlinlang.org/docs/multiplatform/whats-new-compose-110.html)  
15. Production-Ready Navigation 3 in Jetpack Compose | by Stefano Natali \- ProAndroidDev, accessed December 14, 2025, [https://proandroiddev.com/production-ready-navigation-3-in-jetpack-compose-0ff709d527e4](https://proandroiddev.com/production-ready-navigation-3-in-jetpack-compose-0ff709d527e4)  
16. Set up the Android Gradle Library Plugin for KMP | Kotlin, accessed December 14, 2025, [https://developer.android.com/kotlin/multiplatform/plugin](https://developer.android.com/kotlin/multiplatform/plugin)  
17. What's new in Kotlin 2.3.0-RC3, accessed December 14, 2025, [https://kotlinlang.org/docs/whatsnew-eap.html](https://kotlinlang.org/docs/whatsnew-eap.html)  
18. WKWebView input field displays a screen flash : CMP-9238 \- YouTrack, accessed December 14, 2025, [https://youtrack.jetbrains.com/projects/CMP/issues/CMP-9238/WKWebView-input-field-displays-a-screen-flash](https://youtrack.jetbrains.com/projects/CMP/issues/CMP-9238/WKWebView-input-field-displays-a-screen-flash)  
19. Missing CircularWavyProgressIndicator after updating to Compose Multiplatform 1.9.0-beta03 \- Stack Overflow, accessed December 14, 2025, [https://stackoverflow.com/questions/79745053/missing-circularwavyprogressindicator-after-updating-to-compose-multiplatform-1](https://stackoverflow.com/questions/79745053/missing-circularwavyprogressindicator-after-updating-to-compose-multiplatform-1)  
20. Releases · JetBrains/kotlin \- GitHub, accessed December 14, 2025, [https://github.com/jetbrains/kotlin/releases](https://github.com/jetbrains/kotlin/releases)