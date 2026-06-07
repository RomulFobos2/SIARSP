package com.mai.siarsp.service.general;

import com.mai.siarsp.models.OrderedProduct;
import com.mai.siarsp.models.Supply;
import com.mai.siarsp.repo.OrderedProductRepository;
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

    public ProductPriceAggregateService(SupplyRepository supplyRepository,
                                        OrderedProductRepository orderedProductRepository) {
        this.supplyRepository = supplyRepository;
        this.orderedProductRepository = orderedProductRepository;
    }

    // ========== DTO ==========

    public record PurchasePriceSummary(BigDecimal lastPrice, LocalDate lastDate, String lastSupplierName,
                                       BigDecimal avgPrice, int sampleCount) {}

    public record SalePriceSummary(BigDecimal lastPrice, LocalDate lastDate, String lastClientName,
                                   BigDecimal avgPrice, int sampleCount) {}

    public record ProductSupplyRow(LocalDate deliveryDate, String supplierName,
                                   int quantity, String unit,
                                   BigDecimal purchasePrice, BigDecimal totalPrice) {}

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

    // ========== ЗАКАЗЫ ТОВАРА (источник: фактические партии Supply) ==========

    @Transactional(readOnly = true)
    public List<ProductSupplyRow> getProductSupplies(Long productId) {
        List<Supply> supplies = supplyRepository
                .findByProductIdOrderByDelivery_DeliveryDateDesc(productId);
        List<ProductSupplyRow> rows = new ArrayList<>(supplies.size());
        for (Supply s : supplies) {
            LocalDate date = s.getDelivery() != null ? s.getDelivery().getDeliveryDate() : null;
            String supplier = (s.getDelivery() != null && s.getDelivery().getSupplier() != null)
                    ? s.getDelivery().getSupplier().getName() : "—";
            rows.add(new ProductSupplyRow(
                    date,
                    supplier,
                    s.getQuantity(),
                    s.getUnit(),
                    s.getPurchasePrice(),
                    s.getTotalPrice()
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
