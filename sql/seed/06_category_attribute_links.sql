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
  ON a.name IN (
    'Жирность молока (обычный склад)', 'Объем молока (обычный склад)',
    'Жирность молока (холодильный склад)', 'Объем молока (холодильный склад)'
  )
WHERE c.name = 'Молоко';

-- Сыр
INSERT INTO category_attribute (category_id, attribute_id)
SELECT c.id, a.id
FROM t_product_category c
JOIN t_product_attribute a
  ON a.name IN (
    'Выдержка сыра (обычный склад)', 'Тип сыра (обычный склад)',
    'Выдержка сыра (холодильный склад)', 'Тип сыра (холодильный склад)'
  )
WHERE c.name = 'Сыр';

-- Говядина
INSERT INTO category_attribute (category_id, attribute_id)
SELECT c.id, a.id
FROM t_product_category c
JOIN t_product_attribute a
  ON a.name IN (
    'Категория говядины (обычный склад)', 'Вес куска говядины (обычный склад)',
    'Категория говядины (холодильный склад)', 'Вес куска говядины (холодильный склад)'
  )
WHERE c.name = 'Говядина';

-- Курица
INSERT INTO category_attribute (category_id, attribute_id)
SELECT c.id, a.id
FROM t_product_category c
JOIN t_product_attribute a
  ON a.name IN (
    'Тип разделки курицы (обычный склад)', 'Вес упаковки курицы (обычный склад)',
    'Тип разделки курицы (холодильный склад)', 'Вес упаковки курицы (холодильный склад)'
  )
WHERE c.name = 'Курица';

-- Яблоки
INSERT INTO category_attribute (category_id, attribute_id)
SELECT c.id, a.id
FROM t_product_category c
JOIN t_product_attribute a
  ON a.name IN (
    'Сорт яблок (обычный склад)', 'Калибр яблок (обычный склад)',
    'Сорт яблок (холодильный склад)', 'Калибр яблок (холодильный склад)'
  )
WHERE c.name = 'Яблоки';

-- Огурцы
INSERT INTO category_attribute (category_id, attribute_id)
SELECT c.id, a.id
FROM t_product_category c
JOIN t_product_attribute a
  ON a.name IN (
    'Тип огурцов (обычный склад)', 'Длина огурца (обычный склад)',
    'Тип огурцов (холодильный склад)', 'Длина огурца (холодильный склад)'
  )
WHERE c.name = 'Огурцы';
