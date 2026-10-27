# Проверка экранов mobile#6

Первая версия переходов по wireframes: текст → выбор режима → результат.
Полное описание решений для design: [SCREENS.md](SCREENS.md).

## Запуск

1. Открыть проект в Android Studio, дождаться Gradle Sync, запустить debug-вариант.
2. Для backend использовать рабочую ветку `feature/server-base` из backend PR #16
   и JDK 21. Запуск и переменные окружения описаны в README этой ветки.
3. Адрес задан в `api/ServerConfig.kt`: `http://10.0.2.2:8080` для Android Emulator.
   Для телефона нужен IP компьютера в общей сети. HTTP разрешён только в debug.
4. На Android 17/API 37 при первом запросе разрешить доступ к устройствам поблизости,
   затем повторно нажать кнопку. Без разрешения запрос не отправляется.
5. Для настоящего конспекта backend должен иметь действующие настройки LLM.
   `GET /health` работает и без ключа; сам по себе не подтверждает работу генерации.
6. Если Windows использует системный прокси, Java по умолчанию может подключаться
   напрямую. В проверенной локальной конфигурации прямой запрос к Groq возвращал
   403, а через уже настроенный системный прокси — 200. Для запуска собранного
   backend с настройками сети Windows используйте JDK 21:

   ```powershell
   java -Djava.net.useSystemProxies=true -jar target/backend-0.0.1-SNAPSHOT.jar
   ```

   Команда выполняется из папки backend, где находится локальный `.env`.
   Не добавляйте ключ в команду, Git, скриншоты или комментарии.

## Ручная проверка

- «Проверить связь с сервером» показывает `status = ok` на вводе материала.
- Проверить отказ в разрешении API 37 и повтор после его предоставления.
- Пустой/пробельный текст и более 10 000 Unicode-символов: ошибка, перехода нет.
- Ввести учебный материал, нажать «Выбрать режим». Пять режимов видны; доступен только краткий конспект.
- «Назад» возвращает введённый текст без потери данных.
- «Создать» показывает ожидание, повторная отправка недоступна. Через 20 секунд появляется подсказка о долгом ожидании.
- Успешный ответ открывает результат; «Копировать» копирует его; «Режимы» возвращает к выбору.
- При ошибках сети/400/413/502/таймауте сохраняется материал; есть «Повторить» и «Назад».
- «Отмена» немедленно прекращает ожидание в UI. Сервер может продолжить обработку;
  поздний ответ не должен менять текущий экран или следующий результат.
- Поворот устройства сохраняет материал, экран и выполняющийся запрос без повторного POST.
- Системная кнопка «Назад» действует как описано в SCREENS.md.

## Автоматические проверки

```powershell
.\gradlew.bat testDebugUnitTest lintDebug assembleDebug
.\gradlew.bat connectedDebugAndroidTest
```

Вторая команда требует запущенного эмулятора/устройства.
`StudyApiTest` проверяет HTTP/JSON и ошибки; `StudyFlowTest` — валидацию, переходы,
явный повтор и защиту от позднего ответа после отмены. `StudyFlowUiTest` проверяет
экраны на Android с тестовыми ответами, включая копирование, ошибку и отмену.
Эти тестовые ответы не являются проверкой настоящего backend/LLM.

### Отдельная проверка настоящего backend/LLM

`LiveBackendTest` по умолчанию пропускается. Для его запуска нужны работающий
backend, настроенный локально LLM и эмулятор. Тест отправляет короткий учебный текст
провайдеру; возможен расход его квоты. Проверяются `/health`, получение конспекта
через UI, копирование результата и сохранение текста при возврате.

```powershell
.\gradlew.bat connectedDebugAndroidTest "-Pandroid.testInstrumentationRunnerArguments.class=ru.studyassistant.mobile.LiveBackendTest" "-Pandroid.testInstrumentationRunnerArguments.liveBackend=true" "-Pandroid.testInstrumentationRunnerArguments.expectLlmSuccess=true"
```

Скриншоты и JSON с результатом сохраняются в
`app/build/outputs/connected_android_test_additional_output/`.

Чтобы отдельно проверить недоступный сервер, остановите локальный backend и выполните:

```powershell
.\gradlew.bat connectedDebugAndroidTest "-Pandroid.testInstrumentationRunnerArguments.class=ru.studyassistant.mobile.LiveBackendTest#serverUnavailable" "-Pandroid.testInstrumentationRunnerArguments.liveBackend=true" "-Pandroid.testInstrumentationRunnerArguments.backendOffline=true"
```

После проверки снова запустите backend. Результат выполненной интеграционной
проверки: [INTEGRATION_CHECK.md](INTEGRATION_CHECK.md).

Задача не считается Done до review, слияния и демонстрации полного сценария
с настоящим backend/LLM. Согласование design по #5 учитывается отдельно.
