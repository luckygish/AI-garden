## Agriculture Importer — README

Короткое описание: бэкенд на Spring Boot, который хранит статически сгенерированные AI-планы ухода за растениями и отдает их по параметрам или по хэшу. Есть аутентификация (JWT), сущности пользователя и растений, а также API для работы с планами.

## Стек
- **Java / Spring Boot / Spring Web / Spring Security (JWT)**
- **Spring Data JPA** (PostgreSQL c JSONB полями предполагается)
- **Jackson** для работы с JSON

## Запуск
1. Настроить доступ к БД (application.properties / application.yml — не включены в этот репозиторий, добавьте свои параметры).
2. Собрать и запустить приложение как обычный Spring Boot сервис (через IDE или `mvn spring-boot:run` / `gradle bootRun`).

## Архитектура проекта
- Пакет `com.agriculture.controller` — REST-контроллеры:
  - `AuthController` — регистрация и вход
  - `PlantController` — добавление растения, получение плана ухода по конкретному растению
  - `CarePlanController` — эндпоинты для расчета/проверки/получения планов по параметрам или хэшу
- Пакет `com.agriculture.services` — бизнес-логика:
  - `UserService`, `JwtService`, `PlantService`, `CarePlanService`
- Пакет `com.agriculture.repository` — репозитории JPA:
  - `UserRepository`, `PlantRepository`, `CarePlanRepository`
- Пакет `com.agriculture.models` — JPA-сущности:
  - `User`, `Plant`, `CarePlan`
- Пакет `com.agriculture.dto` — DTO для запросов/ответов:
  - `AuthRequest`, `AuthResponse`, `AddPlantRequest`, `PlantParameters`, `FeedingScheduleResponse`, `CatalogPlant` и др.
- `com.agriculture.configs.SecurityConfig` — конфигурация безопасности и JWT-фильтр
- `com.agriculture.JsonUtils` — утилиты для JSON

## Безопасность
- Все эндпоинты под `/api/auth/**` и `/api/care-plans/**` доступны без авторизации.
- Остальные эндпоинты требуют JWT токен в заголовке `Authorization: Bearer <token>`.
- Конфигурация: `com.agriculture.configs.SecurityConfig`.

## Модель данных (кратко)
- `CarePlan`:
  - `id: UUID`
  - `inputHash: String` — уникальный хэш параметров (MD5 от нормализованной строки)
  - `inputParameters: JSONB` — сохранённый JSON параметров (опционально)
  - `aiGeneratedPlan: JSONB` — готовый JSON с планом ухода
- `Plant` хранит ссылку на `CarePlan` для быстрого доступа к плану ухода.

## Поиск планов ухода

### Текущий подход (рекомендуемый)
Поиск планов ухода осуществляется **прямо по параметрам** через PostgreSQL JSONB запросы:
```sql
SELECT * FROM care_plans WHERE 
  input_parameters->>'culture' = 'Томат' AND 
  input_parameters->>'region' = 'Московская область' AND 
  input_parameters->>'garden_type' = 'открытый грунт'
```

**Преимущества:**
- Точный поиск без зависимости от хэширования
- Простота отладки и понимания
- Нативная поддержка JSONB в PostgreSQL

### Альтернативный подход (закомментирован)
Ранее использовалась генерация MD5 хэша для поиска планов:
```
normalized = "culture:%s|region:%s|garden_type:%s".format(
  culture.toLowerCase().trim(),
  region.toLowerCase().trim(),
  gardenType.toLowerCase().trim()
)
hash = MD5(normalized)
```

Код для генерации хэша сохранен в `PlantService.java` (закомментирован) на случай необходимости в будущем.

## API

### Аутентификация
- Политика пароля: **ровно 6 символов**, обязательно наличие букв и цифр
- DTO:
  - Регистрация: `RegisterRequest { email, password(6), name, region, gardenType }`
  - Логин: `LoginRequest { email, password(6) }`

- **POST** `/api/auth/register`
  - Body: `RegisterRequest`
  - Response: `AuthResponse { token, userId, email, name, region, gardenType }`

- **POST** `/api/auth/login`
  - Body: `LoginRequest`
  - Response: `AuthResponse { token, userId, email, name, region, gardenType }`

- Сброс пароля (MVP):
  - **POST** `/api/auth/password/reset-request`
    - Body: `PasswordResetRequest { email }`
    - Response: строковый токен (в проде отправлять по email)
  - **POST** `/api/auth/password/reset-confirm`
    - Body: `PasswordResetConfirmRequest { token, newPassword(6) }`
    - Response: `"Password updated"`

Пример:
```bash
curl -X POST http://localhost:8080/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "email":"user@example.com",
    "password":"a1b2c3",
    "region":"Московская область",
    "gardenType":"Теплица",
    "name":"Иван"
  }'

curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "email":"user@example.com",
    "password":"a1b2c3"
  }'

# reset (MVP)
curl -X POST http://localhost:8080/api/auth/password/reset-request \
  -H "Content-Type: application/json" \
  -d '{ "email":"user@example.com" }'

curl -X POST http://localhost:8080/api/auth/password/reset-confirm \
  -H "Content-Type: application/json" \
  -d '{ "token":"<TOKEN>", "newPassword":"a1b2d4" }'
```

### Растения
- Требуется JWT (кроме `/api/auth/**`).

- **POST** `/api/plants`
  - Body: `AddPlantRequest { culture, name, variety, plantingDate, growthStage }`
  - Region/gardenType берутся из профиля пользователя.
  - Привязка к существующему `CarePlan` по поиску в JSONB полях.

- **GET** `/api/plants`
  - Возвращает список всех растений пользователя.

- **DELETE** `/api/plants/{plantId}`
  - Удаляет растение (только если принадлежит пользователю).

- **GET** `/api/plants/{plantId}/care-plan`
  - Возвращает сокращённый график подкормок, извлечённый из `aiGeneratedPlan`.

Пример:
```bash
curl -H "Authorization: Bearer <JWT>" \
  http://localhost:8080/api/plants/{plantId}/care-plan
```

### Планы ухода (публичные для фронтенда)
- **GET** `/api/care-plans/generate-hash?culture=...&region=...&gardenType=...`
  - Возвращает строку хэша (MD5) для заданных параметров.

- **GET** `/api/care-plans/exists?culture=...&region=...&gardenType=...`
  - Возвращает `true/false`, есть ли план для этих параметров.

- **GET** `/api/care-plans/by-params?culture=...&region=...&gardenType=...`
  - Возвращает JSON плана ухода по параметрам или `404`, если не найден.

- **GET** `/api/care-plans/by-hash?hash=...`
  - Возвращает JSON плана ухода по хэшу или `404`, если не найден.

Примеры:
```bash
curl "http://localhost:8080/api/care-plans/generate-hash?culture=Огурец&region=Московская%20область&gardenType=Теплица"
curl "http://localhost:8080/api/care-plans/exists?culture=Огурец&region=Московская%20область&gardenType=Теплица"
curl "http://localhost:8080/api/care-plans/by-params?culture=Огурец&region=Московская%20область&gardenType=Теплица"
curl "http://localhost:8080/api/care-plans/by-hash?hash=7c6b8d9e0f1a2b3c4d5e6f7a8b9c0d1e"
```

## Загрузка/импорт статических планов (MVP идея)
- Для MVP планы могут храниться предсгенерированными JSON в таблице `care_plans`.
- Возможные варианты импорта:
  - Скриптом SQL вставить заранее подготовленные JSON + соответствующий `inputHash`.
  - Добавить админ-эндпоинт импорта (пока не реализован).

## Примечания
- Параметры на кириллице в query-строке корректно обрабатываются Spring (UTF-8).
- Поиск планов ухода теперь осуществляется напрямую по JSONB полям, что устраняет проблемы с совместимостью хэш-функций.
- На фронтенде добавлен маппинг синонимов культур (например, "Томат" → "Помидор") для корректного поиска планов.

## Миграции БД
- Flyway миграция для токенов сброса пароля: `src/main/resources/db/migration/V1__create_password_reset_tokens.sql`
- PostgreSQL: используется `gen_random_uuid()` (расширение `pgcrypto`). При отсутствии — замените на `uuid_generate_v4()` из `uuid-ossp`.


