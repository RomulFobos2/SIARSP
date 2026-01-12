package com.mai.siarsp.mapper;

import com.mai.siarsp.dto.VisitorDTO;
import com.mai.siarsp.models.Visitor;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;
import org.mapstruct.factory.Mappers;

import java.util.List;

@Mapper
public interface VisitorMapper {
    VisitorMapper INSTANCE = Mappers.getMapper(VisitorMapper.class);

    @Mapping(source = "id", target = "id")
    @Mapping(source = "lastName", target = "lastName")
    @Mapping(source = "firstName", target = "firstName")
    @Mapping(source = "patronymicName", target = "patronymicName")
    @Mapping(source = "sex", target = "sex")
    @Mapping(source = "username", target = "username")
    @Mapping(source = "role.name", target = "roleName")
    @Mapping(source = "role.description", target = "roleDescription")
    @Mapping(source = "mobileNumber", target = "mobileNumber")
    @Mapping(source = "dateRegistration", target = "dateRegistration")
    @Mapping(source = "dateBirthday", target = "dateBirthday")
    @Mapping(source = "needChangePass", target = "needChangePass")
    @Mapping(source = "visitor", target = "fullName", qualifiedByName = "fullNameMapper")
    VisitorDTO toDTO(Visitor visitor);

    List<VisitorDTO> toDTOList(List<Visitor> visitors);

    @Named("fullNameMapper")
    default String getFullName(Visitor visitor) {
        return visitor.getLastName() + " " + visitor.getFirstName() +
                (visitor.getPatronymicName() != null ? " " + visitor.getPatronymicName() : "");
    }
}
