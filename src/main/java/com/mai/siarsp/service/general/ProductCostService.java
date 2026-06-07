package com.mai.siarsp.service.general;

import com.mai.siarsp.models.Supply;
import com.mai.siarsp.repo.SupplyRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

/**
 * Простой helper для оценки стоимости товара на складе по последней закупочной цене.
 * Используется как для агрегированных карточек на странице склада/зоны, так и для отдельных позиций.
 */
@Service
public class ProductCostService {

    private final SupplyRepository supplyRepository;

    public ProductCostService(SupplyRepository supplyRepository) {
        this.supplyRepository = supplyRepository;
    }

    /**
     * Возвращает последнюю закупочную цену товара (из самой свежей Supply по дате поставки)
     * или null, если закупок нет / цена не задана.
     */
    @Transactional(readOnly = true)
    public BigDecimal getLastPurchasePrice(Long productId) {
        List<Supply> supplies = supplyRepository.findByProductIdOrderByDelivery_DeliveryDateDesc(productId);
        if (supplies.isEmpty()) return null;
        return supplies.get(0).getPurchasePrice();
    }

    /**
     * Возвращает стоимость указанного количества товара по последней закупочной цене.
     * null, если у товара нет закупок.
     */
    @Transactional(readOnly = true)
    public BigDecimal calculateStockCost(Long productId, int quantity) {
        if (quantity <= 0) return BigDecimal.ZERO;
        BigDecimal price = getLastPurchasePrice(productId);
        if (price == null) return null;
        return price.multiply(BigDecimal.valueOf(quantity));
    }
}
