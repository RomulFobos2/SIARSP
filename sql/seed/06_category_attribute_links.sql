-- Общие 4 атрибута для всех категорий
INSERT INTO category_attribute (category_id, attribute_id)
SELECT c.id, a.id
FROM t_productCategory c
JOIN t_productAttribute a
  ON a.name IN ('Длина упаковки', 'Ширина упаковки', 'Высота упаковки', 'Срок годности');

-- Молоко
INSERT INTO category_attribute (category_id, attribute_id)
SELECT c.id, a.id
FROM t_productCategory c
JOIN t_productAttribute a
  ON a.name IN (
    'Жирность молока МЛК-REG', 'Объем молока МЛК-REG',
    'Жирность молока МЛК-REF', 'Объем молока МЛК-REF'
  )
WHERE c.name = 'Молоко';

-- Сыр
INSERT INTO category_attribute (category_id, attribute_id)
SELECT c.id, a.id
FROM t_productCategory c
JOIN t_productAttribute a
  ON a.name IN (
    'Выдержка сыра СЫР-REG', 'Тип сыра СЫР-REG',
    'Выдержка сыра СЫР-REF', 'Тип сыра СЫР-REF'
  )
WHERE c.name = 'Сыр';

-- Говядина
INSERT INTO category_attribute (category_id, attribute_id)
SELECT c.id, a.id
FROM t_productCategory c
JOIN t_productAttribute a
  ON a.name IN (
    'Категория говядины ГВД-REG', 'Вес куска говядины ГВД-REG',
    'Категория говядины ГВД-REF', 'Вес куска говядины ГВД-REF'
  )
WHERE c.name = 'Говядина';

-- Курица
INSERT INTO category_attribute (category_id, attribute_id)
SELECT c.id, a.id
FROM t_productCategory c
JOIN t_productAttribute a
  ON a.name IN (
    'Тип разделки курицы КРЦ-REG', 'Вес упаковки курицы КРЦ-REG',
    'Тип разделки курицы КРЦ-REF', 'Вес упаковки курицы КРЦ-REF'
  )
WHERE c.name = 'Курица';

-- Яблоки
INSERT INTO category_attribute (category_id, attribute_id)
SELECT c.id, a.id
FROM t_productCategory c
JOIN t_productAttribute a
  ON a.name IN (
    'Сорт яблок ЯБЛ-REG', 'Калибр яблок ЯБЛ-REG',
    'Сорт яблок ЯБЛ-REF', 'Калибр яблок ЯБЛ-REF'
  )
WHERE c.name = 'Яблоки';

-- Огурцы
INSERT INTO category_attribute (category_id, attribute_id)
SELECT c.id, a.id
FROM t_productCategory c
JOIN t_productAttribute a
  ON a.name IN (
    'Тип огурцов ОГУ-REG', 'Длина огурца ОГУ-REG',
    'Тип огурцов ОГУ-REF', 'Длина огурца ОГУ-REF'
  )
WHERE c.name = 'Огурцы';
