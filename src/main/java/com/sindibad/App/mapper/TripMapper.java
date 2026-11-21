package com.sindibad.App.mapper;

import com.sindibad.App.dto.TripRequestDTO;
import com.sindibad.App.dto.TripResponseDTO;
import com.sindibad.App.entity.Trip;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

@Mapper(componentModel = "spring")
public interface TripMapper {

    @Mapping(target = "guideId", source = "guide.guideId")
    @Mapping(target = "guideName", source = "trip", qualifiedByName = "getTripGuideFullName")
    @Mapping(target = "categoryId", source = "category.categoryId")
    @Mapping(target = "categoryName", source = "category.categoryName")
    TripResponseDTO toDto(Trip trip);

    @Mapping(target = "tripId", ignore = true)
    @Mapping(target = "tripStatus", ignore = true)
    @Mapping(target = "guide", ignore = true)
    @Mapping(target = "category", ignore = true)
    @Mapping(target = "tripImageUrl", ignore = true)
    @Mapping(target = "tripImagePublicId", ignore = true)
    Trip toEntity(TripRequestDTO dto);

    @Named("getTripGuideFullName")
    default String getTripGuideFullName(Trip trip) {
        if (trip.getGuide() != null && trip.getGuide().getPerson() != null) {
            return trip.getGuide().getPerson().getFirstName() + " " + trip.getGuide().getPerson().getLastName();
        }
        return null;
    }
}