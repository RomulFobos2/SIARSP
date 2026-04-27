package com.mai.siarsp.service.general;

import com.mai.siarsp.models.ClientOrder;
import com.mai.siarsp.models.Delivery;
import com.mai.siarsp.models.Product;
import com.mai.siarsp.models.Supply;
import com.mai.siarsp.repo.ClientOrderRepository;
import com.mai.siarsp.repo.DeliveryRepository;
import com.mai.siarsp.repo.ProductRepository;
import org.apache.poi.xwpf.usermodel.ParagraphAlignment;
import org.apache.poi.xwpf.usermodel.TableRowAlign;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFRun;
import org.apache.poi.xwpf.usermodel.XWPFTable;
import org.apache.poi.xwpf.usermodel.XWPFTableCell;
import org.apache.poi.xwpf.usermodel.XWPFTableRow;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTBorder;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTShd;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTTblBorders;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.STBorder;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.STShd;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Формирование отчетных документов для операционной и управленческой отчетности.
 */

@Service
public class ReportDocumentService {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd.MM.yyyy");
    private static final String FONT_FAMILY = "Times New Roman";
    private static final int FONT_SIZE_DATA = 9;
    private static final String COLOR_HEADER = "D9E2F3";
    private static final String COLOR_ZEBRA = "F2F2F2";
    private static final String COLOR_TOTAL = "FFF2CC";

    private final ClientOrderRepository clientOrderRepository;
    private final ProductRepository productRepository;
    private final DeliveryRepository deliveryRepository;
    private final ProductExpirationService productExpirationService;

    public ReportDocumentService(ClientOrderRepository clientOrderRepository,
                                 ProductRepository productRepository,
                                 DeliveryRepository deliveryRepository,
                                 ProductExpirationService productExpirationService) {
        this.clientOrderRepository = clientOrderRepository;
        this.productRepository = productRepository;
        this.deliveryRepository = deliveryRepository;
        this.productExpirationService = productExpirationService;
    }

    @Transactional(readOnly = true)
    public ReportFile generateOrdersReport(LocalDate startDate, LocalDate endDate) {
        List<ClientOrder> orders = clientOrderRepository.findAllByOrderByOrderDateDesc().stream()
                .filter(order -> {
                    LocalDate orderDate = order.getOrderDate().toLocalDate();
                    return !orderDate.isBefore(startDate) && !orderDate.isAfter(endDate);
                })
                .toList();

        BigDecimal totalAmount = orders.stream()
                .map(ClientOrder::getTotalAmount)
                .filter(amount -> amount != null)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        try (XWPFDocument document = new XWPFDocument()) {
            addTitle(document, "Отчёт по заказам");
            addPeriod(document, startDate, endDate);

            XWPFTable table = document.createTable(Math.max(orders.size() + 2, 2), 5);
            formatTable(table);
            formatHeaderRow(table, 0, "№ заказа", "Дата заказа", "Клиент", "Статус", "Сумма, руб.");
            for (int i = 0; i < orders.size(); i++) {
                ClientOrder order = orders.get(i);
                boolean zebra = i % 2 == 1;
                formatDataCell(table.getRow(i + 1).getCell(0), order.getOrderNumber(), zebra);
                formatDataCell(table.getRow(i + 1).getCell(1), order.getOrderDate().toLocalDate().format(DATE_FORMATTER), zebra);
                formatDataCell(table.getRow(i + 1).getCell(2), order.getClient() != null ? order.getClient().getOrganizationName() : "—", zebra);
                formatDataCell(table.getRow(i + 1).getCell(3), order.getStatus() != null ? order.getStatus().getDisplayName() : "—", zebra);
                formatDataCell(table.getRow(i + 1).getCell(4), order.getTotalAmount() != null ? order.getTotalAmount().toString() : "0", zebra);
            }

            formatTotalRow(table, orders.size() + 1, 5, 0, "ИТОГО", 4, totalAmount.toString());

            return new ReportFile(buildFileName("orders", startDate, endDate), toBytes(document));
        } catch (IOException exception) {
            throw new IllegalStateException("Не удалось сформировать отчёт по заказам", exception);
        }
    }

    @Transactional(readOnly = true)
    public ReportFile generateStockReport(LocalDate startDate, LocalDate endDate) {
        List<Product> products = productRepository.findAll().stream()
                .sorted(Comparator.comparing(Product::getName, String.CASE_INSENSITIVE_ORDER))
                .toList();

        try (XWPFDocument document = new XWPFDocument()) {
            addTitle(document, "Отчёт по товарам на складе");
            addPeriod(document, startDate, endDate);

            XWPFTable table = document.createTable(Math.max(products.size() + 1, 2), 6);
            formatTable(table);
            formatHeaderRow(table, 0, "Артикул", "Товар", "Категория", "Остаток", "Резерв", "Доступно");
            for (int i = 0; i < products.size(); i++) {
                Product product = products.get(i);
                int availableQuantity = product.getStockQuantity() - product.getReservedQuantity();
                boolean zebra = i % 2 == 1;
                formatDataCell(table.getRow(i + 1).getCell(0), product.getArticle(), zebra);
                formatDataCell(table.getRow(i + 1).getCell(1), product.getName(), zebra);
                formatDataCell(table.getRow(i + 1).getCell(2), product.getCategory() != null ? product.getCategory().getName() : "—", zebra);
                formatDataCell(table.getRow(i + 1).getCell(3), String.valueOf(product.getStockQuantity()), zebra);
                formatDataCell(table.getRow(i + 1).getCell(4), String.valueOf(product.getReservedQuantity()), zebra);
                formatDataCell(table.getRow(i + 1).getCell(5), String.valueOf(Math.max(availableQuantity, 0)), zebra);
            }

            return new ReportFile(buildFileName("warehouse", startDate, endDate), toBytes(document));
        } catch (IOException exception) {
            throw new IllegalStateException("Не удалось сформировать отчёт по товарам", exception);
        }
    }

    @Transactional(readOnly = true)
    public ReportFile generateStatisticsReport(LocalDate startDate, LocalDate endDate) {
        List<ClientOrder> orders = clientOrderRepository.findAllByOrderByOrderDateDesc().stream()
                .filter(order -> {
                    LocalDate orderDate = order.getOrderDate().toLocalDate();
                    return !orderDate.isBefore(startDate) && !orderDate.isAfter(endDate);
                })
                .toList();
        List<Product> products = productRepository.findAllWithAttributeValues();

        BigDecimal totalRevenue = orders.stream()
                .map(ClientOrder::getTotalAmount)
                .filter(amount -> amount != null)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal averageOrderAmount = orders.isEmpty()
                ? BigDecimal.ZERO
                : totalRevenue.divide(BigDecimal.valueOf(orders.size()), 2, RoundingMode.HALF_UP);

        int totalStock = products.stream().mapToInt(Product::getStockQuantity).sum();
        long expiringProducts = products.stream()
                .filter(product -> productExpirationService.getExpirationDate(product)
                        .filter(expirationDate -> !expirationDate.isBefore(startDate) && !expirationDate.isAfter(endDate))
                        .isPresent())
                .count();

        try (XWPFDocument document = new XWPFDocument()) {
            addTitle(document, "Статистический отчёт");
            addPeriod(document, startDate, endDate);

            XWPFTable table = document.createTable(6, 2);
            formatTable(table);
            formatHeaderRow(table, 0, "Показатель", "Значение");
            String[][] data = {
                    {"Количество заказов за период", String.valueOf(orders.size())},
                    {"Сумма заказов за период, руб.", totalRevenue.toString()},
                    {"Средний чек, руб.", averageOrderAmount.toString()},
                    {"Общий остаток товаров на складе", String.valueOf(totalStock)},
                    {"Товаров с истечением срока в периоде", String.valueOf(expiringProducts)}
            };
            for (int i = 0; i < data.length; i++) {
                boolean zebra = i % 2 == 1;
                formatDataCell(table.getRow(i + 1).getCell(0), data[i][0], zebra);
                formatDataCell(table.getRow(i + 1).getCell(1), data[i][1], zebra);
            }

            return new ReportFile(buildFileName("statistics", startDate, endDate), toBytes(document));
        } catch (IOException exception) {
            throw new IllegalStateException("Не удалось сформировать статистический отчёт", exception);
        }
    }

    @Transactional(readOnly = true)
    public ReportFile generateExpirationReport(LocalDate startDate, LocalDate endDate) {
        List<ExpiringProductRow> products = productRepository.findAllWithAttributeValues().stream()
                .map(product -> productExpirationService.getExpirationDate(product)
                        .map(expirationDate -> new ExpiringProductRow(product, expirationDate))
                        .orElse(null))
                .filter(row -> row != null
                        && !row.expirationDate().isBefore(startDate)
                        && !row.expirationDate().isAfter(endDate))
                .sorted(Comparator.comparing(ExpiringProductRow::expirationDate))
                .toList();

        try (XWPFDocument document = new XWPFDocument()) {
            addTitle(document, "Отчёт по срокам годности");
            addPeriod(document, startDate, endDate);

            XWPFTable table = document.createTable(Math.max(products.size() + 1, 2), 4);
            formatTable(table);
            formatHeaderRow(table, 0, "Артикул", "Товар", "Остаток", "Срок годности");
            for (int i = 0; i < products.size(); i++) {
                ExpiringProductRow row = products.get(i);
                boolean zebra = i % 2 == 1;
                formatDataCell(table.getRow(i + 1).getCell(0), row.product().getArticle(), zebra);
                formatDataCell(table.getRow(i + 1).getCell(1), row.product().getName(), zebra);
                formatDataCell(table.getRow(i + 1).getCell(2), String.valueOf(row.product().getStockQuantity()), zebra);
                formatDataCell(table.getRow(i + 1).getCell(3), row.expirationDate().format(DATE_FORMATTER), zebra);
            }

            return new ReportFile(buildFileName("expiration", startDate, endDate), toBytes(document));
        } catch (IOException exception) {
            throw new IllegalStateException("Не удалось сформировать отчёт по срокам годности", exception);
        }
    }

    @Transactional(readOnly = true)
    public ReportFile generateSuppliesReport(LocalDate startDate, LocalDate endDate) {
        List<Delivery> deliveries = deliveryRepository.findAllByOrderByDeliveryDateDesc().stream()
                .filter(delivery -> !delivery.getDeliveryDate().isBefore(startDate)
                        && !delivery.getDeliveryDate().isAfter(endDate))
                .toList();

        List<SupplyRow> rows = new ArrayList<>();
        for (Delivery delivery : deliveries) {
            for (Supply supply : delivery.getSupplies()) {
                rows.add(new SupplyRow(delivery, supply));
            }
        }

        BigDecimal totalAmount = rows.stream()
                .map(row -> row.supply().getTotalPrice())
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        try (XWPFDocument document = new XWPFDocument()) {
            addTitle(document, "Отчёт о поставках");
            addPeriod(document, startDate, endDate);
            addSummaryLine(document, "Всего поставок: " + deliveries.size()
                    + ", позиций: " + rows.size()
                    + ", на сумму: " + totalAmount + " руб.");

            XWPFTable table = document.createTable(Math.max(rows.size() + 2, 2), 11);
            formatTable(table);
            formatHeaderRow(table, 0,
                    "№ поставки", "Дата", "Поставщик", "ИНН", "Товар",
                    "Артикул", "Ед. изм.", "Кол-во", "Цена, руб.", "Сумма, руб.", "Дефицит");

            for (int i = 0; i < rows.size(); i++) {
                SupplyRow row = rows.get(i);
                Delivery d = row.delivery();
                Supply s = row.supply();
                boolean zebra = i % 2 == 1;
                formatDataCell(table.getRow(i + 1).getCell(0), String.valueOf(d.getId()), zebra);
                formatDataCell(table.getRow(i + 1).getCell(1), d.getDeliveryDate().format(DATE_FORMATTER), zebra);
                formatDataCell(table.getRow(i + 1).getCell(2), d.getSupplier() != null ? d.getSupplier().getName() : "—", zebra);
                formatDataCell(table.getRow(i + 1).getCell(3), d.getSupplier() != null ? d.getSupplier().getInn() : "—", zebra);
                formatDataCell(table.getRow(i + 1).getCell(4), s.getProduct() != null ? s.getProduct().getName() : "—", zebra);
                formatDataCell(table.getRow(i + 1).getCell(5), s.getProduct() != null ? s.getProduct().getArticle() : "—", zebra);
                formatDataCell(table.getRow(i + 1).getCell(6), s.getUnit() != null ? s.getUnit() : "—", zebra);
                formatDataCell(table.getRow(i + 1).getCell(7), String.valueOf(s.getQuantity()), zebra);
                formatDataCell(table.getRow(i + 1).getCell(8), s.getPurchasePrice() != null ? s.getPurchasePrice().toString() : "0", zebra);
                formatDataCell(table.getRow(i + 1).getCell(9), s.getTotalPrice().toString(), zebra);
                formatDataCell(table.getRow(i + 1).getCell(10), s.getDeficitQuantity() > 0
                        ? s.getDeficitQuantity() + (s.getDeficitReason() != null ? " (" + s.getDeficitReason() + ")" : "")
                        : "—", zebra);
            }

            formatTotalRow(table, rows.size() + 1, 11, 0, "ИТОГО", 9, totalAmount.toString());

            return new ReportFile(buildFileName("supplies", startDate, endDate), toBytes(document));
        } catch (IOException exception) {
            throw new IllegalStateException("Не удалось сформировать отчёт о поставках", exception);
        }
    }

    // ========== ВСПОМОГАТЕЛЬНЫЕ МЕТОДЫ ==========

    private void addTitle(XWPFDocument document, String title) {
        XWPFParagraph paragraph = document.createParagraph();
        paragraph.setAlignment(ParagraphAlignment.CENTER);
        XWPFRun run = paragraph.createRun();
        run.setBold(true);
        run.setFontSize(14);
        run.setFontFamily(FONT_FAMILY);
        run.setText(title);
    }

    private void addPeriod(XWPFDocument document, LocalDate startDate, LocalDate endDate) {
        XWPFParagraph paragraph = document.createParagraph();
        paragraph.setAlignment(ParagraphAlignment.CENTER);
        paragraph.setSpacingAfter(200);
        XWPFRun run = paragraph.createRun();
        run.setFontSize(11);
        run.setFontFamily(FONT_FAMILY);
        run.setText("Период: " + startDate.format(DATE_FORMATTER) + " \u2013 " + endDate.format(DATE_FORMATTER));
    }

    private void addSummaryLine(XWPFDocument document, String text) {
        XWPFParagraph paragraph = document.createParagraph();
        paragraph.setSpacingAfter(100);
        XWPFRun run = paragraph.createRun();
        run.setFontSize(11);
        run.setFontFamily(FONT_FAMILY);
        run.setItalic(true);
        run.setText(text);
    }

    private void formatTable(XWPFTable table) {
        table.setTableAlignment(TableRowAlign.CENTER);
        setTableBorders(table);
    }

    private void formatHeaderRow(XWPFTable table, int rowIndex, String... headers) {
        XWPFTableRow row = table.getRow(rowIndex);
        for (int i = 0; i < headers.length; i++) {
            XWPFTableCell cell = row.getCell(i);
            setCellText(cell, headers[i], FONT_SIZE_DATA, true);
            setCellBackground(cell, COLOR_HEADER);
        }
    }

    private void formatDataCell(XWPFTableCell cell, String text, boolean zebra) {
        setCellText(cell, text, FONT_SIZE_DATA, false);
        if (zebra) {
            setCellBackground(cell, COLOR_ZEBRA);
        }
    }

    private void formatTotalRow(XWPFTable table, int rowIndex, int colCount,
                                 int labelCellIndex, String label,
                                 int valueCellIndex, String value) {
        XWPFTableRow row = table.getRow(rowIndex);
        for (int i = 0; i < colCount; i++) {
            XWPFTableCell cell = row.getCell(i);
            if (i == labelCellIndex) {
                setCellText(cell, label, FONT_SIZE_DATA, true);
            } else if (i == valueCellIndex) {
                setCellText(cell, value, FONT_SIZE_DATA, true);
            } else {
                setCellText(cell, "", FONT_SIZE_DATA, false);
            }
            setCellBackground(cell, COLOR_TOTAL);
        }
    }

    private void setCellText(XWPFTableCell cell, String text, int fontSize, boolean bold) {
        if (cell.getParagraphs().size() > 0) {
            cell.removeParagraph(0);
        }
        XWPFParagraph paragraph = cell.addParagraph();
        paragraph.setSpacingAfter(0);
        paragraph.setSpacingBefore(0);
        XWPFRun run = paragraph.createRun();
        run.setText(text != null ? text : "");
        run.setFontSize(fontSize);
        run.setBold(bold);
        run.setFontFamily(FONT_FAMILY);
    }

    private void setCellBackground(XWPFTableCell cell, String hexColor) {
        CTShd shd = cell.getCTTc().addNewTcPr().addNewShd();
        shd.setFill(hexColor);
        shd.setVal(STShd.CLEAR);
    }

    private void setTableBorders(XWPFTable table) {
        CTTblBorders borders = table.getCTTbl().getTblPr().addNewTblBorders();
        setBorder(borders.addNewTop());
        setBorder(borders.addNewBottom());
        setBorder(borders.addNewLeft());
        setBorder(borders.addNewRight());
        setBorder(borders.addNewInsideH());
        setBorder(borders.addNewInsideV());
    }

    private void setBorder(CTBorder border) {
        border.setVal(STBorder.SINGLE);
        border.setSz(BigInteger.valueOf(4));
        border.setSpace(BigInteger.ZERO);
        border.setColor("000000");
    }

    private byte[] toBytes(XWPFDocument document) throws IOException {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        document.write(outputStream);
        return outputStream.toByteArray();
    }

    private String buildFileName(String reportType, LocalDate startDate, LocalDate endDate) {
        return reportType + "_report_" + startDate + "_" + endDate + ".docx";
    }

    private record SupplyRow(Delivery delivery, Supply supply) {
    }

    private record ExpiringProductRow(Product product, LocalDate expirationDate) {
    }

    public record ReportFile(String fileName, byte[] content) {
    }
}
