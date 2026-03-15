INSERT INTO t_product_attribute_value (value, product_id, attribute_id)
SELECT x.val, p.id, a.id
FROM (
    SELECT 'МЛК-REG' article, 'Длина упаковки' attr, '10' val UNION ALL
    SELECT 'МЛК-REG', 'Ширина упаковки', '10' UNION ALL
    SELECT 'МЛК-REG', 'Высота упаковки', '23' UNION ALL
    SELECT 'МЛК-REG', 'Срок годности', '10' UNION ALL
    SELECT 'МЛК-REG', 'Жирность молока', '3.2' UNION ALL
    SELECT 'МЛК-REG', 'Объем молока', '1.0' UNION ALL

    SELECT 'МЛК-REF', 'Длина упаковки', '10' UNION ALL
    SELECT 'МЛК-REF', 'Ширина упаковки', '10' UNION ALL
    SELECT 'МЛК-REF', 'Высота упаковки', '23' UNION ALL
    SELECT 'МЛК-REF', 'Срок годности', '14' UNION ALL
    SELECT 'МЛК-REF', 'Жирность молока', '3.2' UNION ALL
    SELECT 'МЛК-REF', 'Объем молока', '1.0' UNION ALL

    SELECT 'СЫР-REG', 'Длина упаковки', '18' UNION ALL
    SELECT 'СЫР-REG', 'Ширина упаковки', '12' UNION ALL
    SELECT 'СЫР-REG', 'Высота упаковки', '8' UNION ALL
    SELECT 'СЫР-REG', 'Срок годности', '60' UNION ALL
    SELECT 'СЫР-REG', 'Выдержка сыра', '3' UNION ALL
    SELECT 'СЫР-REG', 'Тип сыра', 'Полутвердый' UNION ALL

    SELECT 'СЫР-REF', 'Длина упаковки', '18' UNION ALL
    SELECT 'СЫР-REF', 'Ширина упаковки', '12' UNION ALL
    SELECT 'СЫР-REF', 'Высота упаковки', '8' UNION ALL
    SELECT 'СЫР-REF', 'Срок годности', '90' UNION ALL
    SELECT 'СЫР-REF', 'Выдержка сыра', '6' UNION ALL
    SELECT 'СЫР-REF', 'Тип сыра', 'Твердый' UNION ALL

    SELECT 'ГВД-REG', 'Длина упаковки', '28' UNION ALL
    SELECT 'ГВД-REG', 'Ширина упаковки', '20' UNION ALL
    SELECT 'ГВД-REG', 'Высота упаковки', '10' UNION ALL
    SELECT 'ГВД-REG', 'Срок годности', '7' UNION ALL
    SELECT 'ГВД-REG', 'Категория говядины', 'Первая' UNION ALL
    SELECT 'ГВД-REG', 'Вес куска говядины', '1.5' UNION ALL

    SELECT 'ГВД-REF', 'Длина упаковки', '28' UNION ALL
    SELECT 'ГВД-REF', 'Ширина упаковки', '20' UNION ALL
    SELECT 'ГВД-REF', 'Высота упаковки', '10' UNION ALL
    SELECT 'ГВД-REF', 'Срок годности', '12' UNION ALL
    SELECT 'ГВД-REF', 'Категория говядины', 'Высшая' UNION ALL
    SELECT 'ГВД-REF', 'Вес куска говядины', '1.8' UNION ALL

    SELECT 'КРЦ-REG', 'Длина упаковки', '30' UNION ALL
    SELECT 'КРЦ-REG', 'Ширина упаковки', '20' UNION ALL
    SELECT 'КРЦ-REG', 'Высота упаковки', '12' UNION ALL
    SELECT 'КРЦ-REG', 'Срок годности', '5' UNION ALL
    SELECT 'КРЦ-REG', 'Тип разделки курицы', 'Тушка' UNION ALL
    SELECT 'КРЦ-REG', 'Вес упаковки курицы', '1.7' UNION ALL

    SELECT 'КРЦ-REF', 'Длина упаковки', '30' UNION ALL
    SELECT 'КРЦ-REF', 'Ширина упаковки', '20' UNION ALL
    SELECT 'КРЦ-REF', 'Высота упаковки', '12' UNION ALL
    SELECT 'КРЦ-REF', 'Срок годности', '9' UNION ALL
    SELECT 'КРЦ-REF', 'Тип разделки курицы', 'Филе' UNION ALL
    SELECT 'КРЦ-REF', 'Вес упаковки курицы', '1.2' UNION ALL

    SELECT 'ЯБЛ-REG', 'Длина упаковки', '35' UNION ALL
    SELECT 'ЯБЛ-REG', 'Ширина упаковки', '25' UNION ALL
    SELECT 'ЯБЛ-REG', 'Высота упаковки', '18' UNION ALL
    SELECT 'ЯБЛ-REG', 'Срок годности', '20' UNION ALL
    SELECT 'ЯБЛ-REG', 'Сорт яблок', 'Гренни Смит' UNION ALL
    SELECT 'ЯБЛ-REG', 'Калибр яблок', '75' UNION ALL

    SELECT 'ЯБЛ-REF', 'Длина упаковки', '35' UNION ALL
    SELECT 'ЯБЛ-REF', 'Ширина упаковки', '25' UNION ALL
    SELECT 'ЯБЛ-REF', 'Высота упаковки', '18' UNION ALL
    SELECT 'ЯБЛ-REF', 'Срок годности', '35' UNION ALL
    SELECT 'ЯБЛ-REF', 'Сорт яблок', 'Фуджи' UNION ALL
    SELECT 'ЯБЛ-REF', 'Калибр яблок', '80' UNION ALL

    SELECT 'ОГУ-REG', 'Длина упаковки', '32' UNION ALL
    SELECT 'ОГУ-REG', 'Ширина упаковки', '24' UNION ALL
    SELECT 'ОГУ-REG', 'Высота упаковки', '16' UNION ALL
    SELECT 'ОГУ-REG', 'Срок годности', '7' UNION ALL
    SELECT 'ОГУ-REG', 'Тип огурцов', 'Короткоплодные' UNION ALL
    SELECT 'ОГУ-REG', 'Длина огурца', '11' UNION ALL

    SELECT 'ОГУ-REF', 'Длина упаковки', '32' UNION ALL
    SELECT 'ОГУ-REF', 'Ширина упаковки', '24' UNION ALL
    SELECT 'ОГУ-REF', 'Высота упаковки', '16' UNION ALL
    SELECT 'ОГУ-REF', 'Срок годности', '12' UNION ALL
    SELECT 'ОГУ-REF', 'Тип огурцов', 'Длинноплодные' UNION ALL
    SELECT 'ОГУ-REF', 'Длина огурца', '18'
) x
JOIN t_product p ON p.article = x.article
JOIN t_product_attribute a ON a.name = x.attr;
