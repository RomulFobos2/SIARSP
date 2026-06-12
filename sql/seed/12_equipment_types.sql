-- ВАЖНО: Типы оборудования также создаются приложением через RoleRunner.
-- Имена ДОЛЖНЫ совпадать с RoleRunner.createDefaultEquipmentTypesIfNotExist().

INSERT IGNORE INTO t_equipment_type (name)
VALUES
('Стеллаж'),
('Холодильная камера'),
('Поддон'),
('Весы'),
('Погрузчик'),
('Прочее');
