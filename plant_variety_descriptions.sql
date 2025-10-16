-- Создание таблицы для хранения описаний сортов растений
CREATE TABLE IF NOT EXISTS plant_variety_descriptions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    culture VARCHAR(100) NOT NULL,
    variety VARCHAR(100) NOT NULL,
    description TEXT,
    ripening_period VARCHAR(200),
    plant_height VARCHAR(100),
    fruit_weight VARCHAR(100),
    yield VARCHAR(100),
    disease_resistance TEXT, -- JSON array as string
    growing_conditions TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    
    -- Уникальный индекс для комбинации культура + сорт
    UNIQUE(culture, variety)
);

-- Создание индексов для быстрого поиска
CREATE INDEX IF NOT EXISTS idx_plant_variety_descriptions_culture ON plant_variety_descriptions(culture);
CREATE INDEX IF NOT EXISTS idx_plant_variety_descriptions_variety ON plant_variety_descriptions(variety);
CREATE INDEX IF NOT EXISTS idx_plant_variety_descriptions_created_at ON plant_variety_descriptions(created_at);

-- Комментарии к таблице и колонкам
COMMENT ON TABLE plant_variety_descriptions IS 'Таблица для хранения подробных описаний сортов растений, полученных через DeepSeek API';
COMMENT ON COLUMN plant_variety_descriptions.id IS 'Уникальный идентификатор записи';
COMMENT ON COLUMN plant_variety_descriptions.culture IS 'Название культуры (например, Томат)';
COMMENT ON COLUMN plant_variety_descriptions.variety IS 'Название сорта (например, Титан)';
COMMENT ON COLUMN plant_variety_descriptions.description IS 'Подробное описание сорта';
COMMENT ON COLUMN plant_variety_descriptions.ripening_period IS 'Срок созревания';
COMMENT ON COLUMN plant_variety_descriptions.plant_height IS 'Высота растения';
COMMENT ON COLUMN plant_variety_descriptions.fruit_weight IS 'Масса плода';
COMMENT ON COLUMN plant_variety_descriptions.yield IS 'Урожайность';
COMMENT ON COLUMN plant_variety_descriptions.disease_resistance IS 'Устойчивость к болезням (JSON массив)';
COMMENT ON COLUMN plant_variety_descriptions.growing_conditions IS 'Условия выращивания';
COMMENT ON COLUMN plant_variety_descriptions.created_at IS 'Дата создания записи';
COMMENT ON COLUMN plant_variety_descriptions.updated_at IS 'Дата последнего обновления записи';
