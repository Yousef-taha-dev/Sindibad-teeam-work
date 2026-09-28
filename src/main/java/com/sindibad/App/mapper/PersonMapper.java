package com.sindibad.App.mapper;

import com.sindibad.App.dto.PersonResponseDTO;
import com.sindibad.App.dto.RegisterRequestDTO;
import com.sindibad.App.entity.Person;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface PersonMapper {

    PersonResponseDTO toDto(Person person);

    @Mapping(target = "personId", ignore = true)
    @Mapping(target = "profileImage", ignore = true)
    @Mapping(target = "accountStatus", ignore = true)
    Person toEntity(RegisterRequestDTO dto);
}