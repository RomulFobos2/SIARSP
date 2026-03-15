INSERT INTO t_storageZone (label, length, width, height, shelf_id)
VALUES
('A1', 200, 80, 40, (SELECT s.id FROM t_shelf s JOIN t_warehouse w ON w.id = s.warehouse_id WHERE s.code = 'A' AND w.name = 'Основной склад')),
('A2', 200, 80, 40, (SELECT s.id FROM t_shelf s JOIN t_warehouse w ON w.id = s.warehouse_id WHERE s.code = 'A' AND w.name = 'Основной склад')),
('B1', 200, 80, 40, (SELECT s.id FROM t_shelf s JOIN t_warehouse w ON w.id = s.warehouse_id WHERE s.code = 'B' AND w.name = 'Основной склад')),
('B2', 200, 80, 40, (SELECT s.id FROM t_shelf s JOIN t_warehouse w ON w.id = s.warehouse_id WHERE s.code = 'B' AND w.name = 'Основной склад')),
('C1', 200, 80, 40, (SELECT s.id FROM t_shelf s JOIN t_warehouse w ON w.id = s.warehouse_id WHERE s.code = 'C' AND w.name = 'Основной склад')),
('C2', 200, 80, 40, (SELECT s.id FROM t_shelf s JOIN t_warehouse w ON w.id = s.warehouse_id WHERE s.code = 'C' AND w.name = 'Основной склад')),

('A1', 200, 80, 40, (SELECT s.id FROM t_shelf s JOIN t_warehouse w ON w.id = s.warehouse_id WHERE s.code = 'A' AND w.name = 'Холодильный склад')),
('A2', 200, 80, 40, (SELECT s.id FROM t_shelf s JOIN t_warehouse w ON w.id = s.warehouse_id WHERE s.code = 'A' AND w.name = 'Холодильный склад')),
('B1', 200, 80, 40, (SELECT s.id FROM t_shelf s JOIN t_warehouse w ON w.id = s.warehouse_id WHERE s.code = 'B' AND w.name = 'Холодильный склад')),
('B2', 200, 80, 40, (SELECT s.id FROM t_shelf s JOIN t_warehouse w ON w.id = s.warehouse_id WHERE s.code = 'B' AND w.name = 'Холодильный склад')),
('C1', 200, 80, 40, (SELECT s.id FROM t_shelf s JOIN t_warehouse w ON w.id = s.warehouse_id WHERE s.code = 'C' AND w.name = 'Холодильный склад')),
('C2', 200, 80, 40, (SELECT s.id FROM t_shelf s JOIN t_warehouse w ON w.id = s.warehouse_id WHERE s.code = 'C' AND w.name = 'Холодильный склад'));
