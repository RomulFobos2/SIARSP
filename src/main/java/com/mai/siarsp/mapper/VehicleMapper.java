package com.mai.siarsp.mapper;

import com.mai.siarsp.dto.VehicleDTO;
import com.mai.siarsp.models.Vehicle;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;
import org.mapstruct.factory.Mappers;

import java.util.List;

@Mapper
public interface VehicleMapper {
    VehicleMapper INSTANCE = Mappers.getMapper(VehicleMapper.class);

    @Mapping(source = "vehicle", target = "fullName", qualifiedByName = "vehicleFullName")
    @Mapping(source = "vehicle", target = "available", qualifiedByName = "vehicleAvailable")
    VehicleDTO toDTO(Vehicle vehicle);

    List<VehicleDTO> toDTOList(List<Vehicle> vehicles);

    @Named("vehicleFullName")
    default String getFullName(Vehicle vehicle) {
        return vehicle.getFullName();
    }

    @Named("vehicleAvailable")
    default boolean isAvailable(Vehicle vehicle) {
        return vehicle.isAvailable();
    }
}
