package com.mai.siarsp.service.general;

import com.mai.siarsp.models.Product;
import com.mai.siarsp.models.ProductAttributeValue;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.Optional;

/**
 * Контроль сроков годности: поиск рисковых остатков и запуск предупредительных действий.
 */

@Service
public class ProductExpirationService {

    public static final String EXPIRATION_ATTRIBUTE_NAME = "Срок годности";

    public Optional<LocalDate> getExpirationDate(Product product) {
        if (product == null || product.getAttributeValues() == null) {
            return Optional.empty();
        }

        for (ProductAttributeValue value : product.getAttributeValues()) {
            if (value.getAttribute() == null || value.getValue() == null) {
                continue;
            }
            if (!EXPIRATION_ATTRIBUTE_NAME.equalsIgnoreCase(value.getAttribute().getName())) {
                continue;
            }
            try {
                return Optional.of(LocalDate.parse(value.getValue().trim()));
            } catch (DateTimeParseException ignored) {
                return Optional.empty();
            }
        }
        return Optional.empty();
    }
}
