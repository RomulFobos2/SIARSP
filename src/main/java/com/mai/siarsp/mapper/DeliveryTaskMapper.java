package com.mai.siarsp.mapper;

import com.mai.siarsp.dto.DeliveryTaskDTO;
import com.mai.siarsp.models.DeliveryTask;
import com.mai.siarsp.models.Employee;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;
import org.mapstruct.factory.Mappers;

import java.util.List;

@Mapper(uses = {RoutePointMapper.class})
public interface DeliveryTaskMapper {
    DeliveryTaskMapper INSTANCE = Mappers.getMapper(DeliveryTaskMapper.class);

    @Mapping(source = "clientOrder.id", target = "clientOrderId")
    @Mapping(source = "clientOrder.orderNumber", target = "clientOrderNumber")
    @Mapping(source = "driver.id", target = "driverId")
    @Mapping(source = "driver", target = "driverFullName", qualifiedByName = "employeeFullName")
    @Mapping(source = "vehicle.id", target = "vehicleId")
    @Mapping(source = "vehicle.registrationNumber", target = "vehicleRegistrationNumber")
    @Mapping(source = "deliveryTask", target = "totalMileage", qualifiedByName = "totalMileage")
    @Mapping(source = "ttn.id", target = "ttnId")
    DeliveryTaskDTO toDTO(DeliveryTask deliveryTask);

    List<DeliveryTaskDTO> toDTOList(List<DeliveryTask> deliveryTasks);

    @Named("employeeFullName")
    default String getEmployeeFullName(Employee employee) {
        return employee.getFullName();
    }

    @Named("totalMileage")
    default Integer getTotalMileage(DeliveryTask deliveryTask) {
        return deliveryTask.getTotalMileage();
    }
}
