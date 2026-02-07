package com.mai.siarsp.mapper;

import com.mai.siarsp.dto.RequestForDeliveryDTO;
import com.mai.siarsp.models.RequestForDelivery;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;

import java.util.List;

@Mapper(uses = {RequestedProductMapper.class})
public interface RequestForDeliveryMapper {
    RequestForDeliveryMapper INSTANCE = Mappers.getMapper(RequestForDeliveryMapper.class);

    @Mapping(source = "supplier.id", target = "supplierId")
    @Mapping(source = "supplier.name", target = "supplierName")
    @Mapping(source = "delivery.id", target = "deliveryId")
    RequestForDeliveryDTO toDTO(RequestForDelivery request);

    List<RequestForDeliveryDTO> toDTOList(List<RequestForDelivery> requests);
}
