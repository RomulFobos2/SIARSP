package com.mai.siarsp.mapper;

import com.mai.siarsp.dto.StorageZoneDTO;
import com.mai.siarsp.models.StorageZone;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;
import org.mapstruct.factory.Mappers;

import java.util.List;

@Mapper(uses = {ZoneProductMapper.class})
public interface StorageZoneMapper {
    StorageZoneMapper INSTANCE = Mappers.getMapper(StorageZoneMapper.class);

    @Mapping(source = "shelf.id", target = "shelfId")
    @Mapping(source = "shelf.code", target = "shelfCode")
    @Mapping(source = "storageZone", target = "capacityVolume", qualifiedByName = "capacityVolume")
    @Mapping(source = "storageZone", target = "occupancyPercentage", qualifiedByName = "occupancyPercentage")
    StorageZoneDTO toDTO(StorageZone storageZone);

    List<StorageZoneDTO> toDTOList(List<StorageZone> storageZones);

    @Named("capacityVolume")
    default double getCapacityVolume(StorageZone storageZone) {
        return storageZone.getCapacityVolume();
    }

    @Named("occupancyPercentage")
    default double getOccupancyPercentage(StorageZone storageZone) {
        return storageZone.getOccupancyPercentage();
    }
}
