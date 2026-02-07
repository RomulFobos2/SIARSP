package com.mai.siarsp.mapper;

import com.mai.siarsp.dto.ShelfDTO;
import com.mai.siarsp.models.Shelf;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;

import java.util.List;

@Mapper(uses = {StorageZoneMapper.class})
public interface ShelfMapper {
    ShelfMapper INSTANCE = Mappers.getMapper(ShelfMapper.class);

    @Mapping(source = "warehouse.id", target = "warehouseId")
    @Mapping(source = "warehouse.name", target = "warehouseName")
    ShelfDTO toDTO(Shelf shelf);

    List<ShelfDTO> toDTOList(List<Shelf> shelves);
}
