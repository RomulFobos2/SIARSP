package com.mai.siarsp.mapper;

import com.mai.siarsp.dto.ClientOrderDTO;
import com.mai.siarsp.models.ClientOrder;
import com.mai.siarsp.models.Employee;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;
import org.mapstruct.factory.Mappers;

import java.util.List;

@Mapper(uses = {OrderedProductMapper.class})
public interface ClientOrderMapper {
    ClientOrderMapper INSTANCE = Mappers.getMapper(ClientOrderMapper.class);

    @Mapping(source = "client.id", target = "clientId")
    @Mapping(source = "client.organizationName", target = "clientOrganizationName")
    @Mapping(source = "responsibleEmployee.id", target = "responsibleEmployeeId")
    @Mapping(source = "responsibleEmployee", target = "responsibleEmployeeFullName", qualifiedByName = "employeeFullName")
    @Mapping(source = "deliveryTask.id", target = "deliveryTaskId")
    @Mapping(source = "acceptanceAct.id", target = "acceptanceActId")
    @Mapping(source = "status.displayName", target = "statusDisplayName")
    ClientOrderDTO toDTO(ClientOrder clientOrder);

    List<ClientOrderDTO> toDTOList(List<ClientOrder> clientOrders);

    @Named("employeeFullName")
    default String getEmployeeFullName(Employee employee) {
        return employee.getFullName();
    }
}
