package com.mai.siarsp.mapper;

import com.mai.siarsp.dto.SupplyDTO;
import com.mai.siarsp.models.Supply;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;
import org.mapstruct.factory.Mappers;

import java.math.BigDecimal;
import java.util.List;

@Mapper
public interface SupplyMapper {
    SupplyMapper INSTANCE = Mappers.getMapper(SupplyMapper.class);

    @Mapping(source = "delivery.id", target = "deliveryId")
    @Mapping(source = "product.id", target = "productId")
    @Mapping(source = "product.name", target = "productName")
    @Mapping(source = "product.article", target = "productArticle")
    @Mapping(source = "supply", target = "totalPrice", qualifiedByName = "totalPrice")
    @Mapping(target = "orderedQuantity", ignore = true)
    SupplyDTO toDTO(Supply supply);

    List<SupplyDTO> toDTOList(List<Supply> supplies);

    @Named("totalPrice")
    default BigDecimal getTotalPrice(Supply supply) {
        return supply.getTotalPrice();
    }
}
