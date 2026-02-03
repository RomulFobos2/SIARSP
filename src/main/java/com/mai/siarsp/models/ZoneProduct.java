package com.mai.siarsp.models;

import com.mai.siarsp.enumeration.BoxOrientation;
import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.util.Arrays;
import java.util.Comparator;
import java.util.Map;

/**
 * Товар на полке (зоне хранения)
 *
 * Представляет конкретное размещение товара на полке склада.
 * Связывает товар с полкой и указывает количество и ориентацию размещения.
 * Это финальный уровень в иерархии складского учета.
 *
 * Иерархия складского учета:
 * Warehouse (Основной склад)
 *   → Shelf (Стеллаж A)
 *     → StorageZone (Полка A1: 200×80×40 см)
 *       → ZoneProduct (Молоко 3.2% - 50 шт., стоя)
 *       → ZoneProduct (Хлеб - 30 шт., лежа на боку)
 *
 * Назначение:
 * - Фиксация точного местоположения товара на складе
 * - Учет физического размещения с учетом габаритов упаковки
 * - Оптимизация использования пространства полки
 * - Поддержка различных ориентаций размещения товара
 *
 * Ориентации размещения (см. BoxOrientation enum):
 * - STANDARD - стандартная (обычное положение, основание вниз)
 * - ROTATED_90 - повернуто на 90° (длина и ширина меняются местами)
 * - LAY_ON_SIDE - лежит на боку (ширина и высота меняются местами)
 * - ROTATE_AND_LAY - повернуто и лежит (длина и высота меняются местами)
 *
 * Примеры размещения:
 *
 * Молоко 3.2% 1л (упаковка: 10×10×23 см):
 * - STANDARD: стоит (10×10×23) - занимает мало места по площади
 * - LAY_ON_SIDE: лежит (10×23×10) - занимает больше места, но ниже
 *
 * Хлеб белый (упаковка: 30×12×8 см):
 * - STANDARD: стоит (30×12×8) - неустойчиво
 * - LAY_ON_SIDE: лежит (30×8×12) - более устойчиво
 *
 * Согласно ТЗ п.118-120:
 * - Система подсчитывает оставшееся место на складе
 * - Учитывает габариты товара при размещении
 * - Автоматически подбирает оптимальную ориентацию
 *
 * Бизнес-процессы:
 *
 * 1. Размещение товара:
 *    - Товар поступает на склад (Delivery)
 *    - Product.quantityForStock увеличивается (оприходовано, но не размещено)
 *    - Заведующий выбирает полку (StorageZone)
 *    - Система подбирает оптимальную ориентацию (findBestOrientation)
 *    - Создается ZoneProduct
 *    - Product.quantityForStock уменьшается (размещено на полке)
 *
 * 2. Комплектация заказа:
 *    - Клиент оформляет заказ (ClientOrder)
 *    - Товар резервируется (Product.reservedQuantity увеличивается)
 *    - При отгрузке товар берется с полки
 *    - ZoneProduct.quantity уменьшается
 *    - Product.stockQuantity и reservedQuantity уменьшаются
 *
 * Связи:
 * - Размещен на одной полке (StorageZone)
 * - Относится к одному товару (Product)
 */
@Data
@NoArgsConstructor
@Entity
@Table(name = "t_zoneProduct")
@EqualsAndHashCode(of = "id")
public class ZoneProduct {

    // ========== ПОЛЯ ==========
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Количество единиц товара на данной полке
     * Сколько штук товара физически размещено на этой полке
     *
     * Примеры:
     * - 50 шт. (молоко на полке A1)
     * - 30 шт. (хлеб на полке B2)
     * - 100 шт. (сметана на полке A2)
     *
     * Используется для:
     * - Учета точного местоположения товара
     * - Расчета занятого объема (getTotalVolume)
     * - Комплектации заказов (откуда брать товар)
     * - Инвентаризации (сверка фактического наличия)
     * - Поиска товара на складе
     *
     * Бизнес-правила:
     * - quantity должно быть <= Product.stockQuantity
     * - Сумма quantity по всем ZoneProduct для товара ≤ Product.stockQuantity
     * - При уменьшении quantity до 0 запись ZoneProduct удаляется
     */
    @Column(nullable = false)
    private int quantity;

    /**
     * Ориентация размещения товара на полке
     * Определяет, как повернута упаковка товара
     *
     * Возможные ориентации (см. BoxOrientation enum):
     *
     * - STANDARD (стандартная) - обычное положение
     *   Габариты: длина × ширина × высота (L × W × H)
     *   Пример: молоко стоит (10×10×23 см)
     *
     * - ROTATED_90 (повернуто на 90°) - поворот вокруг вертикальной оси
     *   Габариты: ширина × длина × высота (W × L × H)
     *   Пример: коробка повернута (20×30×15 вместо 30×20×15)
     *
     * - LAY_ON_SIDE (лежит на боку) - положено на бок
     *   Габариты: длина × высота × ширина (L × H × W)
     *   Пример: хлеб лежит (30×8×12 вместо 30×12×8)
     *
     * - ROTATE_AND_LAY (повернуто и лежит) - комбинация поворота и укладки
     *   Габариты: ширина × высота × длина (W × H × L)
     *   Пример: коробка повернута и лежит
     *
     * По умолчанию: STANDARD (стандартная ориентация)
     *
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private BoxOrientation orientation = BoxOrientation.STANDARD;

    /**
     * Полка (зона хранения), на которой размещен товар
     * Конкретное место на складе
     */
    @ToString.Exclude
    @ManyToOne
    @JoinColumn(nullable = false)
    private StorageZone zone;

    /**
     * Товар, размещенный на полке
     * Ссылка на справочник товаров
     */
    @ManyToOne
    @JoinColumn(nullable = false)
    private Product product;

    // ========== КОНСТРУКТОРЫ ==========

    /**
     * Создает новое размещение товара на полке
     * Ориентация по умолчанию: STANDARD (стандартная)
     *
     * @param product товар, который размещается
     * @param quantity количество единиц товара
     */
    public ZoneProduct(Product product, int quantity) {
        this.product = product;
        this.quantity = quantity;
    }

    // ========== МЕТОДЫ ==========

    /**
     * Вычисляет объем одной единицы товара в кубических метрах
     * Учитывает габариты упаковки товара
     *
     * Формула: (length / 100) × (width / 100) × (height / 100)
     * Деление на 100 переводит сантиметры в метры
     *
     * Примеры:
     * - Молоко 10×10×23 см = 0.1×0.1×0.23 = 0.0023 м³ = 2.3 л
     * - Хлеб 30×12×8 см = 0.3×0.12×0.08 = 0.00288 м³ = 2.88 л
     * - Яблоки (коробка) 40×30×20 см = 0.4×0.3×0.2 = 0.024 м³ = 24 л
     *
     * Важно:
     * Если габариты товара не указаны (null), возвращает 0.0
     * Это предотвращает ошибки NullPointerException
     *
     * @return объем одной единицы товара в м³, или 0.0 если габариты не указаны
     */
    public double getVolumePerUnit() {
        Double length = product.getPackageLength();
        Double width = product.getPackageWidth();
        Double height = product.getPackageHeight();

        if (length == null || width == null || height == null) {
            return 0.0;
        }

        return (length / 100.0) * (width / 100.0) * (height / 100.0);
    }

    /**
     * Вычисляет общий объем товара на полке в кубических метрах
     * Умножает объем одной единицы на количество
     *
     * Формула: getVolumePerUnit() × quantity
     *
     * Примеры:
     * - Молоко: 0.0023 м³ × 50 шт. = 0.115 м³ = 115 л
     * - Хлеб: 0.00288 м³ × 30 шт. = 0.0864 м³ = 86.4 л
     * - Яблоки: 0.024 м³ × 10 коробок = 0.24 м³ = 240 л
     *
     * @return общий объем товара в м³
     */
    public double getTotalVolume() {
        return getVolumePerUnit() * quantity;
    }

    /**
     * Проверяет физическую возможность размещения товара на полке
     * Подбирает оптимальную ориентацию и проверяет вместимость
     *
     * Алгоритм:
     * 1. Вызывает findBestOrientation() для поиска оптимальной ориентации
     * 2. Если найдена подходящая ориентация:
     *    - Устанавливает this.orientation
     *    - Возвращает true
     * 3. Если ни одна ориентация не подходит:
     *    - Возвращает false
     *
     * Используется для:
     * - Валидации размещения товара перед сохранением
     * - Автоматического подбора ориентации
     * - Предупреждения о невозможности размещения
     * - Поиска подходящей полки для товара
     *
     * Пример использования:
     * ZoneProduct zp = new ZoneProduct(milk, 50);
     * zp.setZone(shelfA1);
     * if (zp.canFitPhysically()) {
     *     // Можно размещать, ориентация установлена
     *     save(zp);
     * } else {
     *     // Не помещается, нужна другая полка
     *     showError("Товар не помещается на полку A1");
     * }
     *
     * TODO: Перенести в ZoneProductService - сложная бизнес-логика размещения
     * Этот метод содержит сложную бизнес-логику и должен быть в сервисном слое
     *
     * @return true если товар помещается на полке, false если не помещается
     */
    public boolean canFitPhysically() {
        BoxOrientation bestOrientation = findBestOrientation(product, zone, quantity);

        if (bestOrientation != null) {
            this.orientation = bestOrientation;
            return true;
        }

        return false;
    }

    /**
     * Находит оптимальную ориентацию размещения товара на полке
     * Перебирает все возможные ориентации и выбирает ту, которая:
     * 1. Позволяет разместить требуемое количество товара
     * 2. Максимизирует вместимость (можно разместить больше всего)
     *
     * Алгоритм:
     * 1. Получает габариты товара (длина, ширина, высота)
     * 2. Для каждой ориентации (STANDARD, ROTATED_90, LAY_ON_SIDE, ROTATE_AND_LAY):
     *    a. Применяет ориентацию к габаритам (applyOrientation)
     *    b. Вычисляет сколько товара помещается по каждому измерению:
     *       - По длине: zone.length / dims[0]
     *       - По ширине: zone.width / dims[1]
     *       - По высоте: zone.height / dims[2]
     *    c. Вычисляет максимальное количество: fitL × fitW × fitH
     * 3. Фильтрует ориентации, где maxQty >= quantity (помещается нужное кол-во)
     * 4. Выбирает ориентацию с максимальной вместимостью
     *
     * Примеры:
     *
     * Молоко 10×10×23 см на полке 200×80×40 см, нужно 50 шт.:
     *
     * STANDARD (10×10×23):
     * - По длине: 200/10 = 20
     * - По ширине: 80/10 = 8
     * - По высоте: 40/23 = 1
     * - Всего: 20×8×1 = 160 шт. ✓ (>= 50)
     *
     * LAY_ON_SIDE (10×23×10):
     * - По длине: 200/10 = 20
     * - По ширине: 80/23 = 3
     * - По высоте: 40/10 = 4
     * - Всего: 20×3×4 = 240 шт. ✓ (>= 50, ЛУЧШЕ!)
     *
     * Выбрана: LAY_ON_SIDE (максимальная вместимость 240 шт.)
     *
     * Используется для:
     * - Автоматического подбора оптимальной ориентации
     * - Максимизации использования пространства полки
     * - Проверки возможности размещения товара
     * - Оптимизации загрузки склада
     *
     * TODO: Перенести в ZoneProductService - алгоритм оптимального размещения
     * Эта сложная бизнес-логика должна быть в сервисном слое
     *
     * @param product товар, который нужно разместить
     * @param zone полка, на которую размещается товар
     * @param quantity требуемое количество товара
     * @return оптимальная ориентация или null если товар не помещается
     */
    public BoxOrientation findBestOrientation(Product product, StorageZone zone, int quantity) {
        Double boxL = product.getPackageLength();
        Double boxW = product.getPackageWidth();
        Double boxH = product.getPackageHeight();

        // Проверка на null габаритов
        if (boxL == null || boxW == null || boxH == null) {
            return null;
        }

        return Arrays.stream(BoxOrientation.values())
                .map(orientation -> {
                    double[] dims = applyOrientation(boxL, boxW, boxH, orientation);
                    int fitL = (int) (zone.getLength() / dims[0]);
                    int fitW = (int) (zone.getWidth() / dims[1]);
                    int fitH = (int) (zone.getHeight() / dims[2]);
                    int maxQty = fitL * fitW * fitH;
                    return Map.entry(orientation, maxQty);
                })
                .filter(e -> e.getValue() >= quantity)
                .max(Comparator.comparingInt(Map.Entry::getValue))
                .map(Map.Entry::getKey)
                .orElse(null);
    }

    /**
     * Применяет ориентацию к габаритам упаковки товара
     * Меняет местами измерения в зависимости от ориентации
     *
     * Входные параметры:
     * - l (length) - длина упаковки
     * - w (width) - ширина упаковки
     * - h (height) - высота упаковки
     *
     * Возвращает массив [эффективная_длина, эффективная_ширина, эффективная_высота]
     *
     * Логика преобразований:
     *
     * - STANDARD (стандартная):
     *   [l, w, h] - без изменений
     *   Пример: [30, 12, 8] → [30, 12, 8]
     *
     * - ROTATED_90 (повернуто на 90°):
     *   [w, l, h] - длина и ширина меняются местами
     *   Пример: [30, 12, 8] → [12, 30, 8]
     *
     * - LAY_ON_SIDE (лежит на боку):
     *   [l, h, w] - ширина и высота меняются местами
     *   Пример: [30, 12, 8] → [30, 8, 12]
     *
     * - ROTATE_AND_LAY (повернуто и лежит):
     *   [w, h, l] - полная перестановка
     *   Пример: [30, 12, 8] → [12, 8, 30]
     *
     * Используется в findBestOrientation() для расчета вместимости
     *
     * @param l длина упаковки в см
     * @param w ширина упаковки в см
     * @param h высота упаковки в см
     * @param orientation ориентация размещения
     * @return массив эффективных габаритов [длина, ширина, высота] в см
     */
    private double[] applyOrientation(double l, double w, double h, BoxOrientation orientation) {
        return switch (orientation) {
            case STANDARD -> new double[]{l, w, h};
            case ROTATED_90 -> new double[]{w, l, h};
            case LAY_ON_SIDE -> new double[]{l, h, w};
            case ROTATE_AND_LAY -> new double[]{w, h, l};
        };
    }
}