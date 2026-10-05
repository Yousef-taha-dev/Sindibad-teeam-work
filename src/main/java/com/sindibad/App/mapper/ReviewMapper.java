package com.sindibad.App.mapper;

import com.sindibad.App.dto.ReviewResponseDTO;
import com.sindibad.App.entity.Review;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface ReviewMapper {

    @Mapping(target = "bookingId", source = "booking.bookingId")
    @Mapping(
            target = "touristName",
            expression = "java(review.getBooking().getTourist().getPerson().getFirstName() + \" \" + review.getBooking().getTourist().getPerson().getLastName())"
    )
    ReviewResponseDTO toDto(Review review);
}