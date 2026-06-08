package com.mai.siarsp.service.general;

import com.mai.siarsp.dto.CommercialOfferImportResult;
import com.mai.siarsp.dto.CommercialOfferImportResult.ImportedItem;
import com.mai.siarsp.models.Product;
import com.mai.siarsp.repo.ProductRepository;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFTable;
import org.apache.poi.xwpf.usermodel.XWPFTableCell;
import org.apache.poi.xwpf.usermodel.XWPFTableRow;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Парсинг docx-файла коммерческого предложения для импорта позиций в форму
 * создания клиентского заказа.
 * <p>
 * Ожидаемая структура: первая таблица в документе содержит 6 колонок —
 * «№ п/п, Наименование товара, Артикул, Количество, Скидка %, Цена за единицу с учётом НДС».
 * Цена в файле — это цена ПОСЛЕ скидки, поэтому при импорте восстанавливается
 * базовая цена: {@code originalPrice = priceAfterDiscount / (1 - discount/100)}.
 */
@Service
@Slf4j
public class CommercialOfferImportService {

    private static final int COL_INDEX_ARTICLE = 2;
    private static final int COL_INDEX_QUANTITY = 3;
    private static final int COL_INDEX_DISCOUNT = 4;
    private static final int COL_INDEX_PRICE = 5;
    private static final int COL_INDEX_NAME = 1;
    private static final int EXPECTED_COLUMNS = 6;

    private final ProductRepository productRepository;

    public CommercialOfferImportService(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    @Transactional(readOnly = true)
    public CommercialOfferImportResult parse(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Файл не выбран или пуст.");
        }
        String originalName = file.getOriginalFilename();
        if (originalName != null && !originalName.toLowerCase().endsWith(".docx")) {
            throw new IllegalArgumentException("Поддерживается только формат .docx.");
        }

        try (InputStream in = file.getInputStream();
             XWPFDocument doc = new XWPFDocument(in)) {

            List<XWPFTable> tables = doc.getTables();
            if (tables.isEmpty()) {
                throw new IllegalArgumentException("В файле не найдено ни одной таблицы.");
            }
            XWPFTable table = tables.get(0);
            List<XWPFTableRow> rows = table.getRows();
            if (rows.size() < 2) {
                throw new IllegalArgumentException("Таблица не содержит данных.");
            }

            validateHeader(rows.get(0));

            // Аккумулируем дубли по артикулу, сохраняем порядок появления
            LinkedHashMap<String, ImportedItem> byArticle = new LinkedHashMap<>();
            int duplicatesMerged = 0;
            int totalDataRows = 0;

            for (int i = 1; i < rows.size(); i++) {
                XWPFTableRow row = rows.get(i);
                String article = cellText(row, COL_INDEX_ARTICLE);
                if (article.isBlank()) {
                    // пустая строка — пропускаем молча
                    continue;
                }
                totalDataRows++;
                String nameInFile = cellText(row, COL_INDEX_NAME);
                int quantity = parseQuantity(cellText(row, COL_INDEX_QUANTITY), i);
                int discount = parseDiscount(cellText(row, COL_INDEX_DISCOUNT), i);
                BigDecimal price = parseDecimal(cellText(row, COL_INDEX_PRICE), i);

                if (quantity <= 0 || price.signum() <= 0 || discount >= 100) {
                    // строка некорректная — пропускаем (молчаливо, как пустую)
                    continue;
                }

                ImportedItem existing = byArticle.get(article);
                if (existing != null) {
                    duplicatesMerged++;
                    int sumQty = existing.quantity() + quantity;
                    byArticle.put(article, new ImportedItem(
                            existing.rowNumber(),
                            existing.article(),
                            existing.productNameInFile(),
                            existing.productNameInSystem(),
                            existing.productId(),
                            sumQty,
                            existing.originalPrice(),
                            existing.priceAfterDiscount(),
                            existing.discountPercent(),
                            existing.status()
                    ));
                    continue;
                }

                Optional<Product> match = productRepository.findByArticle(article);
                BigDecimal originalPrice = recoverOriginalPrice(price, discount);
                if (match.isPresent()) {
                    Product p = match.get();
                    byArticle.put(article, new ImportedItem(
                            i, article, nameInFile,
                            p.getName(), p.getId(),
                            quantity, originalPrice, price, discount,
                            "FOUND"
                    ));
                } else {
                    byArticle.put(article, new ImportedItem(
                            i, article, nameInFile,
                            null, null,
                            quantity, originalPrice, price, discount,
                            "NOT_FOUND"
                    ));
                }
            }

            List<ImportedItem> items = new ArrayList<>(byArticle.values());
            int foundCount = (int) items.stream().filter(it -> "FOUND".equals(it.status())).count();
            int notFoundCount = items.size() - foundCount;
            return new CommercialOfferImportResult(totalDataRows, foundCount, notFoundCount,
                    duplicatesMerged, items);

        } catch (IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            log.error("Не удалось разобрать docx: {}", e.getMessage(), e);
            throw new IllegalArgumentException("Не удалось разобрать файл. Проверьте, что это docx с таблицей коммерческого предложения.");
        }
    }

    // ========== HELPERS ==========

    private void validateHeader(XWPFTableRow header) {
        List<XWPFTableCell> cells = header.getTableCells();
        if (cells.size() < EXPECTED_COLUMNS) {
            throw new IllegalArgumentException(
                    "Ожидается таблица с " + EXPECTED_COLUMNS + " колонками, найдено " + cells.size() + ".");
        }
        // Лёгкая проверка по ключевым словам в заголовках — нечувствительная к точному написанию
        String h2 = cells.get(COL_INDEX_ARTICLE).getText().toLowerCase();
        String h3 = cells.get(COL_INDEX_QUANTITY).getText().toLowerCase();
        String h4 = cells.get(COL_INDEX_DISCOUNT).getText().toLowerCase();
        String h5 = cells.get(COL_INDEX_PRICE).getText().toLowerCase();
        Map<String, String> expectations = new LinkedHashMap<>();
        expectations.put("«Артикул»", h2);
        expectations.put("«Количество»", h3);
        expectations.put("«Скидка»", h4);
        expectations.put("«Цена»", h5);
        for (Map.Entry<String, String> e : expectations.entrySet()) {
            String token = e.getKey().replace("«", "").replace("»", "").toLowerCase();
            if (!e.getValue().contains(token)) {
                throw new IllegalArgumentException(
                        "Заголовок не соответствует ожидаемому. Не найдено слово " + e.getKey() + ".");
            }
        }
    }

    private String cellText(XWPFTableRow row, int col) {
        List<XWPFTableCell> cells = row.getTableCells();
        if (col >= cells.size()) return "";
        return cells.get(col).getText() == null ? "" : cells.get(col).getText().trim();
    }

    private int parseQuantity(String text, int rowIdx) {
        try {
            return Integer.parseInt(text.replaceAll("\\s+", ""));
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Строка " + rowIdx + ": некорректное количество «" + text + "».");
        }
    }

    private int parseDiscount(String text, int rowIdx) {
        String cleaned = text.replace("%", "").replaceAll("\\s+", "");
        try {
            int d = Integer.parseInt(cleaned);
            if (d < 0 || d > 100) {
                throw new IllegalArgumentException("Строка " + rowIdx + ": скидка вне диапазона 0..100 («" + text + "»).");
            }
            return d;
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Строка " + rowIdx + ": некорректная скидка «" + text + "».");
        }
    }

    private BigDecimal parseDecimal(String text, int rowIdx) {
        String cleaned = text.replace(",", ".").replaceAll("[^0-9.\\-]", "");
        try {
            return new BigDecimal(cleaned);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Строка " + rowIdx + ": некорректная цена «" + text + "».");
        }
    }

    /**
     * Восстанавливает цену ДО скидки из цены ПОСЛЕ скидки.
     * priceAfterDiscount = originalPrice × (1 - discount/100)
     * → originalPrice = priceAfterDiscount / (1 - discount/100)
     */
    private BigDecimal recoverOriginalPrice(BigDecimal priceAfterDiscount, int discountPercent) {
        if (discountPercent <= 0) {
            return priceAfterDiscount.setScale(2, RoundingMode.HALF_UP);
        }
        BigDecimal multiplier = BigDecimal.ONE.subtract(
                BigDecimal.valueOf(discountPercent).divide(BigDecimal.valueOf(100)));
        return priceAfterDiscount.divide(multiplier, 2, RoundingMode.HALF_UP);
    }
}
