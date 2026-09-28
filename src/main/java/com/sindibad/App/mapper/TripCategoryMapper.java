package com.sindibad.App.mapper;

import com.sindibad.App.dto.TripCategoryDTO;
import com.sindibad.App.entity.TripCategory;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface TripCategoryMapper {

    @Mapping(target = "categoryId", expression = "java((int) category.getCategoryId())")
    TripCategoryDTO toDto(TripCategory category);

    @Mapping(target = "categoryId", expression = "java(dto.categoryId() != null ? dto.categoryId().longValue() : 0L)")
    TripCategory toEntity(TripCategoryDTO dto);
}