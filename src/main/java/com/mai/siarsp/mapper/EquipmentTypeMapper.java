package com.mai.siarsp.mapper;

import com.mai.siarsp.dto.EquipmentTypeDTO;
import com.mai.siarsp.models.EquipmentType;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

import java.util.List;

@Mapper
public interface EquipmentTypeMapper {
    EquipmentTypeMapper INSTANCE = Mappers.getMapper(EquipmentTypeMapper.class);

    EquipmentTypeDTO toDTO(EquipmentType equipmentType);

    List<EquipmentTypeDTO> toDTOList(List<EquipmentType> equipmentTypes);
}
