package com.sindibad.App.mapper;

import com.sindibad.App.dto.NotificationResponseDTO;
import com.sindibad.App.entity.Notifications;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring")
public interface NotificationMapper {

    @Mapping(source = "person.personId", target = "personId")
    NotificationResponseDTO toDto(Notifications notification);

    List<NotificationResponseDTO> toDtoList(List<Notifications> notifications);
}