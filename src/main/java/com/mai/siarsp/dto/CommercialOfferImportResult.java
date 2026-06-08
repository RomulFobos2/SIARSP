package com.mai.siarsp.dto;

import java.math.BigDecimal;
import java.util.List;

/**
 * Результат разбора docx-файла коммерческого предложения.
 * <p>
 * Используется для предпросмотра импорта в модальном окне на странице создания
 * клиентского заказа. Содержит как сводные счётчики, так и детальный список
 * распознанных строк (включая ненайденные в БД — они тоже показываются пользователю,
 * но при подтверждении пропускаются).
 */
public record CommercialOfferImportResult(
        int totalRows,
        int foundCount,
        int notFoundCount,
        int duplicatesMerged,
        List<ImportedItem> items
) {
    /**
     * Одна позиция из таблицы коммерческого предложения.
     *
     * @param rowNumber           номер строки в исходной таблице (1-based, без заголовка)
     * @param article             артикул из файла
     * @param productNameInFile   наименование, как написано в файле
     * @param productNameInSystem наименование товара в БД (null, если не найден)
     * @param productId           id товара в БД (null, если не найден)
     * @param quantity            количество (учитывает суммированные дубли)
     * @param originalPrice       расчётная цена до скидки = priceAfterDiscount / (1 - discount/100)
     * @param priceAfterDiscount  цена из файла (с учётом скидки и НДС)
     * @param discountPercent     скидка из файла, 0..99
     * @param status              "FOUND" или "NOT_FOUND"
     */
    public record ImportedItem(
            int rowNumber,
            String article,
            String productNameInFile,
            String productNameInSystem,
            Long productId,
            int quantity,
            BigDecimal originalPrice,
            BigDecimal priceAfterDiscount,
            int discountPercent,
            String status
    ) {
    }
}
