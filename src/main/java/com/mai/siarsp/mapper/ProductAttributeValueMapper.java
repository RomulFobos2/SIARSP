package com.mai.siarsp.mapper;

import com.mai.siarsp.dto.ProductAttributeValueDTO;
import com.mai.siarsp.models.ProductAttributeValue;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;

import java.util.List;

@Mapper
public interface ProductAttributeValueMapper {
    ProductAttributeValueMapper INSTANCE = Mappers.getMapper(ProductAttributeValueMapper.class);

    @Mapping(source = "product.id", target = "productId")
    @Mapping(source = "product.article", target = "productArticle")
    @Mapping(source = "attribute.id", target = "attributeId")
    @Mapping(source = "attribute.name", target = "attributeName")
    @Mapping(source = "attribute.unit", target = "attributeUnit")
    @Mapping(source = "attribute.dataType", target = "attributeDataType")
    ProductAttributeValueDTO toDTO(ProductAttributeValue value);

    List<ProductAttributeValueDTO> toDTOList(List<ProductAttributeValue> values);
}
