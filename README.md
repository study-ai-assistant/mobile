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

### 1. Что установить

- **Android Studio** (последняя стабильная версия) с developer.android.com/studio. Внутри уже есть Android SDK, эмулятор и свой JDK, отдельно ничего ставить не нужно.
- **Git** (в Windows удобно через Git Bash).
- Для проверки связи с сервером: **JDK 21** (Temurin) и backend, см. README в [backend](https://github.com/study-ai-assistant/backend).

### 2. Клонировать и открыть проект

```bash
git clone https://github.com/study-ai-assistant/mobile.git
cd mobile
```

В Android Studio: **File → Open**, выбрать папку `mobile`. Дождаться окончания **Gradle Sync** (первый раз долго, нужен интернет). Если синхронизация оборвалась с ошибкой сети, нажать **Try Again** или **File → Sync Project with Gradle Files**.

### 3. Запустить на эмуляторе

1. Открыть **Device Manager** и создать эмулятор (например, Medium Phone).
2. Выбрать конфигурацию **app** и эмулятор, нажать зелёную кнопку **Run**.
3. Ожидаемый результат: экран «ИИ-помощник для учёбы» с полем текста, кнопкой «Отправить» и проверкой связи с сервером.

Проверка нового экрана и ограничения текущего backend: [docs/SERVER_SCREEN.md](docs/SERVER_SCREEN.md).

Если эмулятор не стартует, проверьте, что в BIOS включена виртуализация (Intel VT-x / AMD-V).

### 4. Запустить backend (для проверки связи)

Инструкция в README backend (ветка `feature/server-base`). Кратко:

```bash
cd backend
git checkout feature/server-base
cp .env.example .env
./mvnw.cmd spring-boot:run
```

Проверка в браузере: `http://localhost:8080/health` должен вернуть `{"status":"ok"}`.

### 5. Адрес сервера из приложения

| Где запущено приложение | Адрес сервера |
|---|---|
| Android-эмулятор | `http://10.0.2.2:8080` |
| Физический телефон | `http://<IP-компьютера>:8080` (одна Wi-Fi сеть, порт открыт в брандмауэре) |

`localhost` внутри эмулятора или телефона указывает на само устройство, а не на компьютер.

Для сетевых запросов нужно разрешение `android.permission.INTERNET` в `AndroidManifest.xml`. Обычный HTTP (без шифрования) Android по умолчанию блокирует, поэтому его нужно разрешить **только в debug-конфигурации** для локальной разработки.

### 6. Частые проблемы

| Проблема | Что делать |
|---|---|
| `handshake` / не качаются зависимости | Повторить Sync, проверить VPN и антивирус |
| Эмулятор не запускается | Включить виртуализацию в BIOS |
| Приложение не видит сервер | Использовать `10.0.2.2`, проверить, что backend запущен |
| В Git попал `local.properties` | Он должен быть в `.gitignore`, убрать из индекса: `git restore --staged local.properties` |
## Быстрый старт

> **Будет заполнен после выбора стека.**

Подробная инструкция появится в wiki: [Сборка и запуск](https://github.com/study-ai-assistant/mobile/wiki/Сборка-и-запуск).

## Структура репозитория

```
mobile/
├── .github/
│   ├── ISSUE_TEMPLATE/     # шаблоны задач и ошибок
│   ├── workflows/          # CI (пока заготовка)
│   ├── CODEOWNERS
│   └── pull_request_template.md
├── docs/
│   ├── DEFINITION_OF_DONE.md
│   └── FIGMA_GUIDE.md      # правила работы с макетами
├── CONTRIBUTING.md
└── README.md
```

После выбора стека добавятся папки с исходным кодом приложения и тестами — структура будет описана здесь.

## Участники

Список команды — в [wiki backend](https://github.com/study-ai-assistant/backend/wiki).
