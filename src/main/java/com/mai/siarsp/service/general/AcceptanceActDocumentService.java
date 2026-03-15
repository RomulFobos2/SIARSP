package com.mai.siarsp.service.general;

import com.mai.siarsp.models.*;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.xwpf.usermodel.*;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Сервис генерации приёмо-сдаточного акта в формате docx
 *
 * Программно формирует Word-документ на основе данных акта приёма-передачи
 * (AcceptanceAct), заказа клиента (ClientOrder) и связанных сущностей.
 *
 * Структура документа:
 * 1. Заголовок с номером и датой
 * 2. Сведения о передающей стороне (ИП "Левчук")
 * 3. Сведения о принимающей стороне (клиент)
 * 4. Основание (номер заказа)
 * 5. Таблица переданных товаров
 * 6. Сведения о подписании
 * 7. Подписи сторон
 */
@Service
@Slf4j
public class AcceptanceActDocumentService {

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd.MM.yyyy");
    private static final DateTimeFormatter DATETIME_FMT = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm");
    private static final String FONT_NAME = "Times New Roman";
    private static final int FONT_SIZE_TITLE = 14;
    private static final int FONT_SIZE_NORMAL = 11;
    private static final int FONT_SIZE_SMALL = 10;

    /**
     * Генерирует документ акта приёма-передачи в формате docx
     *
     * @param act акт приёма-передачи с заполненными связями
     * @return ReportFile с именем файла и содержимым документа
     * @throws IOException при ошибке записи документа
     */
    public static ReportDocumentService.ReportFile generateDocument(AcceptanceAct act) throws IOException {
        ClientOrder order = act.getClientOrder();
        Client client = act.getClient();
        Employee deliveredBy = act.getDeliveredBy();
        List<OrderedProduct> products = order.getOrderedProducts();

        try (XWPFDocument document = new XWPFDocument()) {

            // ========== ЗАГОЛОВОК ==========
            addTitle(document, "АКТ ПРИЁМА-ПЕРЕДАЧИ ТОВАРА");
            addCenteredText(document, "№ " + act.getActNumber() + " от " + act.getActDate().format(DATE_FMT));
            addEmptyLine(document);

            // ========== ПРЕАМБУЛА ==========
            addParagraphText(document,
                    "Настоящий акт составлен о том, что представитель Передающей стороны передал, "
                            + "а представитель Принимающей стороны принял следующий товар:");
            addEmptyLine(document);

            // ========== СТОРОНЫ ==========
            addSectionHeader(document, "1. Передающая сторона");
            addParagraphText(document, "ИП «Левчук» — склад продуктов питания и товаров первой необходимости");
            addParagraphText(document, "Представитель: " + deliveredBy.getFullName() + " (водитель-экспедитор)");
            addEmptyLine(document);

            addSectionHeader(document, "2. Принимающая сторона");
            addParagraphText(document, "Организация: " + client.getOrganizationName()
                    + " (" + client.getOrganizationType() + ")");
            addParagraphText(document, "Адрес: " + safeStr(client.getDeliveryAddress()));
            if (client.getInn() != null && !client.getInn().isBlank()) {
                addParagraphText(document, "ИНН: " + client.getInn());
            }
            if (act.getClientRepresentative() != null && !act.getClientRepresentative().isBlank()) {
                addParagraphText(document, "Представитель: " + act.getClientRepresentative());
            } else if (client.getContactPerson() != null && !client.getContactPerson().isBlank()) {
                addParagraphText(document, "Контактное лицо: " + client.getContactPerson());
            }
            addEmptyLine(document);

            // ========== ОСНОВАНИЕ ==========
            addSectionHeader(document, "3. Основание");
            addParagraphText(document, "Заказ № " + order.getOrderNumber()
                    + " от " + order.getOrderDate().format(DATETIME_FMT));
            if (order.getDeliveryDate() != null) {
                addParagraphText(document, "Планируемая дата доставки: " + order.getDeliveryDate().format(DATE_FMT));
            }
            if (order.getActualDeliveryDate() != null) {
                addParagraphText(document, "Фактическая дата доставки: " + order.getActualDeliveryDate().format(DATE_FMT));
            }
            addEmptyLine(document);

            // ========== ТАБЛИЦА ТОВАРОВ ==========
            addSectionHeader(document, "4. Перечень переданных товаров");

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

            // ========== СТАТУС ПОДПИСАНИЯ ==========
            addSectionHeader(document, "5. Сведения о приёмке");
            if (act.isSigned()) {
                addParagraphText(document, "Товар принят в полном объёме. Претензий по количеству и качеству нет.");
                if (act.getSignedAt() != null) {
                    addParagraphText(document, "Дата и время подписания: " + act.getSignedAt().format(DATETIME_FMT));
                }
            } else {
                addParagraphText(document, "Акт ожидает подписания принимающей стороной.");
            }
            if (act.getComment() != null && !act.getComment().isBlank()) {
                addParagraphText(document, "Примечание: " + act.getComment());
            }
            addEmptyLine(document);

            // ========== ПОДПИСИ ==========
            addSectionHeader(document, "6. Подписи сторон");
            addEmptyLine(document);
            addParagraphText(document, "Передал: __________________ / " + deliveredBy.getFullName());
            addEmptyLine(document);
            String receiverName = (act.getClientRepresentative() != null && !act.getClientRepresentative().isBlank())
                    ? act.getClientRepresentative()
                    : "__________________";
            addParagraphText(document, "Принял:  __________________ / " + receiverName);

            // Сформировать результат
            byte[] content = toBytes(document);
            String fileName = "Акт_приёмки_" + act.getActNumber().replace("/", "-") + ".docx";
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
        XWPFParagraph paragraph = cell.getParagraphArray(0);
        if (paragraph == null) {
            paragraph = cell.addParagraph();
        }
        paragraph.setAlignment(ParagraphAlignment.CENTER);
        paragraph.setSpacingAfter(0);
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
}
