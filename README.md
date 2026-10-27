# mobile — мобильное приложение «ИИ-помощник для учёбы»

Мобильный клиент студенческого проекта «ИИ-помощник для учёбы».

Пользователь загружает учебный материал, выбирает режим обработки и получает результат от сервера, который обращается к LLM:

- краткий конспект;
- подробный конспект;
- объяснение простыми словами;
- вопросы для самопроверки;
- ключевые термины.

В этом репозитории работают подкоманды **mobile** (код приложения) и **design** (UI/UX, макеты в Figma).

## Ссылки

| Что | Где |
|---|---|
| Сервер (backend) | [study-ai-assistant/backend](https://github.com/study-ai-assistant/backend) |
| Доска задач (Project) | [Sprints](https://github.com/orgs/study-ai-assistant/projects/1) |
| Wiki mobile (архитектура, сборка, дизайн) | [Wiki](https://github.com/study-ai-assistant/mobile/wiki) |
| Wiki backend (ТЗ, правила команды, API) | [Wiki](https://github.com/study-ai-assistant/backend/wiki) |
| Контракт API | [openapi.yaml в backend](https://github.com/study-ai-assistant/backend/blob/main/docs/api/openapi.yaml) |
| Работа с Figma | [docs/FIGMA_GUIDE.md](docs/FIGMA_GUIDE.md) |
| Как участвовать | [CONTRIBUTING.md](CONTRIBUTING.md) |
| Definition of Done | [docs/DEFINITION_OF_DONE.md](docs/DEFINITION_OF_DONE.md) |

## Быстрый старт

1. Установить Git и Android Studio. В SDK Manager установить Android SDK Platform 37,
   Platform-Tools и, для эмулятора, Android Emulator с системным образом.
2. Клонировать репозиторий и открыть папку `mobile` через File → Open:

   ```text
   git clone https://github.com/study-ai-assistant/mobile.git
   cd mobile
   ```

3. Дождаться Gradle Sync. Проект использует Gradle wrapper 9.6.0 и JDK 25 для
   Gradle daemon (при необходимости загружается автоматически). Путь к Android SDK
   хранится в локальном `local.properties`, который не добавляется в Git.
4. Создать устройство в Device Manager либо подключить телефон с USB debugging.
   Выбрать конфигурацию `app` и нажать Run. Ожидаемый результат: экран
   «ИИ-помощник для учёбы» с полем текста, кнопкой «Отправить» и проверкой связи с сервером.

Проверка экрана и ограничения backend: [docs/SERVER_SCREEN.md](docs/SERVER_SCREEN.md).

Проверка из корня проекта:

```powershell
# Windows / PowerShell
.\gradlew.bat assembleDebug testDebugUnitTest lintDebug
```

```bash
# macOS / Linux
./gradlew assembleDebug testDebugUnitTest lintDebug
```

APK: `app/build/outputs/apk/debug/app-debug.apk`.

Подробная [инструкция установки, запуска и решения проблем](docs/SETUP.md)
описывает Windows/macOS/Linux, телефон, эмулятор и подключение к backend.
Страница команды: [wiki «Сборка и запуск»](https://github.com/study-ai-assistant/mobile/wiki/Сборка-и-запуск).

## Структура репозитория

```text
mobile/
├── app/
│   ├── build.gradle.kts       # Android-модуль: SDK, зависимости, типы сборки
│   └── src/
│       ├── main/             # Manifest, Kotlin/Compose-код и ресурсы
│       ├── test/             # Локальные JVM-тесты
│       └── androidTest/      # Тесты на устройстве/эмуляторе
├── gradle/
│   ├── libs.versions.toml     # Версии плагинов и библиотек
│   ├── gradle-daemon-jvm.properties # JDK для Gradle daemon
│   └── wrapper/              # Версия и загрузчик Gradle
├── .github/                  # Шаблоны задач/PR, CODEOWNERS, CI
├── docs/                     # Инструкции, правила дизайна, Definition of Done
├── build.gradle.kts          # Плагины проекта
├── settings.gradle.kts       # Репозитории зависимостей и модули
├── gradle.properties         # Общие настройки Gradle
├── gradlew / gradlew.bat      # Запуск Gradle на Unix / Windows
├── CONTRIBUTING.md
└── README.md
```

Исходный пакет: `app/src/main/java/ru/studyassistant/mobile`.
В нём `MainActivity.kt` задаёт стартовый экран, `ui/theme` — тему Compose.

## Участники

Список команды — в [wiki backend](https://github.com/study-ai-assistant/backend/wiki).
