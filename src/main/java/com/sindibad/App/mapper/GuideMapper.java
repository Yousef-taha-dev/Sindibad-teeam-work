package com.sindibad.App.mapper;

import com.sindibad.App.dto.PendingGuideDTO;
import com.sindibad.App.entity.Guide;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface GuideMapper {

    @Mapping(target = "guideId", expression = "java(guide.getGuideId() != null ? guide.getGuideId().intValue() : null)")
    @Mapping(target = "personId", expression = "java(guide.getPerson() != null && guide.getPerson().getPersonId() != null ? guide.getPerson().getPersonId().intValue() : null)")
    @Mapping(target = "firstName", source = "person.firstName")
    @Mapping(target = "lastName", source = "person.lastName")
    @Mapping(target = "email", source = "person.email")
    @Mapping(target = "phoneNumber", source = "person.phoneNumber")
    @Mapping(target = "availableDays", ignore = true)
    @Mapping(target = "startTime", ignore = true)
    @Mapping(target = "endTime", ignore = true)
    PendingGuideDTO toPendingGuideDto(Guide guide);
}