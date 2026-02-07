package com.mai.siarsp.mapper;

import com.mai.siarsp.dto.DeliveryDTO;
import com.mai.siarsp.models.Delivery;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;
import org.mapstruct.factory.Mappers;

import java.math.BigDecimal;
import java.util.List;

@Mapper(uses = {SupplyMapper.class})
public interface DeliveryMapper {
    DeliveryMapper INSTANCE = Mappers.getMapper(DeliveryMapper.class);

    @Mapping(source = "supplier.id", target = "supplierId")
    @Mapping(source = "supplier.name", target = "supplierName")
    @Mapping(source = "request.id", target = "requestId")
    @Mapping(source = "delivery", target = "totalCost", qualifiedByName = "totalCost")
    DeliveryDTO toDTO(Delivery delivery);

    List<DeliveryDTO> toDTOList(List<Delivery> deliveries);

    @Named("totalCost")
    default BigDecimal getTotalCost(Delivery delivery) {
        return delivery.getTotalCost();
    }
}
