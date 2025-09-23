-- Удаляем поле input_hash из таблицы care_plans
ALTER TABLE care_plans DROP COLUMN IF EXISTS input_hash;

-- Добавляем уникальный индекс на комбинацию параметров
-- Это предотвратит дублирование планов для одинаковых параметров
CREATE UNIQUE INDEX IF NOT EXISTS idx_care_plans_unique_params 
ON care_plans USING btree (
    (input_parameters->>'culture'),
    (input_parameters->>'region'),
    (input_parameters->>'garden_type')
);

-- Добавляем комментарий к индексу
COMMENT ON INDEX idx_care_plans_unique_params IS 'Уникальный индекс для предотвращения дублирования планов ухода с одинаковыми параметрами';
