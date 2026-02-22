package com.mai.siarsp.mapper;

import com.mai.siarsp.dto.WriteOffActDTO;
import com.mai.siarsp.models.Employee;
import com.mai.siarsp.models.WriteOffAct;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;
import org.mapstruct.factory.Mappers;

import java.util.List;

@Mapper
public interface WriteOffActMapper {
    WriteOffActMapper INSTANCE = Mappers.getMapper(WriteOffActMapper.class);

    @Mapping(source = "product.id", target = "productId")
    @Mapping(source = "product.name", target = "productName")
    @Mapping(source = "product.article", target = "productArticle")
    @Mapping(source = "responsibleEmployee.id", target = "responsibleEmployeeId")
    @Mapping(source = "responsibleEmployee", target = "responsibleEmployeeFullName", qualifiedByName = "employeeFullName")
    @Mapping(source = "status.displayName", target = "statusDisplayName")
    @Mapping(source = "warehouse.id", target = "warehouseId")
    @Mapping(source = "warehouse.name", target = "warehouseName")
    WriteOffActDTO toDTO(WriteOffAct writeOffAct);

    List<WriteOffActDTO> toDTOList(List<WriteOffAct> writeOffActs);

    @Named("employeeFullName")
    default String getEmployeeFullName(Employee employee) {
        return employee.getFullName();
    }
}
