-- Обновляем «Срок годности» с количества дней на формат даты (для существующей БД)
UPDATE t_product_attribute_value pav
JOIN t_product p ON pav.product_id = p.id
JOIN t_product_attribute a ON pav.attribute_id = a.id
SET pav.value = CASE p.article
    WHEN 'МЛК-001' THEN '2027-01-15'
    WHEN 'МЛК-002' THEN '2026-03-25'
    WHEN 'СЫР-001' THEN '2026-09-10'
    WHEN 'СЫР-002' THEN '2026-05-14'
    WHEN 'ГВД-001' THEN '2029-03-01'
    WHEN 'ГВД-002' THEN '2026-03-22'
    WHEN 'КРЦ-001' THEN '2029-02-20'
    WHEN 'КРЦ-002' THEN '2026-03-20'
    WHEN 'ЯБЛ-001' THEN '2027-03-10'
    WHEN 'ЯБЛ-002' THEN '2026-04-14'
    WHEN 'ОГУ-001' THEN '2028-03-15'
    WHEN 'ОГУ-002' THEN '2026-03-22'
END
WHERE a.name = 'Срок годности';

-- Полная вставка значений атрибутов (для чистой БД)
INSERT INTO t_product_attribute_value (value, product_id, attribute_id)
SELECT x.val, p.id, a.id
FROM (
    -- === Молоко сухое цельное 500г (МЛК-001, REGULAR) ===
    SELECT 'МЛК-001' article, 'Длина упаковки' attr, '15' val UNION ALL
    SELECT 'МЛК-001', 'Ширина упаковки', '10' UNION ALL
    SELECT 'МЛК-001', 'Высота упаковки', '20' UNION ALL
    SELECT 'МЛК-001', 'Срок годности', '2027-01-15' UNION ALL
    SELECT 'МЛК-001', 'Жирность', '25' UNION ALL
    SELECT 'МЛК-001', 'Объём', '0.5' UNION ALL

    -- === Молоко пастеризованное 3.2% 1л (МЛК-002, REFRIGERATOR) ===
    SELECT 'МЛК-002', 'Длина упаковки', '10' UNION ALL
    SELECT 'МЛК-002', 'Ширина упаковки', '10' UNION ALL
    SELECT 'МЛК-002', 'Высота упаковки', '23' UNION ALL
    SELECT 'МЛК-002', 'Срок годности', '2026-03-25' UNION ALL
    SELECT 'МЛК-002', 'Жирность', '3.2' UNION ALL
    SELECT 'МЛК-002', 'Объём', '1.0' UNION ALL

    -- === Сыр плавленый «Дружба» 200г (СЫР-001, REGULAR) ===
    SELECT 'СЫР-001', 'Длина упаковки', '10' UNION ALL
    SELECT 'СЫР-001', 'Ширина упаковки', '10' UNION ALL
    SELECT 'СЫР-001', 'Высота упаковки', '4' UNION ALL
    SELECT 'СЫР-001', 'Срок годности', '2026-09-10' UNION ALL
    SELECT 'СЫР-001', 'Массовая доля жира', '45' UNION ALL
    SELECT 'СЫР-001', 'Срок созревания', '0' UNION ALL

    -- === Сыр полутвёрдый «Российский» 300г (СЫР-002, REFRIGERATOR) ===
    SELECT 'СЫР-002', 'Длина упаковки', '18' UNION ALL
    SELECT 'СЫР-002', 'Ширина упаковки', '12' UNION ALL
    SELECT 'СЫР-002', 'Высота упаковки', '8' UNION ALL
    SELECT 'СЫР-002', 'Срок годности', '2026-05-14' UNION ALL
    SELECT 'СЫР-002', 'Массовая доля жира', '50' UNION ALL
    SELECT 'СЫР-002', 'Срок созревания', '3' UNION ALL

    -- === Говядина тушёная ГОСТ 338г (ГВД-001, REGULAR) ===
    SELECT 'ГВД-001', 'Длина упаковки', '10' UNION ALL
    SELECT 'ГВД-001', 'Ширина упаковки', '10' UNION ALL
    SELECT 'ГВД-001', 'Высота упаковки', '11' UNION ALL
    SELECT 'ГВД-001', 'Срок годности', '2029-03-01' UNION ALL
    SELECT 'ГВД-001', 'Сорт мяса', 'Высший' UNION ALL
    SELECT 'ГВД-001', 'Масса нетто', '0.338' UNION ALL

    -- === Говядина охлаждённая вырезка 1кг (ГВД-002, REFRIGERATOR) ===
    SELECT 'ГВД-002', 'Длина упаковки', '28' UNION ALL
    SELECT 'ГВД-002', 'Ширина упаковки', '20' UNION ALL
    SELECT 'ГВД-002', 'Высота упаковки', '8' UNION ALL
    SELECT 'ГВД-002', 'Срок годности', '2026-03-22' UNION ALL
    SELECT 'ГВД-002', 'Сорт мяса', 'Первый' UNION ALL
    SELECT 'ГВД-002', 'Масса нетто', '1.0' UNION ALL

    -- === Курица тушёная ГОСТ 325г (КРЦ-001, REGULAR) ===
    SELECT 'КРЦ-001', 'Длина упаковки', '10' UNION ALL
    SELECT 'КРЦ-001', 'Ширина упаковки', '10' UNION ALL
    SELECT 'КРЦ-001', 'Высота упаковки', '11' UNION ALL
    SELECT 'КРЦ-001', 'Срок годности', '2029-02-20' UNION ALL
    SELECT 'КРЦ-001', 'Вид разделки', 'Тушка' UNION ALL
    SELECT 'КРЦ-001', 'Вес упаковки', '0.325' UNION ALL

    -- === Филе куриное охлаждённое 0.8кг (КРЦ-002, REFRIGERATOR) ===
    SELECT 'КРЦ-002', 'Длина упаковки', '25' UNION ALL
    SELECT 'КРЦ-002', 'Ширина упаковки', '18' UNION ALL
    SELECT 'КРЦ-002', 'Высота упаковки', '6' UNION ALL
    SELECT 'КРЦ-002', 'Срок годности', '2026-03-20' UNION ALL
    SELECT 'КРЦ-002', 'Вид разделки', 'Филе' UNION ALL
    SELECT 'КРЦ-002', 'Вес упаковки', '0.8' UNION ALL

    -- === Яблоки сушёные 200г (ЯБЛ-001, REGULAR) ===
    SELECT 'ЯБЛ-001', 'Длина упаковки', '20' UNION ALL
    SELECT 'ЯБЛ-001', 'Ширина упаковки', '15' UNION ALL
    SELECT 'ЯБЛ-001', 'Высота упаковки', '5' UNION ALL
    SELECT 'ЯБЛ-001', 'Срок годности', '2027-03-10' UNION ALL
    SELECT 'ЯБЛ-001', 'Сорт', 'Антоновка' UNION ALL
    SELECT 'ЯБЛ-001', 'Калибр плода', '0' UNION ALL

    -- === Яблоки свежие «Гала» 1кг (ЯБЛ-002, REFRIGERATOR) ===
    SELECT 'ЯБЛ-002', 'Длина упаковки', '30' UNION ALL
    SELECT 'ЯБЛ-002', 'Ширина упаковки', '22' UNION ALL
    SELECT 'ЯБЛ-002', 'Высота упаковки', '12' UNION ALL
    SELECT 'ЯБЛ-002', 'Срок годности', '2026-04-14' UNION ALL
    SELECT 'ЯБЛ-002', 'Сорт', 'Гала' UNION ALL
    SELECT 'ЯБЛ-002', 'Калибр плода', '75' UNION ALL

    -- === Огурцы маринованные 720мл (ОГУ-001, REGULAR) ===
    SELECT 'ОГУ-001', 'Длина упаковки', '10' UNION ALL
    SELECT 'ОГУ-001', 'Ширина упаковки', '10' UNION ALL
    SELECT 'ОГУ-001', 'Высота упаковки', '17' UNION ALL
    SELECT 'ОГУ-001', 'Срок годности', '2028-03-15' UNION ALL
    SELECT 'ОГУ-001', 'Тип плода', 'Корнишоны' UNION ALL
    SELECT 'ОГУ-001', 'Средняя длина плода', '8' UNION ALL

    -- === Огурцы свежие короткоплодные 1кг (ОГУ-002, REFRIGERATOR) ===
    SELECT 'ОГУ-002', 'Длина упаковки', '30' UNION ALL
    SELECT 'ОГУ-002', 'Ширина упаковки', '22' UNION ALL
    SELECT 'ОГУ-002', 'Высота упаковки', '14' UNION ALL
    SELECT 'ОГУ-002', 'Срок годности', '2026-03-22' UNION ALL
    SELECT 'ОГУ-002', 'Тип плода', 'Короткоплодные' UNION ALL
    SELECT 'ОГУ-002', 'Средняя длина плода', '12'
) x
JOIN t_product p ON p.article = x.article
JOIN t_product_attribute a ON a.name = x.attr;
