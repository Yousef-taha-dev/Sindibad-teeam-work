package com.sindibad.App.mapper;

import com.sindibad.App.dto.BookingResponseDTO;
import com.sindibad.App.entity.Booking;
import com.sindibad.App.entity.Guide;
import com.sindibad.App.entity.Person;
import com.sindibad.App.entity.Tourist;
import com.sindibad.App.entity.Trip;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class BookingMapperTest {

    private final BookingMapper bookingMapper = Mappers.getMapper(BookingMapper.class);

    @Test
    void tripGuideAndPriceAreReturnedWhenBookingGuideIsNull() {
        Booking booking = bookingWithTripGuide(4L, "Lina", "Haddad", new BigDecimal("10.00"), 2);
        booking.setGuide(null);

        BookingResponseDTO response = bookingMapper.toDto(booking);

        assertEquals(4L, response.guideId());
        assertEquals("Lina Haddad", response.guideName());
        assertEquals(new BigDecimal("20.00"), response.totalPrice());
        assertNull(response.bookingType());
    }

    @Test
    void totalPriceUsesScaleTwoAndHalfUp() {
        Booking roundsUp = bookingWithTripGuide(4L, "Lina", "Haddad", new BigDecimal("10.005"), 1);
        Booking staysDown = bookingWithTripGuide(4L, "Lina", "Haddad", new BigDecimal("10.004"), 1);

        assertEquals(new BigDecimal("10.01"), bookingMapper.toDto(roundsUp).totalPrice());
        assertEquals(new BigDecimal("10.00"), bookingMapper.toDto(staysDown).totalPrice());
    }

    @Test
    void missingGuideReturnsNullGuideFields() {
        Booking booking = bookingWithTripGuide(4L, "Lina", "Haddad", new BigDecimal("10.00"), 2);
        booking.getTrip().setGuide(null);

        BookingResponseDTO response = bookingMapper.toDto(booking);

        assertNull(response.guideId());
        assertNull(response.guideName());
        assertEquals(new BigDecimal("20.00"), response.totalPrice());
    }

    @Test
    void missingGuidePersonPreservesGuideId() {
        Booking booking = bookingWithTripGuide(4L, "Lina", "Haddad", new BigDecimal("10.00"), 2);
        booking.getTrip().getGuide().setPerson(null);

        BookingResponseDTO response = bookingMapper.toDto(booking);

        assertEquals(4L, response.guideId());
        assertNull(response.guideName());
    }

    @Test
    void missingTripPriceOrGuestCountReturnsNullTotalPrice() {
        Booking missingTrip = bookingWithTripGuide(4L, "Lina", "Haddad", new BigDecimal("10.00"), 2);
        missingTrip.setTrip(null);

        Booking missingPrice = bookingWithTripGuide(4L, "Lina", "Haddad", new BigDecimal("10.00"), 2);
        missingPrice.getTrip().setPricePerPerson(null);

        Booking missingGuests = bookingWithTripGuide(4L, "Lina", "Haddad", new BigDecimal("10.00"), 2);
        missingGuests.setNumberOfGuests(null);

        assertNull(bookingMapper.toDto(missingTrip).totalPrice());
        assertNull(bookingMapper.toDto(missingTrip).guideId());
        assertNull(bookingMapper.toDto(missingTrip).guideName());
        assertNull(bookingMapper.toDto(missingPrice).totalPrice());
        assertNull(bookingMapper.toDto(missingGuests).totalPrice());
    }

    private Booking bookingWithTripGuide(
            Long guideId,
            String firstName,
            String lastName,
            BigDecimal pricePerPerson,
            Integer guests) {

        Person person = new Person();
        person.setFirstName(firstName);
        person.setLastName(lastName);

        Guide guide = new Guide();
        guide.setGuideId(guideId);
        guide.setPerson(person);

        Trip trip = new Trip();
        trip.setTripId(10L);
        trip.setTitle("Petra by night");
        trip.setGuide(guide);
        trip.setPricePerPerson(pricePerPerson);

        Tourist tourist = new Tourist();
        tourist.setTouristId(7L);
        tourist.setPerson(person);

        Booking booking = new Booking();
        booking.setBookingId(20L);
        booking.setTrip(trip);
        booking.setTourist(tourist);
        booking.setNumberOfGuests(guests);
        booking.setBookingStatus("Pending");
        return booking;
    }
}
