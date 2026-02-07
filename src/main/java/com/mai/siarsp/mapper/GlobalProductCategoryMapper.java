package com.mai.siarsp.mapper;

import com.mai.siarsp.dto.GlobalProductCategoryDTO;
import com.mai.siarsp.models.GlobalProductCategory;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

import java.util.List;

@Mapper
public interface GlobalProductCategoryMapper {
    GlobalProductCategoryMapper INSTANCE = Mappers.getMapper(GlobalProductCategoryMapper.class);

    GlobalProductCategoryDTO toDTO(GlobalProductCategory globalProductCategory);

    List<GlobalProductCategoryDTO> toDTOList(List<GlobalProductCategory> categories);
}
