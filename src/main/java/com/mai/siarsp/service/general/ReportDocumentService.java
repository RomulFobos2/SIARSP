package com.mai.siarsp.service.general;

import com.mai.siarsp.models.ClientOrder;
import com.mai.siarsp.models.Product;
import com.mai.siarsp.repo.ClientOrderRepository;
import com.mai.siarsp.repo.ProductRepository;
import org.apache.poi.xwpf.usermodel.ParagraphAlignment;
import org.apache.poi.xwpf.usermodel.TableRowAlign;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFRun;
import org.apache.poi.xwpf.usermodel.XWPFTable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;

@Service
public class ReportDocumentService {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd.MM.yyyy");

    private final ClientOrderRepository clientOrderRepository;
    private final ProductRepository productRepository;
    private final ProductExpirationService productExpirationService;

    public ReportDocumentService(ClientOrderRepository clientOrderRepository,
                                 ProductRepository productRepository,
                                 ProductExpirationService productExpirationService) {
        this.clientOrderRepository = clientOrderRepository;
        this.productRepository = productRepository;
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
            setHeader(table, 0, "№ заказа", "Дата заказа", "Клиент", "Статус", "Сумма, руб.");
            for (int i = 0; i < orders.size(); i++) {
                ClientOrder order = orders.get(i);
                table.getRow(i + 1).getCell(0).setText(order.getOrderNumber());
                table.getRow(i + 1).getCell(1).setText(order.getOrderDate().toLocalDate().format(DATE_FORMATTER));
                table.getRow(i + 1).getCell(2).setText(order.getClient() != null ? order.getClient().getName() : "—");
                table.getRow(i + 1).getCell(3).setText(order.getStatus() != null ? order.getStatus().name() : "—");
                table.getRow(i + 1).getCell(4).setText(order.getTotalAmount() != null ? order.getTotalAmount().toString() : "0");
            }

            int totalRowIndex = orders.size() + 1;
            table.getRow(totalRowIndex).getCell(0).setText("ИТОГО");
            table.getRow(totalRowIndex).getCell(4).setText(totalAmount.toString());

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
            setHeader(table, 0, "Артикул", "Товар", "Категория", "Остаток", "Резерв", "Доступно");
            for (int i = 0; i < products.size(); i++) {
                Product product = products.get(i);
                int availableQuantity = product.getStockQuantity() - product.getReservedQuantity();
                table.getRow(i + 1).getCell(0).setText(product.getArticle());
                table.getRow(i + 1).getCell(1).setText(product.getName());
                table.getRow(i + 1).getCell(2).setText(product.getCategory() != null ? product.getCategory().getName() : "—");
                table.getRow(i + 1).getCell(3).setText(String.valueOf(product.getStockQuantity()));
                table.getRow(i + 1).getCell(4).setText(String.valueOf(product.getReservedQuantity()));
                table.getRow(i + 1).getCell(5).setText(String.valueOf(Math.max(availableQuantity, 0)));
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
            setHeader(table, 0, "Показатель", "Значение");
            table.getRow(1).getCell(0).setText("Количество заказов за период");
            table.getRow(1).getCell(1).setText(String.valueOf(orders.size()));
            table.getRow(2).getCell(0).setText("Сумма заказов за период, руб.");
            table.getRow(2).getCell(1).setText(totalRevenue.toString());
            table.getRow(3).getCell(0).setText("Средний чек, руб.");
            table.getRow(3).getCell(1).setText(averageOrderAmount.toString());
            table.getRow(4).getCell(0).setText("Общий остаток товаров на складе");
            table.getRow(4).getCell(1).setText(String.valueOf(totalStock));
            table.getRow(5).getCell(0).setText("Товаров с истечением срока в периоде");
            table.getRow(5).getCell(1).setText(String.valueOf(expiringProducts));

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
            setHeader(table, 0, "Артикул", "Товар", "Остаток", "Срок годности");
            for (int i = 0; i < products.size(); i++) {
                ExpiringProductRow row = products.get(i);
                table.getRow(i + 1).getCell(0).setText(row.product().getArticle());
                table.getRow(i + 1).getCell(1).setText(row.product().getName());
                table.getRow(i + 1).getCell(2).setText(String.valueOf(row.product().getStockQuantity()));
                table.getRow(i + 1).getCell(3).setText(row.expirationDate().format(DATE_FORMATTER));
            }

            return new ReportFile(buildFileName("expiration", startDate, endDate), toBytes(document));
        } catch (IOException exception) {
            throw new IllegalStateException("Не удалось сформировать отчёт по срокам годности", exception);
        }
    }

    private void addTitle(XWPFDocument document, String title) {
        XWPFParagraph paragraph = document.createParagraph();
        paragraph.setAlignment(ParagraphAlignment.CENTER);
        XWPFRun run = paragraph.createRun();
        run.setBold(true);
        run.setFontSize(14);
        run.setText(title);
    }

    private void addPeriod(XWPFDocument document, LocalDate startDate, LocalDate endDate) {
        XWPFParagraph paragraph = document.createParagraph();
        paragraph.setAlignment(ParagraphAlignment.CENTER);
        XWPFRun run = paragraph.createRun();
        run.setText("Период: " + startDate.format(DATE_FORMATTER) + " - " + endDate.format(DATE_FORMATTER));
    }

    private void setHeader(XWPFTable table, int rowIndex, String... headers) {
        for (int i = 0; i < headers.length; i++) {
            table.getRow(rowIndex).getCell(i).setText(headers[i]);
        }
    }

    private void formatTable(XWPFTable table) {
        table.setTableAlignment(TableRowAlign.CENTER);
    }

    private byte[] toBytes(XWPFDocument document) throws IOException {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        document.write(outputStream);
        return outputStream.toByteArray();
    }

    private String buildFileName(String reportType, LocalDate startDate, LocalDate endDate) {
        return reportType + "_report_" + startDate + "_" + endDate + ".docx";
    }

    private record ExpiringProductRow(Product product, LocalDate expirationDate) {
    }

    public record ReportFile(String fileName, byte[] content) {
    }
}
