package com.mai.siarsp.mapper;

import com.mai.siarsp.dto.ProductAttributeDTO;
import com.mai.siarsp.models.ProductAttribute;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

import java.util.List;

@Mapper
public interface ProductAttributeMapper {
    ProductAttributeMapper INSTANCE = Mappers.getMapper(ProductAttributeMapper.class);

    ProductAttributeDTO toDTO(ProductAttribute productAttribute);

    List<ProductAttributeDTO> toDTOList(List<ProductAttribute> attributes);
}
