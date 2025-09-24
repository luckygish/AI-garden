# Безопасность проекта

## 🔐 Защита API ключей

### ✅ Что НЕ должно попадать в Git:

1. **API ключи и токены:**
   - DeepSeek API ключи
   - JWT секреты
   - Пароли к базе данных
   - Другие чувствительные данные

2. **Файлы конфигурации с секретами:**
   - `.env` файлы
   - `application-local.properties`
   - `application-prod.properties`
   - `secrets.properties`

3. **Логи и временные файлы:**
   - `*.log` файлы
   - `*.tmp` файлы
   - `build/` директории
   - `.gradle/` директории

### ✅ Что ДОЛЖНО быть в Git:

1. **Примеры конфигурации:**
   - `env.example` - пример переменных окружения
   - `application.properties` - основная конфигурация (без секретов)
   - `DEEPSEEK_SETUP.md` - инструкции по настройке

2. **Исходный код:**
   - Все `.java` файлы
   - Миграции базы данных
   - Документация

## 🛡️ Рекомендации по безопасности

### 1. Использование переменных окружения

**✅ Правильно:**
```bash
# Установка переменной окружения
$env:DEEPSEEK_API_KEY="your_api_key_here"

# Использование в коде
@Value("${deepseek.api.key:}")
private String apiKey;
```

**❌ Неправильно:**
```java
// НЕ делайте так!
private String apiKey = "sk-5e063c04f63646cf833c9fa231f9d6c0";
```

### 2. Настройка .gitignore

Убедитесь, что в `.gitignore` есть:
```
.env
*.log
build/
.gradle/
application-*.properties
secrets.properties
```

### 3. Проверка перед коммитом

Перед каждым коммитом проверяйте:
```bash
# Проверить, что не добавляете секреты
git status
git diff --cached

# Убедиться, что .env файлы не попадают в коммит
git check-ignore .env
```

## 🔧 Настройка для разработки

### Windows (PowerShell)
```powershell
# Установить API ключ
$env:DEEPSEEK_API_KEY="your_api_key_here"

# Проверить
echo $env:DEEPSEEK_API_KEY

# Запустить приложение
.\gradlew bootRun
```

### Windows (Command Prompt)
```cmd
set DEEPSEEK_API_KEY=your_api_key_here
echo %DEEPSEEK_API_KEY%
.\gradlew bootRun
```

### Linux/macOS
```bash
export DEEPSEEK_API_KEY="your_api_key_here"
echo $DEEPSEEK_API_KEY
./gradlew bootRun
```

## 🚨 Если секрет попал в Git

### 1. Немедленно смените ключ
- Перейдите на [DeepSeek Platform](https://platform.deepseek.com/)
- Создайте новый API ключ
- Удалите старый ключ

### 2. Удалите из истории Git
```bash
# Удалить файл из истории
git filter-branch --force --index-filter \
  "git rm --cached --ignore-unmatch path/to/file" \
  --prune-empty --tag-name-filter cat -- --all

# Принудительно обновить удаленный репозиторий
git push origin --force --all
```

### 3. Обновите переменные окружения
```bash
# Установите новый ключ
$env:DEEPSEEK_API_KEY="new_api_key_here"
```

## 📋 Чек-лист безопасности

- [ ] API ключи не захардкожены в коде
- [ ] Используются переменные окружения
- [ ] `.env` файлы в `.gitignore`
- [ ] `application-*.properties` с секретами в `.gitignore`
- [ ] Логи не попадают в репозиторий
- [ ] Временные файлы игнорируются
- [ ] Документация по безопасности создана
- [ ] Команда знает о правилах безопасности

## 🔍 Мониторинг

Регулярно проверяйте:
1. Логи приложения на предмет утечек
2. Историю Git на предмет случайных коммитов секретов
3. Активность API ключей
4. Доступы к репозиторию

## 📞 Контакты

При обнаружении уязвимостей:
1. Немедленно смените все ключи
2. Сообщите команде
3. Исправьте проблему
4. Обновите документацию
