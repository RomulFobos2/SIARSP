package com.mai.siarsp.mapper;

import com.mai.siarsp.dto.ProductDTO;
import com.mai.siarsp.models.Product;
import com.mai.siarsp.models.ProductAttributeValue;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;
import org.mapstruct.factory.Mappers;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.List;

@Mapper(uses = {ProductAttributeValueMapper.class})
public interface ProductMapper {
    ProductMapper INSTANCE = Mappers.getMapper(ProductMapper.class);

    @Mapping(source = "category.id", target = "categoryId")
    @Mapping(source = "category.name", target = "categoryName")
    @Mapping(source = "product", target = "availableQuantity", qualifiedByName = "availableQuantity")
    @Mapping(source = "product", target = "expirationDate", qualifiedByName = "expirationDate")
    ProductDTO toDTO(Product product);

    List<ProductDTO> toDTOList(List<Product> products);

    @Named("availableQuantity")
    default int getAvailableQuantity(Product product) {
        return product.getAvailableQuantity();
    }

    @Named("expirationDate")
    default LocalDate getExpirationDate(Product product) {
        if (product.getAttributeValues() == null) {
            return null;
        }
        for (ProductAttributeValue value : product.getAttributeValues()) {
            if (value.getAttribute() == null || value.getValue() == null) {
                continue;
            }
            if (!"Срок годности".equalsIgnoreCase(value.getAttribute().getName())) {
                continue;
            }
            try {
                return LocalDate.parse(value.getValue().trim());
            } catch (DateTimeParseException ignored) {
                return null;
            }
        }
        return null;
    }
}
