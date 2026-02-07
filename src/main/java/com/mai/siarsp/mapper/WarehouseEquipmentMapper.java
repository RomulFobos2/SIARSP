package com.mai.siarsp.mapper;

import com.mai.siarsp.dto.WarehouseEquipmentDTO;
import com.mai.siarsp.models.WarehouseEquipment;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;
import org.mapstruct.factory.Mappers;

import java.time.LocalDate;
import java.util.List;

@Mapper
public interface WarehouseEquipmentMapper {
    WarehouseEquipmentMapper INSTANCE = Mappers.getMapper(WarehouseEquipmentMapper.class);

    @Mapping(source = "warehouse.id", target = "warehouseId")
    @Mapping(source = "warehouse.name", target = "warehouseName")
    @Mapping(source = "equipment", target = "expirationDate", qualifiedByName = "expirationDate")
    @Mapping(source = "equipment", target = "expired", qualifiedByName = "isExpired")
    WarehouseEquipmentDTO toDTO(WarehouseEquipment equipment);

    List<WarehouseEquipmentDTO> toDTOList(List<WarehouseEquipment> equipments);

    @Named("expirationDate")
    default LocalDate getExpirationDate(WarehouseEquipment equipment) {
        return equipment.getExpirationDate();
    }

    @Named("isExpired")
    default boolean isExpired(WarehouseEquipment equipment) {
        return equipment.isExpired();
    }
}
