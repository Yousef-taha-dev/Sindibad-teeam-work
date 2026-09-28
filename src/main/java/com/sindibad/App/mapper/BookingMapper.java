package com.sindibad.App.mapper;

import com.sindibad.App.dto.BookingRequestDTO;
import com.sindibad.App.dto.BookingResponseDTO;
import com.sindibad.App.entity.Booking;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

@Mapper(componentModel = "spring")
public interface BookingMapper {

    @Mapping(target = "tripId", source = "trip.tripId")
    @Mapping(target = "tripTitle", source = "trip.title")
    @Mapping(target = "guideId", source = "guide.guideId")
    @Mapping(target = "guideName", source = "booking", qualifiedByName = "getGuideFullName")
    @Mapping(target = "touristId", source = "tourist.touristId")
    @Mapping(target = "touristName", source = "booking", qualifiedByName = "getTouristFullName")
    @Mapping(target = "bookingType", ignore = true)
    @Mapping(target = "totalPrice", ignore = true)
    BookingResponseDTO toDto(Booking booking);

    @Mapping(target = "bookingId", ignore = true)
    @Mapping(target = "bookingDate", ignore = true)
    @Mapping(target = "bookingStatus", ignore = true)
    @Mapping(target = "trip", ignore = true)
    @Mapping(target = "guide", ignore = true)
    @Mapping(target = "tourist", ignore = true)
    Booking toEntity(BookingRequestDTO dto);

    @Named("getGuideFullName")
    default String getGuideFullName(Booking booking) {
        if (booking.getGuide() != null && booking.getGuide().getPerson() != null) {
            return booking.getGuide().getPerson().getFirstName() + " " + booking.getGuide().getPerson().getLastName();
        }
        return null;
    }

    @Named("getTouristFullName")
    default String getTouristFullName(Booking booking) {
        if (booking.getTourist() != null && booking.getTourist().getPerson() != null) {
            return booking.getTourist().getPerson().getFirstName() + " " + booking.getTourist().getPerson().getLastName();
        }
        return null;
    }
}