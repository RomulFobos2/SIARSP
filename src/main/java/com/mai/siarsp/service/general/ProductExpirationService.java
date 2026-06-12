package com.mai.siarsp.service.general;

import com.mai.siarsp.models.Product;
import com.mai.siarsp.models.Supply;
import com.mai.siarsp.repo.SupplyRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * Контроль сроков годности в модели партионного учёта.
 * <p>
 * Атрибут «Срок годности» теперь хранится как Integer (число дней) на товаре. Конкретные даты
 * партий хранятся в Supply (productionDate + expirationDate). Этот сервис содержит helper-методы
 * для расчётов и выборок по партиям.
 */
@Service
public class ProductExpirationService {

    public static final String EXPIRATION_ATTRIBUTE_NAME = "Срок годности";

    private final SupplyRepository supplyRepository;

    public ProductExpirationService(SupplyRepository supplyRepository) {
        this.supplyRepository = supplyRepository;
    }

    /**
     * Срок хранения товара в днях.
     */
    public Optional<Integer> getShelfLifeDays(Product product) {
        if (product == null) return Optional.empty();
        Integer days = product.getShelfLifeDays();
        return Optional.ofNullable(days);
    }

    /**
     * Вычисляет expirationDate партии = productionDate + shelfLifeDays.
     * Если у товара не задан shelfLifeDays — возвращает productionDate (= партия «бесконечного» хранения).
     */
    public LocalDate calculateExpirationDate(Supply supply) {
        if (supply == null || supply.getProductionDate() == null) return null;
        Integer days = supply.getProduct() != null ? supply.getProduct().getShelfLifeDays() : null;
        if (days == null) return supply.getProductionDate();
        return supply.getProductionDate().plusDays(days);
    }

    /**
     * Минимальный срок годности по партиям товара, лежащим на складе, не истекшим к referenceDate.
     * Используется для предупреждения, что заказ нельзя оформить из-за просрочки на дату доставки.
     */
    @Transactional(readOnly = true)
    public Optional<LocalDate> getEarliestUnexpiredExpiration(Product product, LocalDate referenceDate) {
        if (product == null || product.getId() == null) return Optional.empty();
        LocalDate min = supplyRepository.findEarliestUnexpiredOnStock(product.getId(), referenceDate);
        return Optional.ofNullable(min);
    }

    /**
     * Партии, лежащие на складе, у которых срок годности истёк к указанной дате.
     * Используется для автосписания и проверки перед отгрузкой.
     */
    @Transactional(readOnly = true)
    public List<Supply> findExpiredSuppliesOnStock(LocalDate today) {
        return supplyRepository.findExpiredOnStock(today);
    }

    /**
     * @deprecated старая семантика (Product-level expiration); используйте
     * {@link #getEarliestUnexpiredExpiration(Product, LocalDate)} либо
     * {@link #findExpiredSuppliesOnStock(LocalDate)}.
     */
    @Deprecated
    public Optional<LocalDate> getExpirationDate(Product product) {
        return Optional.empty();
    }
}
