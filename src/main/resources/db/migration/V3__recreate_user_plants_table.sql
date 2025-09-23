-- Пересоздание таблицы user_plants с полем culture
DROP TABLE IF EXISTS user_plants CASCADE;

CREATE TABLE user_plants (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    care_plan_id UUID NOT NULL REFERENCES care_plans(id) ON DELETE CASCADE,
    name VARCHAR(255) NOT NULL,
    variety VARCHAR(255),
    culture VARCHAR(255) NOT NULL, -- Каноническое название культуры
    planting_date DATE NOT NULL,
    growth_stage VARCHAR(255),
    created_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP
);

-- Создаем индексы для оптимизации
CREATE INDEX idx_user_plants_user_id ON user_plants(user_id);
CREATE INDEX idx_user_plants_care_plan_id ON user_plants(care_plan_id);
CREATE INDEX idx_user_plants_culture ON user_plants(culture);

-- Комментарии к таблице и полям
COMMENT ON TABLE user_plants IS 'Таблица растений пользователей';
COMMENT ON COLUMN user_plants.id IS 'Уникальный идентификатор растения';
COMMENT ON COLUMN user_plants.user_id IS 'Ссылка на пользователя-владельца';
COMMENT ON COLUMN user_plants.care_plan_id IS 'Ссылка на план ухода';
COMMENT ON COLUMN user_plants.name IS 'Название растения (как ввел пользователь)';
COMMENT ON COLUMN user_plants.variety IS 'Сорт растения';
COMMENT ON COLUMN user_plants.culture IS 'Каноническое название культуры для поиска планов ухода';
COMMENT ON COLUMN user_plants.planting_date IS 'Дата посадки/посева';
COMMENT ON COLUMN user_plants.growth_stage IS 'Текущая стадия роста';
