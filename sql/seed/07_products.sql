INSERT INTO t_product (
    name,
    article,
    stock_quantity,
    quantity_for_stock,
    image,
    warehouse_type,
    category_id,
    reserved_quantity
)
VALUES
('Молоко 3.2% 1л (обычный склад)', 'МЛК-REG', 0, 0, '/images/products/default.png', 'REGULAR', (SELECT id FROM t_productCategory WHERE name = 'Молоко'), 0),
('Молоко 3.2% 1л (холодильный склад)', 'МЛК-REF', 0, 0, '/images/products/default.png', 'REFRIGERATOR', (SELECT id FROM t_productCategory WHERE name = 'Молоко'), 0),

('Сыр полутвердый 0.5кг (обычный склад)', 'СЫР-REG', 0, 0, '/images/products/default.png', 'REGULAR', (SELECT id FROM t_productCategory WHERE name = 'Сыр'), 0),
('Сыр полутвердый 0.5кг (холодильный склад)', 'СЫР-REF', 0, 0, '/images/products/default.png', 'REFRIGERATOR', (SELECT id FROM t_productCategory WHERE name = 'Сыр'), 0),

('Говядина охлажденная (обычный склад)', 'ГВД-REG', 0, 0, '/images/products/default.png', 'REGULAR', (SELECT id FROM t_productCategory WHERE name = 'Говядина'), 0),
('Говядина охлажденная (холодильный склад)', 'ГВД-REF', 0, 0, '/images/products/default.png', 'REFRIGERATOR', (SELECT id FROM t_productCategory WHERE name = 'Говядина'), 0),

('Курица тушка (обычный склад)', 'КРЦ-REG', 0, 0, '/images/products/default.png', 'REGULAR', (SELECT id FROM t_productCategory WHERE name = 'Курица'), 0),
('Курица тушка (холодильный склад)', 'КРЦ-REF', 0, 0, '/images/products/default.png', 'REFRIGERATOR', (SELECT id FROM t_productCategory WHERE name = 'Курица'), 0),

('Яблоки свежие (обычный склад)', 'ЯБЛ-REG', 0, 0, '/images/products/default.png', 'REGULAR', (SELECT id FROM t_productCategory WHERE name = 'Яблоки'), 0),
('Яблоки свежие (холодильный склад)', 'ЯБЛ-REF', 0, 0, '/images/products/default.png', 'REFRIGERATOR', (SELECT id FROM t_productCategory WHERE name = 'Яблоки'), 0),

('Огурцы свежие (обычный склад)', 'ОГУ-REG', 0, 0, '/images/products/default.png', 'REGULAR', (SELECT id FROM t_productCategory WHERE name = 'Огурцы'), 0),
('Огурцы свежие (холодильный склад)', 'ОГУ-REF', 0, 0, '/images/products/default.png', 'REFRIGERATOR', (SELECT id FROM t_productCategory WHERE name = 'Огурцы'), 0);
