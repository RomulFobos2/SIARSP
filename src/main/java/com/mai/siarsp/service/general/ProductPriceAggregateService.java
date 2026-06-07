package com.mai.siarsp.service.general;

import com.mai.siarsp.models.OrderedProduct;
import com.mai.siarsp.models.RequestedProduct;
import com.mai.siarsp.models.Supply;
import com.mai.siarsp.repo.OrderedProductRepository;
import com.mai.siarsp.repo.RequestedProductRepository;
import com.mai.siarsp.repo.SupplyRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Агрегаты цен по товару для карточки товара:
 * — последняя/средняя закупочная цена (Supply),
 * — последняя/средняя продажная цена (OrderedProduct),
 * — строки таблицы «Заказы товара» (RequestedProduct).
 */
@Service
public class ProductPriceAggregateService {

    private final SupplyRepository supplyRepository;
    private final OrderedProductRepository orderedProductRepository;
    private final RequestedProductRepository requestedProductRepository;

    public ProductPriceAggregateService(SupplyRepository supplyRepository,
                                        OrderedProductRepository orderedProductRepository,
                                        RequestedProductRepository requestedProductRepository) {
        this.supplyRepository = supplyRepository;
        this.orderedProductRepository = orderedProductRepository;
        this.requestedProductRepository = requestedProductRepository;
    }

    // ========== DTO ==========

    public record PurchasePriceSummary(BigDecimal lastPrice, LocalDate lastDate, String lastSupplierName,
                                       BigDecimal avgPrice, int sampleCount) {}

    public record SalePriceSummary(BigDecimal lastPrice, LocalDate lastDate, String lastClientName,
                                   BigDecimal avgPrice, int sampleCount) {}

    public record ProductRequestRow(Long requestId, LocalDate requestDate, String supplierName,
                                    int quantity, BigDecimal purchasePrice, BigDecimal totalPrice,
                                    String statusDisplayName) {}

    // ========== ЗАКУПОЧНАЯ ЦЕНА ==========

    @Transactional(readOnly = true)
    public PurchasePriceSummary getPurchaseSummary(Long productId) {
        List<Supply> supplies = supplyRepository.findByProductIdOrderByDelivery_DeliveryDateDesc(productId);
        if (supplies.isEmpty()) {
            return new PurchasePriceSummary(null, null, null, null, 0);
        }
        Supply last = supplies.get(0);
        BigDecimal lastPrice = last.getPurchasePrice();
        LocalDate lastDate = last.getDelivery() != null ? last.getDelivery().getDeliveryDate() : null;
        String supplier = (last.getDelivery() != null && last.getDelivery().getSupplier() != null)
                ? last.getDelivery().getSupplier().getName() : null;

        BigDecimal avg = averagePrice(supplies.stream().map(Supply::getPurchasePrice).toList());
        return new PurchasePriceSummary(lastPrice, lastDate, supplier, avg, supplies.size());
    }

    // ========== ПРОДАЖНАЯ ЦЕНА ==========

    @Transactional(readOnly = true)
    public SalePriceSummary getSaleSummary(Long productId) {
        List<OrderedProduct> sales = orderedProductRepository
                .findByProductIdOrderByClientOrder_OrderDateDesc(productId);
        if (sales.isEmpty()) {
            return new SalePriceSummary(null, null, null, null, 0);
        }
        OrderedProduct last = sales.get(0);
        BigDecimal lastPrice = last.getPrice();
        LocalDate lastDate = (last.getClientOrder() != null && last.getClientOrder().getOrderDate() != null)
                ? last.getClientOrder().getOrderDate().toLocalDate() : null;
        String client = (last.getClientOrder() != null && last.getClientOrder().getClient() != null)
                ? last.getClientOrder().getClient().getDisplayName() : null;

        BigDecimal avg = averagePrice(sales.stream().map(OrderedProduct::getPrice).toList());
        return new SalePriceSummary(lastPrice, lastDate, client, avg, sales.size());
    }

    // ========== ЗАКАЗЫ ТОВАРА ==========

    @Transactional(readOnly = true)
    public List<ProductRequestRow> getProductRequests(Long productId) {
        List<RequestedProduct> items = requestedProductRepository
                .findByProductIdOrderByRequest_RequestDateDesc(productId);
        List<ProductRequestRow> rows = new ArrayList<>(items.size());
        for (RequestedProduct rp : items) {
            var req = rp.getRequest();
            if (req == null) continue;
            String supplierName = req.getSupplier() != null ? req.getSupplier().getName() : "—";
            String statusName = req.getStatus() != null ? req.getStatus().getDisplayName() : "—";
            rows.add(new ProductRequestRow(
                    req.getId(),
                    req.getRequestDate(),
                    supplierName,
                    rp.getQuantity(),
                    rp.getPurchasePrice(),
                    rp.getTotalPrice(),
                    statusName
            ));
        }
        return rows;
    }

    // ========== ВСПОМОГАТЕЛЬНОЕ ==========

    private BigDecimal averagePrice(List<BigDecimal> prices) {
        BigDecimal sum = BigDecimal.ZERO;
        int count = 0;
        for (BigDecimal p : prices) {
            if (p == null) continue;
            sum = sum.add(p);
            count++;
        }
        if (count == 0) return null;
        return sum.divide(BigDecimal.valueOf(count), 2, RoundingMode.HALF_UP);
    }
}
