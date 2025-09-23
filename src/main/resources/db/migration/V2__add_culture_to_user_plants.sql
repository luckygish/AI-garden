-- Добавляем поле culture в таблицу user_plants
ALTER TABLE user_plants ADD COLUMN culture VARCHAR(255);

-- Обновляем существующие записи (если есть)
-- Для существующих растений устанавливаем culture = name
UPDATE user_plants SET culture = name WHERE culture IS NULL;
