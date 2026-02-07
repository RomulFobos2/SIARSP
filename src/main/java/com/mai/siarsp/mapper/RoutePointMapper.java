package com.mai.siarsp.mapper;

import com.mai.siarsp.dto.RoutePointDTO;
import com.mai.siarsp.models.RoutePoint;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;
import org.mapstruct.factory.Mappers;

import java.util.List;

@Mapper
public interface RoutePointMapper {
    RoutePointMapper INSTANCE = Mappers.getMapper(RoutePointMapper.class);

    @Mapping(source = "deliveryTask.id", target = "deliveryTaskId")
    @Mapping(source = "routePoint", target = "coordinates", qualifiedByName = "coordinates")
    RoutePointDTO toDTO(RoutePoint routePoint);

    List<RoutePointDTO> toDTOList(List<RoutePoint> routePoints);

    @Named("coordinates")
    default String getCoordinates(RoutePoint routePoint) {
        return routePoint.getCoordinates();
    }
}
