package com.mai.siarsp.mapper;

import com.mai.siarsp.dto.RequestedProductDTO;
import com.mai.siarsp.models.RequestedProduct;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;

import java.util.List;

@Mapper
public interface RequestedProductMapper {
    RequestedProductMapper INSTANCE = Mappers.getMapper(RequestedProductMapper.class);

    @Mapping(source = "request.id", target = "requestId")
    @Mapping(source = "product.id", target = "productId")
    @Mapping(source = "product.name", target = "productName")
    @Mapping(source = "product.article", target = "productArticle")
    RequestedProductDTO toDTO(RequestedProduct requestedProduct);

    List<RequestedProductDTO> toDTOList(List<RequestedProduct> requestedProducts);
}
