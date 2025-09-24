# Agriculture Importer - DeepSeek Integration

## Настройка DeepSeek API

### 1. Получение API ключа
1. Зарегистрируйтесь на [DeepSeek](https://platform.deepseek.com/)
2. Перейдите в раздел API Keys
3. Создайте новый API ключ
4. Скопируйте ключ (начинается с `sk-`)

### 2. Настройка переменных окружения

#### Вариант 1: Переменные окружения системы
```bash
# Windows (PowerShell)
$env:DEEPSEEK_API_KEY="sk-your-api-key-here"

# Windows (CMD)
set DEEPSEEK_API_KEY=sk-your-api-key-here

# Linux/Mac
export DEEPSEEK_API_KEY="sk-your-api-key-here"
```

#### Вариант 2: Файл конфигурации
1. Скопируйте `config.env.example` в `config.env`
2. Замените `YOUR_API_KEY_HERE` на ваш реальный API ключ
3. Запустите приложение

### 3. Проверка интеграции

#### Проверка статуса API
```bash
curl http://localhost:8080/api/deepseek/status
```

Ожидаемый ответ:
```json
{
  "available": true,
  "message": "DeepSeek API настроен и готов к использованию"
}
```

#### Тестирование запроса
```bash
# Простой тест с фиксированными параметрами
curl -X POST "http://localhost:8080/api/deepseek/simple-test"

# Тест с кастомными параметрами
curl -X POST "http://localhost:8080/api/deepseek/test?culture=Томат&region=Московская область&gardenType=открытый грунт"
```

### 4. Безопасность

⚠️ **ВАЖНО**: Никогда не коммитьте API ключи в репозиторий!

- Файл `config.env` добавлен в `.gitignore`
- API ключи не должны быть в `application.properties`
- Используйте переменные окружения для продакшена

### 5. Структура проекта

```
src/main/java/com/agriculture/
├── controller/
│   └── DeepSeekController.java    # REST API endpoints
├── services/
│   └── DeepSeekService.java      # Основная логика интеграции
└── configs/
    └── SecurityConfig.java       # CORS настройки
```

### 6. Конфигурация

Основные параметры в `application.properties`:

```properties
# JWT Configuration
jwt.secret=7A2B4C8E1F3D5A9B0C6E8F2A4D7B1E5C3A9F8D2B6E1C7A0F4D9B8E3C5A1F6E2D7B
jwt.expiration-ms=86400000

# DeepSeek API Configuration
deepseek.api.url=https://api.deepseek.com/v1/chat/completions
deepseek.api.key=${DEEPSEEK_API_KEY}  # Из переменной окружения
deepseek.api.model=deepseek-chat
deepseek.api.timeout=240000  # 4 минуты
deepseek.api.max-retries=5   # 5 попыток
deepseek.api.retry-delay=3000 # 3 секунды между попытками
deepseek.api.max-tokens=8192  # Максимум токенов в ответе (лимит DeepSeek API)
```

### 7. Мониторинг

Приложение логирует:
- Статус подключения к API
- Время выполнения запросов
- Ошибки и повторные попытки
- Размер промтов и ответов

### 8. Troubleshooting

#### Ошибка "API ключ не настроен"
- Проверьте переменную окружения `DEEPSEEK_API_KEY`
- Убедитесь, что ключ начинается с `sk-`

#### Ошибка "Invalid max_tokens value"
- DeepSeek API поддерживает максимум 8192 токена
- Убедитесь, что `deepseek.api.max-tokens` не превышает 8192
- Рекомендуемое значение: 8192

#### Ошибка "Ошибка HTTP запроса"
- Проверьте интернет-соединение
- Убедитесь, что API ключ действителен
- Проверьте лимиты API

#### Таймауты
- Увеличьте `deepseek.api.timeout` в настройках
- Проверьте стабильность интернет-соединения