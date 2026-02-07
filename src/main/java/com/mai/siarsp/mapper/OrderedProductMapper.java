package com.mai.siarsp.mapper;

import com.mai.siarsp.dto.OrderedProductDTO;
import com.mai.siarsp.models.OrderedProduct;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;

import java.util.List;

@Mapper
public interface OrderedProductMapper {
    OrderedProductMapper INSTANCE = Mappers.getMapper(OrderedProductMapper.class);

    @Mapping(source = "clientOrder.id", target = "clientOrderId")
    @Mapping(source = "product.id", target = "productId")
    @Mapping(source = "product.name", target = "productName")
    @Mapping(source = "product.article", target = "productArticle")
    OrderedProductDTO toDTO(OrderedProduct orderedProduct);

    List<OrderedProductDTO> toDTOList(List<OrderedProduct> orderedProducts);
}
