package com.mai.siarsp.mapper;

import com.mai.siarsp.dto.SupplierDTO;
import com.mai.siarsp.models.Supplier;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;
import org.mapstruct.factory.Mappers;

import java.util.List;

@Mapper(uses = {DeliveryMapper.class})
public interface SupplierMapper {
    SupplierMapper INSTANCE = Mappers.getMapper(SupplierMapper.class);

    @Mapping(source = "supplier", target = "fullName", qualifiedByName = "supplierFullName")
    @Mapping(source = "supplier", target = "directorShortName", qualifiedByName = "directorShortName")
    SupplierDTO toDTO(Supplier supplier);

    List<SupplierDTO> toDTOList(List<Supplier> suppliers);

    @Named("supplierFullName")
    default String getFullName(Supplier supplier) {
        return supplier.getFullName();
    }

    @Named("directorShortName")
    default String getDirectorShortName(Supplier supplier) {
        return supplier.getDirectorShortName();
    }
}
