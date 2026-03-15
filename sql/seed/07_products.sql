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
-- Молоко
('Молоко сухое цельное 500г', 'МЛК-001', 0, 0, '/images/products/default.png', 'REGULAR', (SELECT id FROM t_product_category WHERE name = 'Молоко'), 0),
('Молоко пастеризованное 3.2% 1л', 'МЛК-002', 0, 0, '/images/products/default.png', 'REFRIGERATOR', (SELECT id FROM t_product_category WHERE name = 'Молоко'), 0),

-- Сыр
('Сыр плавленый «Дружба» 200г', 'СЫР-001', 0, 0, '/images/products/default.png', 'REGULAR', (SELECT id FROM t_product_category WHERE name = 'Сыр'), 0),
('Сыр полутвёрдый «Российский» 300г', 'СЫР-002', 0, 0, '/images/products/default.png', 'REFRIGERATOR', (SELECT id FROM t_product_category WHERE name = 'Сыр'), 0),

-- Говядина
('Говядина тушёная ГОСТ 338г', 'ГВД-001', 0, 0, '/images/products/default.png', 'REGULAR', (SELECT id FROM t_product_category WHERE name = 'Говядина'), 0),
('Говядина охлаждённая вырезка 1кг', 'ГВД-002', 0, 0, '/images/products/default.png', 'REFRIGERATOR', (SELECT id FROM t_product_category WHERE name = 'Говядина'), 0),

-- Курица
('Курица тушёная ГОСТ 325г', 'КРЦ-001', 0, 0, '/images/products/default.png', 'REGULAR', (SELECT id FROM t_product_category WHERE name = 'Курица'), 0),
('Филе куриное охлаждённое 0.8кг', 'КРЦ-002', 0, 0, '/images/products/default.png', 'REFRIGERATOR', (SELECT id FROM t_product_category WHERE name = 'Курица'), 0),

-- Яблоки
('Яблоки сушёные 200г', 'ЯБЛ-001', 0, 0, '/images/products/default.png', 'REGULAR', (SELECT id FROM t_product_category WHERE name = 'Яблоки'), 0),
('Яблоки свежие «Гала» 1кг', 'ЯБЛ-002', 0, 0, '/images/products/default.png', 'REFRIGERATOR', (SELECT id FROM t_product_category WHERE name = 'Яблоки'), 0),

-- Огурцы
('Огурцы маринованные 720мл', 'ОГУ-001', 0, 0, '/images/products/default.png', 'REGULAR', (SELECT id FROM t_product_category WHERE name = 'Огурцы'), 0),
('Огурцы свежие короткоплодные 1кг', 'ОГУ-002', 0, 0, '/images/products/default.png', 'REFRIGERATOR', (SELECT id FROM t_product_category WHERE name = 'Огурцы'), 0);
