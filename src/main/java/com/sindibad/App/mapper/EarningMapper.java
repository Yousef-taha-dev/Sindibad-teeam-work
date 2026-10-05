package com.sindibad.App.mapper;

import com.sindibad.App.dto.EarningResponseDTO;
import com.sindibad.App.entity.Earning;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface EarningMapper {

    @Mapping(target = "guideId", source = "guide.guideId")
    @Mapping(
            target = "guideName",
            expression = "java(earning.getGuide().getPerson().getFirstName() + \" \" + earning.getGuide().getPerson().getLastName())"
    )
    @Mapping(target = "paymentId", source = "payment.paymentId")
    EarningResponseDTO toDto(Earning earning);
}