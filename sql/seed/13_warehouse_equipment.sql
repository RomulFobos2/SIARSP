INSERT INTO t_warehouseEquipment (
    name,
    serial_number,
    production_date,
    useful_life_years,
    equipment_type_id,
    status,
    warehouse_id
)
VALUES
('Погрузчик Toyota 8FG-1', 'FLT-0001', '2022-03-10', 8,
 (SELECT id FROM t_equipmentType WHERE name = 'Погрузчик'), 'IN_USE',
 (SELECT id FROM t_warehouse WHERE name = 'Основной склад')),

('Погрузчик Toyota 8FG-2', 'FLT-0002', '2023-06-20', 8,
 (SELECT id FROM t_equipmentType WHERE name = 'Погрузчик'), 'IN_USE',
 (SELECT id FROM t_warehouse WHERE name = 'Холодильный склад')),

('Холодильная камера Polair KXH-01', 'REF-1001', '2021-01-15', 10,
 (SELECT id FROM t_equipmentType WHERE name = 'Холодильная камера'), 'IN_USE',
 (SELECT id FROM t_warehouse WHERE name = 'Холодильный склад')),

('Холодильная камера Polair KXH-02', 'REF-1002', '2022-08-01', 10,
 (SELECT id FROM t_equipmentType WHERE name = 'Холодильная камера'), 'IN_USE',
 (SELECT id FROM t_warehouse WHERE name = 'Холодильный склад')),

('Весы МК-А21-1', 'SCL-5001', '2023-02-12', 7,
 (SELECT id FROM t_equipmentType WHERE name = 'Электронные весы'), 'IN_USE',
 (SELECT id FROM t_warehouse WHERE name = 'Основной склад')),

('Весы МК-А21-2', 'SCL-5002', '2023-04-18', 7,
 (SELECT id FROM t_equipmentType WHERE name = 'Электронные весы'), 'IN_USE',
 (SELECT id FROM t_warehouse WHERE name = 'Холодильный склад'));
