package com.mai.siarsp.service.general;

import com.mai.siarsp.models.Employee;
import com.mai.siarsp.models.Product;
import com.mai.siarsp.models.Warehouse;
import com.mai.siarsp.models.WriteOffAct;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.xwpf.usermodel.ParagraphAlignment;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFRun;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.format.DateTimeFormatter;

/**
 * Сервис генерации акта списания товара в формате docx.
 *
 * Программно формирует Word-документ на основе данных WriteOffAct без шаблона.
 *
 * Документ включает: номер и дату акта, сведения о товаре, причине, количестве и
 * стоимости списания, ответственного, комментарий составителя, решение руководителя
 * (если есть) и место для подписей.
 */
@Service
@Slf4j
public class WriteOffActDocumentService {

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd.MM.yyyy");
    private static final String FONT_NAME = "Times New Roman";
    private static final int FONT_SIZE_TITLE = 14;
    private static final int FONT_SIZE_NORMAL = 11;
    private static final int FONT_SIZE_SMALL = 10;

    /**
     * Генерирует docx-документ акта списания.
     *
     * @param act акт списания с заполненными связями (product, responsibleEmployee, warehouse)
     * @return ReportFile с именем файла и содержимым документа
     * @throws IOException при ошибке записи
     */
    public static ReportDocumentService.ReportFile generateDocument(WriteOffAct act) throws IOException {
        Product product = act.getProduct();
        Employee responsible = act.getResponsibleEmployee();
        Warehouse warehouse = act.getWarehouse();

        try (XWPFDocument document = new XWPFDocument()) {

            // ===== Заголовок =====
            addTitle(document, "АКТ СПИСАНИЯ ТОВАРА");
            addCenteredText(document, "№ " + safeStr(act.getActNumber(), "—"));
            addCenteredText(document, "от " + (act.getActDate() != null ? act.getActDate().format(DATE_FMT) : "—"));
            addEmptyLine(document);

            // ===== Статус =====
            String statusName = act.getStatus() != null ? act.getStatus().getDisplayName() : "—";
            addParagraphText(document, "Статус акта: " + statusName);
            addEmptyLine(document);

            // ===== Сведения о товаре =====
            addSectionHeader(document, "1. Сведения о товаре");
            addParagraphText(document, "Наименование: " + (product != null ? safeStr(product.getName(), "—") : "—"));
            addParagraphText(document, "Артикул: " + (product != null ? safeStr(product.getArticle(), "—") : "—"));
            addParagraphText(document, "Склад хранения: " + (warehouse != null ? safeStr(warehouse.getName(), "—") : "—"));
            addEmptyLine(document);

            // ===== Списание =====
            addSectionHeader(document, "2. Сведения о списании");
            String reasonName = act.getReason() != null ? act.getReason().getDisplayName() : "—";
            addParagraphText(document, "Причина списания: " + reasonName);
            addParagraphText(document, "Количество к списанию: " + act.getQuantity() + " шт.");
            addParagraphText(document, "Стоимость списанного товара: "
                    + (act.getTotalCost() != null ? formatDecimal(act.getTotalCost()) + " руб." : "не определена"));
            addEmptyLine(document);

            // ===== Ответственный =====
            addSectionHeader(document, "3. Ответственное лицо");
            addParagraphText(document, "ФИО: " + (responsible != null ? safeStr(responsible.getFullName(), "—") : "—"));
            String role = (responsible != null && responsible.getRole() != null)
                    ? safeStr(responsible.getRole().getDescription(), "")
                    : "";
            if (!role.isBlank()) {
                addParagraphText(document, "Должность: " + role);
            }
            addEmptyLine(document);

            // ===== Комментарий заведующего =====
            if (act.getComment() != null && !act.getComment().isBlank()) {
                addSectionHeader(document, "4. Комментарий составителя");
                addParagraphText(document, act.getComment());
                addEmptyLine(document);
            }

            // ===== Решение руководителя =====
            if (act.getDirectorComment() != null && !act.getDirectorComment().isBlank()) {
                addSectionHeader(document, "5. Решение руководителя");
                addParagraphText(document, act.getDirectorComment());
                addEmptyLine(document);
            }

            // ===== Подписи =====
            addEmptyLine(document);
            addParagraphText(document, "Составитель: ___________________________ / "
                    + (responsible != null ? safeStr(responsible.getFullName(), "") : "") + " /");
            addEmptyLine(document);
            addParagraphText(document, "Руководитель: ___________________________ / _____________________________ /");

            // ===== Результат =====
            byte[] content = toBytes(document);
            String safeNumber = safeStr(act.getActNumber(), "act").replace("/", "-");
            String fileName = "Акт_списания_" + safeNumber + ".docx";
            return new ReportDocumentService.ReportFile(fileName, content);
        }
    }

    // ========== ВСПОМОГАТЕЛЬНЫЕ ==========

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
