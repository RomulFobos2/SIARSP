INSERT INTO t_productCategory (name, global_product_category_id)
VALUES
('Молоко', (SELECT id FROM t_globalProductCategory WHERE name = 'Молочная продукция')),
('Сыр', (SELECT id FROM t_globalProductCategory WHERE name = 'Молочная продукция')),

('Говядина', (SELECT id FROM t_globalProductCategory WHERE name = 'Мясная продукция')),
('Курица', (SELECT id FROM t_globalProductCategory WHERE name = 'Мясная продукция')),

('Яблоки', (SELECT id FROM t_globalProductCategory WHERE name = 'Овощи и фрукты')),
('Огурцы', (SELECT id FROM t_globalProductCategory WHERE name = 'Овощи и фрукты'));
