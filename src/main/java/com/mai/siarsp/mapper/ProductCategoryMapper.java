package com.mai.siarsp.mapper;

import com.mai.siarsp.dto.ProductCategoryDTO;
import com.mai.siarsp.models.ProductCategory;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;
import org.mapstruct.factory.Mappers;

import java.util.List;

@Mapper(uses = {ProductAttributeMapper.class})
public interface ProductCategoryMapper {
    ProductCategoryMapper INSTANCE = Mappers.getMapper(ProductCategoryMapper.class);

    @Mapping(source = "globalProductCategory.id", target = "globalProductCategoryId")
    @Mapping(source = "globalProductCategory.name", target = "globalProductCategoryName")
    @Mapping(source = "category", target = "displayName", qualifiedByName = "displayName")
    ProductCategoryDTO toDTO(ProductCategory category);

    List<ProductCategoryDTO> toDTOList(List<ProductCategory> categories);

    @Named("displayName")
    default String getDisplayName(ProductCategory category) {
        return category.getDisplayName();
    }
}
