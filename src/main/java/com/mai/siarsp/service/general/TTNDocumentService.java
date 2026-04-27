package com.mai.siarsp.service.general;

import com.mai.siarsp.models.*;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.xwpf.usermodel.*;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.*;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Сервис генерации товарно-транспортной накладной (ТТН) в формате docx
 *
 * Программно формирует Word-документ на основе данных ТТН (TTN),
 * заказа клиента (ClientOrder) и связанных сущностей.
 *
 * Структура документа:
 * 1. Заголовок с номером и датой
 * 2. Сведения об отправителе и получателе
 * 3. Сведения о транспорте и водителе
 * 4. Таблица товаров (из заказа клиента)
 * 5. Характеристики груза (вес, объём, описание)
 * 6. Подписи сторон
 */
@Service
@Slf4j
public class TTNDocumentService {

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd.MM.yyyy");
    private static final String FONT_NAME = "Times New Roman";
    private static final int FONT_SIZE_TITLE = 14;
    private static final int FONT_SIZE_NORMAL = 11;
    private static final int FONT_SIZE_SMALL = 10;

    /**
     * Генерирует документ ТТН в формате docx
     *
     * @param ttn товарно-транспортная накладная с заполненными связями
     * @return ReportFile с именем файла и содержимым документа
     * @throws IOException при ошибке записи документа
     */
    public static ReportDocumentService.ReportFile generateDocument(TTN ttn) throws IOException {
        ClientOrder order = ttn.getDeliveryTask().getClientOrder();
        Client client = order.getClient();
        Vehicle vehicle = ttn.getVehicle();
        Employee driver = ttn.getDriver();
        List<OrderedProduct> products = order.getOrderedProducts();

        try (XWPFDocument document = new XWPFDocument()) {

            // ========== ЗАГОЛОВОК ==========
            addTitle(document, "ТОВАРНО-ТРАНСПОРТНАЯ НАКЛАДНАЯ");
            addCenteredText(document, "№ " + ttn.getTtnNumber() + " от " + ttn.getIssueDate().format(DATE_FMT));
            addEmptyLine(document);

            // ========== СТОРОНЫ ==========
            addSectionHeader(document, "1. Грузоотправитель");
            addParagraphText(document, "ИП «Левчук» — склад продуктов питания и товаров первой необходимости");
            addEmptyLine(document);

            addSectionHeader(document, "2. Грузополучатель");
            addParagraphText(document, "Организация: " + client.getOrganizationName()
                    + " (" + client.getOrganizationType() + ")");
            addParagraphText(document, "Адрес доставки: " + safeStr(client.getDeliveryAddress()));
            if (client.getInn() != null && !client.getInn().isBlank()) {
                addParagraphText(document, "ИНН: " + client.getInn());
            }
            if (client.getContactPerson() != null && !client.getContactPerson().isBlank()) {
                addParagraphText(document, "Контактное лицо: " + client.getContactPerson());
            }
            if (client.getPhoneNumber() != null && !client.getPhoneNumber().isBlank()) {
                addParagraphText(document, "Телефон: " + client.getPhoneNumber());
            }
            addEmptyLine(document);

            // ========== ТРАНСПОРТ ==========
            addSectionHeader(document, "3. Сведения о транспорте");
            addParagraphText(document, "Автомобиль: " + vehicle.getBrand() + " " + vehicle.getModel()
                    + ", гос. номер " + vehicle.getRegistrationNumber());
            addParagraphText(document, "Водитель-экспедитор: " + driver.getFullName());
            addEmptyLine(document);

            // ========== ЗАКАЗ ==========
            addSectionHeader(document, "4. Основание");
            addParagraphText(document, "Заказ № " + order.getOrderNumber()
                    + " от " + order.getOrderDate().format(DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm")));
            addEmptyLine(document);

            // ========== ТАБЛИЦА ТОВАРОВ ==========
            addSectionHeader(document, "5. Перечень товаров");

            XWPFTable table = document.createTable(products.size() + 2, 5);
            table.setWidth("100%");

            // Заголовок таблицы
            setTableCell(table, 0, 0, "№", true);
            setTableCell(table, 0, 1, "Наименование товара", true);
            setTableCell(table, 0, 2, "Кол-во", true);
            setTableCell(table, 0, 3, "Цена, руб.", true);
            setTableCell(table, 0, 4, "Сумма, руб.", true);

            // Строки товаров
            BigDecimal totalAmount = BigDecimal.ZERO;
            for (int i = 0; i < products.size(); i++) {
                OrderedProduct op = products.get(i);
                setTableCell(table, i + 1, 0, String.valueOf(i + 1), false);
                setTableCell(table, i + 1, 1, op.getProduct().getName(), false);
                setTableCell(table, i + 1, 2, String.valueOf(op.getQuantity()), false);
                setTableCell(table, i + 1, 3, formatDecimal(op.getPrice()), false);
                setTableCell(table, i + 1, 4, formatDecimal(op.getTotalPrice()), false);
                totalAmount = totalAmount.add(op.getTotalPrice());
            }

            // Итоговая строка
            int lastRow = products.size() + 1;
            setTableCell(table, lastRow, 0, "", true);
            setTableCell(table, lastRow, 1, "", true);
            setTableCell(table, lastRow, 2, "", true);
            setTableCell(table, lastRow, 3, "ИТОГО:", true);
            setTableCell(table, lastRow, 4, formatDecimal(totalAmount), true);

            addEmptyLine(document);

            // ========== ХАРАКТЕРИСТИКИ ГРУЗА ==========
            addSectionHeader(document, "6. Характеристики груза");
            addParagraphText(document, "Описание: " + safeStr(ttn.getCargoDescription(), "не указано"));
            addParagraphText(document, "Общий вес: "
                    + (ttn.getTotalWeight() != null ? ttn.getTotalWeight() + " кг" : "не указан"));
            addParagraphText(document, "Общий объём: "
                    + (ttn.getTotalVolume() != null ? ttn.getTotalVolume() + " м³" : "не указан"));
            if (ttn.getComment() != null && !ttn.getComment().isBlank()) {
                addParagraphText(document, "Примечание: " + ttn.getComment());
            }
            addEmptyLine(document);

            // ========== ПОДПИСИ ==========
            addSectionHeader(document, "7. Подписи");
            addEmptyLine(document);
            addParagraphText(document, "Грузоотправитель: __________________ / __________________");
            addEmptyLine(document);
            addParagraphText(document, "Водитель-экспедитор: __________________ / " + driver.getFullName());
            addEmptyLine(document);
            addParagraphText(document, "Грузополучатель: __________________ / __________________");

            // Сформировать результат
            byte[] content = toBytes(document);
            String fileName = "ТТН_" + ttn.getTtnNumber().replace("/", "-") + ".docx";
            return new ReportDocumentService.ReportFile(fileName, content);
        }
    }

    // ========== ВСПОМОГАТЕЛЬНЫЕ МЕТОДЫ ==========

    private static void addTitle(XWPFDocument document, String text) {
        XWPFParagraph paragraph = document.createParagraph();
        paragraph.setAlignment(ParagraphAlignment.CENTER);
        paragraph.setSpacingAfter(100);
        XWPFRun run = paragraph.createRun();
        run.setText(text);
        run.setBold(true);
        run.setFontSize(FONT_SIZE_TITLE);
        run.setFontFamily(FONT_NAME);
    }

    private static void addCenteredText(XWPFDocument document, String text) {
        XWPFParagraph paragraph = document.createParagraph();
        paragraph.setAlignment(ParagraphAlignment.CENTER);
        paragraph.setSpacingAfter(50);
        XWPFRun run = paragraph.createRun();
        run.setText(text);
        run.setFontSize(FONT_SIZE_NORMAL);
        run.setFontFamily(FONT_NAME);
    }

    private static void addSectionHeader(XWPFDocument document, String text) {
        XWPFParagraph paragraph = document.createParagraph();
        paragraph.setSpacingBefore(100);
        paragraph.setSpacingAfter(50);
        XWPFRun run = paragraph.createRun();
        run.setText(text);
        run.setBold(true);
        run.setFontSize(FONT_SIZE_NORMAL);
        run.setFontFamily(FONT_NAME);
    }

    private static void addParagraphText(XWPFDocument document, String text) {
        XWPFParagraph paragraph = document.createParagraph();
        paragraph.setSpacingAfter(30);
        XWPFRun run = paragraph.createRun();
        run.setText(text);
        run.setFontSize(FONT_SIZE_NORMAL);
        run.setFontFamily(FONT_NAME);
    }

    private static void addEmptyLine(XWPFDocument document) {
        XWPFParagraph paragraph = document.createParagraph();
        paragraph.setSpacingAfter(50);
        XWPFRun run = paragraph.createRun();
        run.setText("");
        run.setFontSize(FONT_SIZE_SMALL);
    }

    private static void setTableCell(XWPFTable table, int row, int col, String text, boolean bold) {
        XWPFTableCell cell = table.getRow(row).getCell(col);
        // Очистить дефолтный параграф
        XWPFParagraph paragraph = cell.getParagraphArray(0);
        if (paragraph == null) {
            paragraph = cell.addParagraph();
        }
        paragraph.setAlignment(ParagraphAlignment.CENTER);
        paragraph.setSpacingAfter(0);
        // Удалить существующие Run-ы
        for (int i = paragraph.getRuns().size() - 1; i >= 0; i--) {
            paragraph.removeRun(i);
        }
        XWPFRun run = paragraph.createRun();
        run.setText(text);
        run.setBold(bold);
        run.setFontSize(FONT_SIZE_SMALL);
        run.setFontFamily(FONT_NAME);
    }

    private static byte[] toBytes(XWPFDocument document) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        document.write(baos);
        return baos.toByteArray();
    }

    private static String formatDecimal(BigDecimal value) {
        if (value == null) return "0.00";
        return value.setScale(2, RoundingMode.HALF_UP).toPlainString();
    }

    private static String safeStr(String value) {
        return value != null ? value : "";
    }

    private static String safeStr(String value, String defaultValue) {
        return (value != null && !value.isBlank()) ? value : defaultValue;
    }
}
