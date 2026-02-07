package com.mai.siarsp.mapper;

import com.mai.siarsp.dto.WarehouseDTO;
import com.mai.siarsp.models.Warehouse;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

import java.util.List;

@Mapper(uses = {ShelfMapper.class})
public interface WarehouseMapper {
    WarehouseMapper INSTANCE = Mappers.getMapper(WarehouseMapper.class);

    WarehouseDTO toDTO(Warehouse warehouse);

    List<WarehouseDTO> toDTOList(List<Warehouse> warehouses);
}
