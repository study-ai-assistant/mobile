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
