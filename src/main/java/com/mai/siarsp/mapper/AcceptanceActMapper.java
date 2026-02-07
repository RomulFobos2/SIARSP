package com.mai.siarsp.mapper;

import com.mai.siarsp.dto.AcceptanceActDTO;
import com.mai.siarsp.models.AcceptanceAct;
import com.mai.siarsp.models.Employee;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;
import org.mapstruct.factory.Mappers;

import java.util.List;

@Mapper
public interface AcceptanceActMapper {
    AcceptanceActMapper INSTANCE = Mappers.getMapper(AcceptanceActMapper.class);

    @Mapping(source = "clientOrder.id", target = "clientOrderId")
    @Mapping(source = "clientOrder.orderNumber", target = "clientOrderNumber")
    @Mapping(source = "client.id", target = "clientId")
    @Mapping(source = "client.organizationName", target = "clientOrganizationName")
    @Mapping(source = "deliveredBy.id", target = "deliveredById")
    @Mapping(source = "deliveredBy", target = "deliveredByFullName", qualifiedByName = "employeeFullName")
    AcceptanceActDTO toDTO(AcceptanceAct acceptanceAct);

    List<AcceptanceActDTO> toDTOList(List<AcceptanceAct> acceptanceActs);

    @Named("employeeFullName")
    default String getEmployeeFullName(Employee employee) {
        return employee.getFullName();
    }
}
