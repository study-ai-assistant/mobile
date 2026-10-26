# Сборка и запуск

Инструкция для Windows, macOS и Linux. Основа — пустой Compose-проект из PR #11.
Экран с запросами к серверу добавлен в PR #13 (задача #6).

## Инструменты и версии

- Git и Android Studio с поддержкой Android Gradle Plugin 9.4.1.
- В SDK Manager установить Android SDK Platform 37 и Android SDK Platform-Tools.
  Для эмулятора также установить Android Emulator и системный образ для своей
  архитектуры. Для проверки разрешения локальной сети использовать API 37.
- Gradle 9.6.0 запускается через wrapper проекта; отдельно Gradle не устанавливать.
- Для Gradle daemon проект запрашивает JDK 25 в
  `gradle/gradle-daemon-jvm.properties`. При необходимости Gradle загружает его
  автоматически; первая синхронизация требует доступа к сети. Не путать с JDK 21
  для backend. Если загрузка JDK не удалась, установить JDK 25 и проверить настройки
  Gradle JDK в Android Studio и диагностику Sync.
- Приложение поддерживает Android 8.0/API 26 и выше; compileSdk/targetSdk — 37.

Версии зависимостей зафиксированы в `gradle/libs.versions.toml`, версия Gradle —
в `gradle/wrapper/gradle-wrapper.properties`.

## Установка и открытие проекта

1. Установить Git и Android Studio с официальных сайтов
   https://git-scm.com/downloads и https://developer.android.com/studio.
   На Windows использовать установщик, на macOS — пакет для архитектуры Mac,
   на Linux — пакет и системные зависимости из инструкции Android Studio.
2. Завершить Setup Wizard Android Studio и проверить компоненты SDK Manager,
   перечисленные выше. Системный образ эмулятора устанавливается отдельно.
3. В PowerShell (Windows) или Terminal (macOS/Linux) выполнить:

   ```text
   git clone https://github.com/study-ai-assistant/mobile.git
   cd mobile
   ```

   Если репозиторий уже клонирован, использовать существующую папку. Перед
   переключением веток проверить `git status` и сохранить свою работу коммитом.
4. В Android Studio выбрать File → Open и папку `mobile`, дождаться Gradle Sync.
5. Если SDK не найден, указать установленный SDK в настройках проекта. Android
   Studio сохраняет путь в локальном `local.properties`; этот файл не коммитить.
   Типичные пути SDK: Windows — `%LOCALAPPDATA%\Android\Sdk`, macOS —
   `~/Library/Android/sdk`, Linux — `~/Android/Sdk`. Фактический путь виден в SDK Manager.

## Запуск на эмуляторе или телефоне

1. В Device Manager создать виртуальное устройство и скачать подходящий образ.
   Для аппаратного ускорения следовать диагностике Emulator: на Windows проверить
   виртуализацию и гипервизор, на Linux — доступ к KVM, на Apple Silicon — ARM64-образ.
2. Выбрать устройство и конфигурацию `app`, нажать Run.
3. Для чистого скелета #11 ожидается `Hello Android!`. После добавления экрана #6
   ожидается поле текста и кнопки отправки/проверки сервера — это другая версия приложения.
4. Для физического телефона включить режим разработчика и USB debugging,
   подключить USB и подтвердить доверие компьютеру на телефоне. Выбрать телефон
   в списке устройств Android Studio. При отсутствии устройства проверить USB-кабель
   и драйвер производителя на Windows либо правила доступа USB на Linux.

## Проверки и сборка APK

Выполнять из корня `mobile`.

Windows / PowerShell:

```powershell
.\gradlew.bat assembleDebug testDebugUnitTest lintDebug
```

macOS / Linux:

```bash
./gradlew assembleDebug testDebugUnitTest lintDebug
```

APK: `app/build/outputs/apk/debug/app-debug.apk`.
Отчёты: `app/build/reports/tests/testDebugUnitTest/` и
`app/build/reports/lint-results-debug.html`.
Инструментальные тесты на подключённом устройстве запускаются отдельно задачей
`connectedDebugAndroidTest`; обычный `testDebugUnitTest` не проверяет интерфейс на устройстве.

## Backend для проверки связи

Для пустого скелета сервер не требуется. Для экрана #6 клонировать backend рядом
с `mobile`, чтобы папки имели общего родителя. Если backend уже есть, использовать
его; перед обновлением проверить ветку и `git status`, не перезаписывать локальный `.env`.

В новом checkout backend, пока реализация находится в feature-ветке:

```text
git clone --branch feature/server-base https://github.com/study-ai-assistant/backend.git
cd backend
```

Установить JDK 21 и проверить, что `java -version` и Maven wrapper используют его.
Сверяться с актуальным README backend при изменении веток и требований.
Создать `.env` из `.env.example` только если собственного `.env` ещё нет.

Windows / PowerShell:

```powershell
if (-not (Test-Path .env)) { Copy-Item .env.example .env }
.\mvnw.cmd spring-boot:run
```

macOS / Linux:

```bash
test -f .env || cp .env.example .env
sh ./mvnw spring-boot:run
```

Оставить сервер работающим. На компьютере открыть `http://localhost:8080/health`:
ожидается HTTP 200 и `{"status":"ok"}`. Успешный health не подтверждает работу LLM.
Для генерации настроить провайдера и ключ по README backend. Ключ хранится только
на сервере в локальном `.env`; не добавлять его в Android-приложение, Git или сообщения.

## Адрес сервера в экране #6

| Устройство | Адрес |
|---|---|
| Android Emulator | `http://10.0.2.2:8080` |
| Телефон в той же Wi-Fi сети | `http://<локальный-IP-компьютера>:8080` |

В реализации #6 адрес находится в `api/ServerConfig.kt`. В пустом скелете #11
этого файла ещё нет. `localhost` на устройстве указывает на само устройство.
Для телефона сервер должен быть доступен через локальный интерфейс компьютера;
если соединение блокирует брандмауэр, разрешить нужному процессу/порту доступ
в доверенной частной сети, не отключая брандмауэр целиком.

Экран #6 использует INTERNET и разрешает HTTP только в debug-сборке.
На Android 17/API 37 при targetSdk 37 также нужен ACCESS_LOCAL_NETWORK:
при первом запросе разрешить доступ к устройствам поблизости, затем повторить проверку.
При отказе разрешение можно изменить в настройках приложения.

Текстовый контракт — `POST /api/v1/text/process`, JSON с полями `text` и `mode`.
Файловый `/api/v1/materials/process` не является заменой текстового маршрута.
Готовность обработки проверять по версии запущенного backend и его OpenAPI.

## Частые проблемы

| Симптом | Проверка |
|---|---|
| SDK location not found | Указать SDK в Android Studio; проверить Platform 37 и local.properties |
| Ошибка скачивания Gradle/JDK/зависимостей | Проверить доступ к адресу из ошибки и настройки прокси; повторить Sync после восстановления доступа |
| Эмулятор не запускается | Проверить аппаратную виртуализацию, совместимость образа и диагностику Emulator |
| На компьютере health работает, в приложении таймаут | Проверить адрес 10.0.2.2 для эмулятора и разрешение локальной сети на API 37 |
| Backend использует неверный JDK | Проверить java -version и вывод mvnw.cmd -version / sh ./mvnw -version; backend требует JDK 21 |
| ./gradlew: Permission denied | В Linux/macOS проверить право выполнения: chmod +x gradlew; в Git оно должно быть сохранено как 100755 |
| Отправка текста возвращает 404 | Проверить версию backend и наличие текстового маршрута; health сам по себе недостаточен |
| Отправка текста возвращает 502 | Проверить конфигурацию LLM и доступ к провайдеру на сервере, не публикуя ключи |

## Подтверждение задачи #4

Каждый разработчик mobile отдельно подтверждает запуск у себя. Инструкция
не является подтверждением тестирования на всех трёх ОС. Для закрытия #4 также
нужна опубликованная страница wiki «Сборка и запуск»; добавление этого файла
через PR не обновляет wiki автоматически.
