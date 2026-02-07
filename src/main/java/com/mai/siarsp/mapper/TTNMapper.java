package com.mai.siarsp.mapper;

import com.mai.siarsp.dto.TTNDTO;
import com.mai.siarsp.models.Employee;
import com.mai.siarsp.models.TTN;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;
import org.mapstruct.factory.Mappers;

import java.util.List;

@Mapper
public interface TTNMapper {
    TTNMapper INSTANCE = Mappers.getMapper(TTNMapper.class);

    @Mapping(source = "deliveryTask.id", target = "deliveryTaskId")
    @Mapping(source = "vehicle.id", target = "vehicleId")
    @Mapping(source = "vehicle.registrationNumber", target = "vehicleRegistrationNumber")
    @Mapping(source = "driver.id", target = "driverId")
    @Mapping(source = "driver", target = "driverFullName", qualifiedByName = "employeeFullName")
    TTNDTO toDTO(TTN ttn);

    List<TTNDTO> toDTOList(List<TTN> ttns);

    @Named("employeeFullName")
    default String getEmployeeFullName(Employee employee) {
        return employee.getFullName();
    }
}
