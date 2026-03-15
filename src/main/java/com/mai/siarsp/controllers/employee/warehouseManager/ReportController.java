package com.mai.siarsp.controllers.employee.warehouseManager;

import com.mai.siarsp.service.general.ReportDocumentService;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.time.LocalDate;

@Controller("warehouseManagerReportController")
@RequestMapping("/employee/warehouseManager/reports")
public class ReportController {

    private final ReportDocumentService reportDocumentService;

    public ReportController(ReportDocumentService reportDocumentService) {
        this.reportDocumentService = reportDocumentService;
    }

    @GetMapping
    public String reportsPage() {
        return "employee/general/reports/reportGenerator";
    }


    @GetMapping("/expiring-products")
    public String expiringProductsRedirect() {
        return "redirect:/employee/warehouseManager/reports";
    }
    @GetMapping("/download/orders")
    public ResponseEntity<byte[]> downloadOrdersReport(@RequestParam(required = false) LocalDate startDate,
                                                       @RequestParam(required = false) LocalDate endDate) {
        return buildResponse(reportDocumentService.generateOrdersReport(resolveStartDate(startDate, endDate),
                resolveEndDate(startDate, endDate)));
    }

    @GetMapping("/download/warehouse")
    public ResponseEntity<byte[]> downloadWarehouseReport(@RequestParam(required = false) LocalDate startDate,
                                                          @RequestParam(required = false) LocalDate endDate) {
        return buildResponse(reportDocumentService.generateStockReport(resolveStartDate(startDate, endDate),
                resolveEndDate(startDate, endDate)));
    }

    @GetMapping("/download/statistics")
    public ResponseEntity<byte[]> downloadStatisticsReport(@RequestParam(required = false) LocalDate startDate,
                                                           @RequestParam(required = false) LocalDate endDate) {
        return buildResponse(reportDocumentService.generateStatisticsReport(resolveStartDate(startDate, endDate),
                resolveEndDate(startDate, endDate)));
    }

    @GetMapping("/download/expiring-products")
    public ResponseEntity<byte[]> downloadExpirationReport(@RequestParam(required = false) LocalDate startDate,
                                                           @RequestParam(required = false) LocalDate endDate) {
        return buildResponse(reportDocumentService.generateExpirationReport(resolveStartDate(startDate, endDate),
                resolveEndDate(startDate, endDate)));
    }

    @GetMapping("/download/supplies")
    public ResponseEntity<byte[]> downloadSuppliesReport(@RequestParam(required = false) LocalDate startDate,
                                                          @RequestParam(required = false) LocalDate endDate) {
        return buildResponse(reportDocumentService.generateSuppliesReport(resolveStartDate(startDate, endDate),
                resolveEndDate(startDate, endDate)));
    }

    private ResponseEntity<byte[]> buildResponse(ReportDocumentService.ReportFile reportFile) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.wordprocessingml.document"));
        headers.setContentDisposition(ContentDisposition.attachment().filename(reportFile.fileName()).build());
        return ResponseEntity.ok().headers(headers).body(reportFile.content());
    }

    private LocalDate resolveStartDate(LocalDate startDate, LocalDate endDate) {
        LocalDate start = startDate != null ? startDate : LocalDate.now().minusDays(7);
        LocalDate end = endDate != null ? endDate : LocalDate.now();
        return start.isBefore(end) || start.isEqual(end) ? start : end;
    }

    private LocalDate resolveEndDate(LocalDate startDate, LocalDate endDate) {
        LocalDate start = startDate != null ? startDate : LocalDate.now().minusDays(7);
        LocalDate end = endDate != null ? endDate : LocalDate.now();
        return start.isBefore(end) || start.isEqual(end) ? end : start;
    }
}
