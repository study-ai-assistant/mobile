# Выбор стека: mobile

## Решение

Нативная Android-разработка: **Kotlin + Jetpack Compose**, Android Studio.

- Минимальная версия: API 26 (Android 8.0), около 98,4% устройств
- Сборка: Gradle (Kotlin DSL)
- Пакет: `ru.studyassistant.mobile`

## Почему

- Соответствует рекомендации из предложения по стеку (репозиторий backend, docs/STACK_PROPOSAL.md)
- TODO: добавить причины, которые обсуждала подкоманда

## Рассмотренные альтернативы

- TODO: что ещё обсуждали и почему не выбрали

## Взаимодействие с backend

Приложение общается с сервером (Java 21, Spring Boot) по HTTP, формат данных JSON.
Контракт API: `docs/api/openapi.yaml` в репозитории backend.
Адрес сервера для эмулятора: `http://10.0.2.2:8080`.

## Принято

Подкоманда mobile, дата: TODO
