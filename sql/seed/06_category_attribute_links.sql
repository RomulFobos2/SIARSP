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
  ON a.name IN ('Жирность', 'Объём')
WHERE c.name = 'Молоко';

-- Сыр
INSERT INTO category_attribute (category_id, attribute_id)
SELECT c.id, a.id
FROM t_product_category c
JOIN t_product_attribute a
  ON a.name IN ('Массовая доля жира', 'Срок созревания')
WHERE c.name = 'Сыр';

-- Говядина
INSERT INTO category_attribute (category_id, attribute_id)
SELECT c.id, a.id
FROM t_product_category c
JOIN t_product_attribute a
  ON a.name IN ('Сорт мяса', 'Масса нетто')
WHERE c.name = 'Говядина';

-- Курица
INSERT INTO category_attribute (category_id, attribute_id)
SELECT c.id, a.id
FROM t_product_category c
JOIN t_product_attribute a
  ON a.name IN ('Вид разделки', 'Вес упаковки')
WHERE c.name = 'Курица';

-- Яблоки
INSERT INTO category_attribute (category_id, attribute_id)
SELECT c.id, a.id
FROM t_product_category c
JOIN t_product_attribute a
  ON a.name IN ('Сорт', 'Калибр плода')
WHERE c.name = 'Яблоки';

-- Огурцы
INSERT INTO category_attribute (category_id, attribute_id)
SELECT c.id, a.id
FROM t_product_category c
JOIN t_product_attribute a
  ON a.name IN ('Тип плода', 'Средняя длина плода')
WHERE c.name = 'Огурцы';
