package com.mai.siarsp.mapper;

import com.mai.siarsp.dto.ProductDTO;
import com.mai.siarsp.models.Product;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;
import org.mapstruct.factory.Mappers;

import java.util.List;

@Mapper(uses = {ProductAttributeValueMapper.class})
public interface ProductMapper {
    ProductMapper INSTANCE = Mappers.getMapper(ProductMapper.class);

    @Mapping(source = "category.id", target = "categoryId")
    @Mapping(source = "category.name", target = "categoryName")
    @Mapping(source = "product", target = "availableQuantity", qualifiedByName = "availableQuantity")
    ProductDTO toDTO(Product product);

    List<ProductDTO> toDTOList(List<Product> products);

    @Named("availableQuantity")
    default int getAvailableQuantity(Product product) {
        return product.getAvailableQuantity();
    }
}
