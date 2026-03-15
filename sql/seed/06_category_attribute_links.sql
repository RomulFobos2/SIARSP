-- Общие 4 атрибута для всех категорий
INSERT INTO category_attribute (category_id, attribute_id)
SELECT c.id, a.id
FROM t_product_category c
JOIN t_product_attribute a
  ON a.name IN ('Длина упаковки', 'Ширина упаковки', 'Высота упаковки', 'Срок годности');

-- Молоко
INSERT INTO category_attribute (category_id, attribute_id)
SELECT c.id, a.id
FROM t_product_category c
JOIN t_product_attribute a
  ON a.name IN ('Жирность молока', 'Объем молока')
WHERE c.name = 'Молоко';

-- Сыр
INSERT INTO category_attribute (category_id, attribute_id)
SELECT c.id, a.id
FROM t_product_category c
JOIN t_product_attribute a
  ON a.name IN ('Выдержка сыра', 'Тип сыра')
WHERE c.name = 'Сыр';

-- Говядина
INSERT INTO category_attribute (category_id, attribute_id)
SELECT c.id, a.id
FROM t_product_category c
JOIN t_product_attribute a
  ON a.name IN ('Категория говядины', 'Вес куска говядины')
WHERE c.name = 'Говядина';

-- Курица
INSERT INTO category_attribute (category_id, attribute_id)
SELECT c.id, a.id
FROM t_product_category c
JOIN t_product_attribute a
  ON a.name IN ('Тип разделки курицы', 'Вес упаковки курицы')
WHERE c.name = 'Курица';

-- Яблоки
INSERT INTO category_attribute (category_id, attribute_id)
SELECT c.id, a.id
FROM t_product_category c
JOIN t_product_attribute a
  ON a.name IN ('Сорт яблок', 'Калибр яблок')
WHERE c.name = 'Яблоки';

-- Огурцы
INSERT INTO category_attribute (category_id, attribute_id)
SELECT c.id, a.id
FROM t_product_category c
JOIN t_product_attribute a
  ON a.name IN ('Тип огурцов', 'Длина огурца')
WHERE c.name = 'Огурцы';
