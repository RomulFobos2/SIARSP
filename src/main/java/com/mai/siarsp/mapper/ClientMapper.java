package com.mai.siarsp.mapper;

import com.mai.siarsp.dto.ClientDTO;
import com.mai.siarsp.models.Client;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;
import org.mapstruct.factory.Mappers;

import java.util.List;

@Mapper(uses = {ClientOrderMapper.class})
public interface ClientMapper {
    ClientMapper INSTANCE = Mappers.getMapper(ClientMapper.class);

    @Mapping(source = "client", target = "displayName", qualifiedByName = "displayName")
    ClientDTO toDTO(Client client);

    List<ClientDTO> toDTOList(List<Client> clients);

    @Named("displayName")
    default String getDisplayName(Client client) {
        return client.getDisplayName();
    }
}
