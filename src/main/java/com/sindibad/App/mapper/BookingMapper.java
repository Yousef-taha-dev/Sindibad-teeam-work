package com.sindibad.App.mapper;

import com.sindibad.App.dto.BookingRequestDTO;
import com.sindibad.App.dto.BookingResponseDTO;
import com.sindibad.App.entity.Booking;
import com.sindibad.App.entity.Guide;
import com.sindibad.App.entity.Person;
import com.sindibad.App.entity.Trip;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;
import org.mapstruct.NullValueCheckStrategy;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Mapper(
        componentModel = "spring",
        nullValueCheckStrategy = NullValueCheckStrategy.ALWAYS
)
public interface BookingMapper {

    @Mapping(target = "tripId", source = "trip.tripId")
    @Mapping(target = "tripTitle", source = "trip.title")
    @Mapping(target = "guideId", source = "trip.guide.guideId")
    @Mapping(target = "guideName", source = "booking", qualifiedByName = "tripGuideFullName")
    @Mapping(target = "touristId", source = "tourist.touristId")
    @Mapping(target = "touristName", source = "booking", qualifiedByName = "getTouristFullName")
    @Mapping(target = "bookingType", ignore = true)
    @Mapping(target = "totalPrice", source = "booking", qualifiedByName = "totalPrice")
    BookingResponseDTO toDto(Booking booking);

    @Mapping(target = "bookingId", ignore = true)
    @Mapping(target = "bookingDate", ignore = true)
    @Mapping(target = "bookingStatus", ignore = true)
    @Mapping(target = "trip", ignore = true)
    @Mapping(target = "guide", ignore = true)
    @Mapping(target = "tourist", ignore = true)
    Booking toEntity(BookingRequestDTO dto);

    @Named("tripGuideFullName")
    default String tripGuideFullName(Booking booking) {
        Guide guide = tripGuide(booking);
        if (guide == null || guide.getPerson() == null) {
            return null;
        }

        Person person = guide.getPerson();
        return person.getFirstName() + " " + person.getLastName();
    }

    @Named("totalPrice")
    default BigDecimal totalPrice(Booking booking) {
        Trip trip = booking == null ? null : booking.getTrip();
        if (trip == null
                || trip.getPricePerPerson() == null
                || booking.getNumberOfGuests() == null) {
            return null;
        }

        return trip.getPricePerPerson()
                .multiply(BigDecimal.valueOf(booking.getNumberOfGuests()))
                .setScale(2, RoundingMode.HALF_UP);
    }

    private static Guide tripGuide(Booking booking) {
        if (booking == null || booking.getTrip() == null) {
            return null;
        }
        return booking.getTrip().getGuide();
    }

    @Named("getTouristFullName")
    default String getTouristFullName(Booking booking) {
        if (booking.getTourist() != null && booking.getTourist().getPerson() != null) {
            return booking.getTourist().getPerson().getFirstName() + " " + booking.getTourist().getPerson().getLastName();
        }
        return null;
    }
}