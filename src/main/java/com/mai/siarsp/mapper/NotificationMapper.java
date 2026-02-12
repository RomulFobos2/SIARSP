package com.mai.siarsp.mapper;

import com.mai.siarsp.dto.NotificationDTO;
import com.mai.siarsp.models.Notification;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;

import java.util.List;

@Mapper
public interface NotificationMapper {
    NotificationMapper INSTANCE = Mappers.getMapper(NotificationMapper.class);

    @Mapping(source = "recipient.id", target = "recipientId")
    @Mapping(target = "status", expression = "java(notification.getStatus().getDisplayName())")
    NotificationDTO toDTO(Notification notification);

    List<NotificationDTO> toDTOList(List<Notification> notifications);
}
