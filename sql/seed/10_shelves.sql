INSERT INTO t_shelf (code, warehouse_id)
VALUES
('A', (SELECT id FROM t_warehouse WHERE name = 'Основной склад')),
('B', (SELECT id FROM t_warehouse WHERE name = 'Основной склад')),
('C', (SELECT id FROM t_warehouse WHERE name = 'Основной склад')),
('A', (SELECT id FROM t_warehouse WHERE name = 'Холодильный склад')),
('B', (SELECT id FROM t_warehouse WHERE name = 'Холодильный склад')),
('C', (SELECT id FROM t_warehouse WHERE name = 'Холодильный склад'));
