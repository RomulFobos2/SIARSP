package com.mai.siarsp.service.general;

import com.mai.siarsp.models.RequestForDelivery;
import com.mai.siarsp.models.RequestedProduct;
import com.mai.siarsp.models.Supplier;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.xwpf.usermodel.*;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTRow;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Сервис генерации договора поставки по шаблону DocRequestForDelivery.docx
 *
 * Открывает шаблон Word-документа, заменяет плейсхолдеры данными из заявки
 * на поставку (RequestForDelivery) и возвращает готовый документ в виде байтового массива.
 *
 * Плейсхолдеры в шаблоне обозначены английскими словами.
 * Строки таблицы товаров размножаются по количеству позиций заявки.
 */
@Service
@Slf4j
public class RequestForDeliveryDocumentService {

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd.MM.yyyy");

    @Value("${word.template.path:src/main/resources/wordTemplates}")
    private String templatePathValue;

    private static String templatePath;

    @PostConstruct
    public void init() {
        templatePath = templatePathValue;
    }

    /**
     * Генерирует договор поставки на основе шаблона и данных заявки
     *
     * @param request заявка на поставку (статус APPROVED)
     * @return ReportFile с именем файла и содержимым документа
     * @throws IOException при ошибке чтения шаблона или записи документа
     */
    public static ReportDocumentService.ReportFile generateContract(RequestForDelivery request) throws IOException {
        String templateFile = templatePath + "/DocRequestForDelivery.docx";

        try (FileInputStream fis = new FileInputStream(templateFile);
             XWPFDocument document = new XWPFDocument(fis)) {

            Supplier supplier = request.getSupplier();
            List<RequestedProduct> products = request.getRequestedProducts();

            // 1. Заменить плейсхолдеры в параграфах документа
            for (XWPFParagraph paragraph : document.getParagraphs()) {
                replacePlaceholdersInParagraph(paragraph, supplier, request);
            }

            // 2. Обработать таблицы
            for (XWPFTable table : document.getTables()) {
                processTable(table, supplier, request, products);
            }

            // 3. Записать результат
            byte[] content = toBytes(document);
            String fileName = "Договор_поставки_заявка_" + request.getId() + ".docx";
            return new ReportDocumentService.ReportFile(fileName, content);
        }
    }

    /**
     * Обрабатывает таблицу: заменяет плейсхолдеры и размножает строки товаров
     */
    private static void processTable(XWPFTable table, Supplier supplier,
                                      RequestForDelivery request, List<RequestedProduct> products) {
        // Найти строку-шаблон с плейсхолдером "Number"
        int templateRowIndex = -1;
        for (int i = 0; i < table.getNumberOfRows(); i++) {
            String rowText = getRowText(table.getRow(i));
            if (rowText.contains("Number")) {
                templateRowIndex = i;
                break;
            }
        }

        if (templateRowIndex >= 0) {
            // Строка товаров найдена — размножить
            XWPFTableRow templateRow = table.getRow(templateRowIndex);
            CTRow ctTemplateRow = templateRow.getCtRow();

            // Создать строки для каждого товара (вставляем после шаблонной строки)
            for (int i = products.size() - 1; i >= 0; i--) {
                RequestedProduct rp = products.get(i);
                CTRow newCtRow = (CTRow) ctTemplateRow.copy();
                XWPFTableRow newRow = new XWPFTableRow(newCtRow, table);

                // Заменить плейсхолдеры в ячейках новой строки
                for (XWPFTableCell cell : newRow.getTableCells()) {
                    for (XWPFParagraph paragraph : cell.getParagraphs()) {
                        replaceProductPlaceholders(paragraph, rp, i + 1);
                    }
                }

                // Вставить строку после шаблонной
                table.addRow(newRow, templateRowIndex + 1);
            }

            // Удалить оригинальную строку-шаблон
            table.removeRow(templateRowIndex);
        }

        // Заменить плейсхолдеры в остальных ячейках (итоги, реквизиты)
        for (XWPFTableRow row : table.getRows()) {
            for (XWPFTableCell cell : row.getTableCells()) {
                for (XWPFParagraph paragraph : cell.getParagraphs()) {
                    replacePlaceholdersInParagraph(paragraph, supplier, request);
                }
            }
        }
    }

    /**
     * Заменяет плейсхолдеры товарной строки данными из RequestedProduct
     */
    private static void replaceProductPlaceholders(XWPFParagraph paragraph, RequestedProduct rp, int number) {
        String fullText = getFullText(paragraph);
        if (fullText.isEmpty()) return;

        fullText = fullText.replace("Number", String.valueOf(number));
        fullText = fullText.replace("ProductName", rp.getProduct().getName());
        fullText = fullText.replace("SupplyUnit", rp.getUnit() != null ? rp.getUnit() : "");
        fullText = fullText.replace("SupplyQuantity", String.valueOf(rp.getQuantity()));
        fullText = fullText.replace("SupplyPurchasePrice", formatDecimal(rp.getPurchasePrice()));
        fullText = fullText.replace("SupplyGetTotalPrice", formatDecimal(rp.getTotalPrice()));

        setFullText(paragraph, fullText);
    }

    /**
     * Заменяет общие плейсхолдеры (поставщик, даты, реквизиты, итоги)
     */
    private static void replacePlaceholdersInParagraph(XWPFParagraph paragraph, Supplier supplier,
                                                        RequestForDelivery request) {
        String fullText = getFullText(paragraph);
        if (fullText.isEmpty()) return;

        boolean changed = false;

        // Дата
        if (fullText.contains("dd.MM.yyy")) {
            fullText = fullText.replace("dd.MM.yyy", LocalDate.now().format(DATE_FMT));
            changed = true;
        }

        // Поставщик — основные данные
        if (fullText.contains("getFullName")) {
            fullText = fullText.replace("getFullName", safeStr(supplier.getFullName()));
            changed = true;
        }
        if (fullText.contains("SupplierGetDirectorShortName")) {
            fullText = fullText.replace("SupplierGetDirectorShortName", safeStr(supplier.getDirectorShortName()));
            changed = true;
        }
        if (fullText.contains("name")) {
            // Заменяем "name" только если это отдельное слово (не часть другого плейсхолдера)
            // getFullName и ProductName уже обработаны выше
            fullText = fullText.replace("name", safeStr(supplier.getName()));
            changed = true;
        }

        // Реквизиты поставщика
        if (fullText.contains("address")) {
            fullText = fullText.replace("address", safeStr(supplier.getAddress()));
            changed = true;
        }
        if (fullText.contains("inn")) {
            fullText = fullText.replace("inn", safeStr(supplier.getInn()));
            changed = true;
        }
        if (fullText.contains("kpp")) {
            fullText = fullText.replace("kpp", safeStr(supplier.getKpp()));
            changed = true;
        }
        if (fullText.contains("ogrn")) {
            fullText = fullText.replace("ogrn", safeStr(supplier.getOgrn()));
            changed = true;
        }
        if (fullText.contains("paymentAccount")) {
            fullText = fullText.replace("paymentAccount", safeStr(supplier.getPaymentAccount()));
            changed = true;
        }
        if (fullText.contains("bank")) {
            fullText = fullText.replace("bank", safeStr(supplier.getBank()));
            changed = true;
        }
        if (fullText.contains("bik")) {
            fullText = fullText.replace("bik", safeStr(supplier.getBik()));
            changed = true;
        }
        if (fullText.contains("contactInfo")) {
            fullText = fullText.replace("contactInfo", safeStr(supplier.getContactInfo()));
            changed = true;
        }

        // Итоги заявки
        if (fullText.contains("RequestForDeliveryGetTotalCost")) {
            fullText = fullText.replace("RequestForDeliveryGetTotalCost", formatDecimal(request.getTotalCost()));
            changed = true;
        }
        if (fullText.contains("RequestForDeliveryDeliveryCost")) {
            fullText = fullText.replace("RequestForDeliveryDeliveryCost", formatDecimal(request.getDeliveryCost()));
            changed = true;
        }

        if (changed) {
            setFullText(paragraph, fullText);
        }
    }

    /**
     * Собирает полный текст параграфа из всех Run-ов
     */
    private static String getFullText(XWPFParagraph paragraph) {
        StringBuilder sb = new StringBuilder();
        for (XWPFRun run : paragraph.getRuns()) {
            String text = run.getText(0);
            if (text != null) {
                sb.append(text);
            }
        }
        return sb.toString();
    }

    /**
     * Записывает текст в параграф: весь текст идёт в первый Run, остальные очищаются.
     * Сохраняет форматирование первого Run.
     */
    private static void setFullText(XWPFParagraph paragraph, String newText) {
        List<XWPFRun> runs = paragraph.getRuns();
        if (runs.isEmpty()) return;

        // Записать весь текст в первый Run
        runs.get(0).setText(newText, 0);

        // Очистить остальные Run-ы
        for (int i = 1; i < runs.size(); i++) {
            runs.get(i).setText("", 0);
        }
    }

    /**
     * Получает полный текст строки таблицы (для поиска плейсхолдеров)
     */
    private static String getRowText(XWPFTableRow row) {
        StringBuilder sb = new StringBuilder();
        for (XWPFTableCell cell : row.getTableCells()) {
            for (XWPFParagraph p : cell.getParagraphs()) {
                sb.append(getFullText(p));
            }
        }
        return sb.toString();
    }

    private static byte[] toBytes(XWPFDocument document) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        document.write(baos);
        return baos.toByteArray();
    }

    private static String formatDecimal(BigDecimal value) {
        if (value == null) return "0.00";
        return value.setScale(2, java.math.RoundingMode.HALF_UP).toPlainString();
    }

    private static String safeStr(String value) {
        return value != null ? value : "";
    }
}
