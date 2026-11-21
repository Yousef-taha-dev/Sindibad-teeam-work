package com.sindibad.App.service;

import com.sindibad.App.dto.BookingResponseDTO;
import com.sindibad.App.entity.Booking;
import com.sindibad.App.entity.Guide;
import com.sindibad.App.entity.Person;
import com.sindibad.App.entity.Tourist;
import com.sindibad.App.entity.Trip;
import com.sindibad.App.mapper.BookingMapper;
import com.sindibad.App.repository.BookingRepository;
import com.sindibad.App.repository.GuideRepository;
import jakarta.persistence.EntityManager;
import com.sindibad.App.repository.PaymentRepository;
import com.sindibad.App.repository.PersonRepository;
import com.sindibad.App.repository.TouristRepository;
import com.sindibad.App.repository.TripRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GuideBookingReadOwnershipTest {

    @Mock
    private BookingRepository bookingRepository;
    @Mock
    private TripRepository tripRepository;
    @Mock
    private TouristRepository touristRepository;
    @Mock
    private PersonRepository personRepository;
    @Mock
    private BookingMapper bookingMapper;
    @Mock
    private PaymentRepository paymentRepository;
    @Mock
    private GuideRepository guideRepository;
    @Mock
    private EntityManager entityManager;

    private BookingServiceImpl bookingService;

    @BeforeEach
    void setUp() {
        bookingService = new BookingServiceImpl(
                bookingRepository,
                tripRepository,
                touristRepository,
                personRepository,
                bookingMapper,
                paymentRepository,
                guideRepository,
                entityManager
        );
    }

    @Test
    void guideCanReadBookingAssignedThroughTheTrip() {
        Guide assigned = approvedGuide(9L, 5L);
        Booking booking = bookingForTripGuide(assigned, tourist(8L));
        booking.setGuide(approvedGuide(4L, 6L));
        BookingResponseDTO response = response();
        when(bookingRepository.findById(42L)).thenReturn(Optional.of(booking));
        when(personRepository.findByEmail("guide@example.com"))
                .thenReturn(Optional.of(assigned.getPerson()));
        when(guideRepository.findByPersonPersonId(5L)).thenReturn(Optional.of(assigned));
        when(bookingMapper.toDto(booking)).thenReturn(response);

        BookingResponseDTO result = bookingService.getBookingById(
                42L,
                "guide@example.com",
                false,
                true
        );

        assertSame(response, result);
        verify(bookingMapper).toDto(booking);
    }

    @Test
    void guideCannotReadBookingAssignedToAnotherGuide() {
        Guide actor = approvedGuide(9L, 5L);
        Booking booking = bookingForTripGuide(approvedGuide(4L, 6L), tourist(8L));
        booking.setGuide(actor);
        when(bookingRepository.findById(42L)).thenReturn(Optional.of(booking));
        when(personRepository.findByEmail("guide@example.com"))
                .thenReturn(Optional.of(actor.getPerson()));
        when(guideRepository.findByPersonPersonId(5L)).thenReturn(Optional.of(actor));

        assertThrows(
                AccessDeniedException.class,
                () -> bookingService.getBookingById(
                        42L,
                        "guide@example.com",
                        false,
                        true
                )
        );

        verify(bookingMapper, never()).toDto(booking);
    }

    @Test
    void guideWithoutAGuideAccountIsDenied() {
        Person person = activePerson(5L);
        Booking booking = bookingForTripGuide(approvedGuide(9L, 5L), tourist(8L));
        when(bookingRepository.findById(42L)).thenReturn(Optional.of(booking));
        when(personRepository.findByEmail("guide@example.com"))
                .thenReturn(Optional.of(person));
        when(guideRepository.findByPersonPersonId(5L)).thenReturn(Optional.empty());

        assertThrows(
                AccessDeniedException.class,
                () -> bookingService.getBookingById(
                        42L,
                        "guide@example.com",
                        false,
                        true
                )
        );

        verify(bookingMapper, never()).toDto(booking);
    }

    @Test
    void touristCanReadOwnBooking() {
        Booking booking = bookingForTripGuide(approvedGuide(9L, 5L), tourist(7L));
        BookingResponseDTO response = response();
        when(bookingRepository.findById(42L)).thenReturn(Optional.of(booking));
        when(personRepository.findByEmail("lina@example.com"))
                .thenReturn(Optional.of(activePerson(3L)));
        when(touristRepository.findByPersonPersonId(3L))
                .thenReturn(Optional.of(tourist(7L)));
        when(bookingMapper.toDto(booking)).thenReturn(response);

        BookingResponseDTO result = bookingService.getBookingById(
                42L,
                "lina@example.com",
                false,
                false
        );

        assertSame(response, result);
    }

    @Test
    void touristCannotReadAnotherTouristsBooking() {
        Booking booking = bookingForTripGuide(approvedGuide(9L, 5L), tourist(8L));
        when(bookingRepository.findById(42L)).thenReturn(Optional.of(booking));
        when(personRepository.findByEmail("lina@example.com"))
                .thenReturn(Optional.of(activePerson(3L)));
        when(touristRepository.findByPersonPersonId(3L))
                .thenReturn(Optional.of(tourist(7L)));

        assertThrows(
                AccessDeniedException.class,
                () -> bookingService.getBookingById(
                        42L,
                        "lina@example.com",
                        false,
                        false
                )
        );

        verify(bookingMapper, never()).toDto(booking);
    }

    @Test
    void adminCanReadAnyBooking() {
        Booking booking = bookingForTripGuide(approvedGuide(9L, 5L), tourist(8L));
        BookingResponseDTO response = response();
        when(bookingRepository.findById(42L)).thenReturn(Optional.of(booking));
        when(bookingMapper.toDto(booking)).thenReturn(response);

        BookingResponseDTO result = bookingService.getBookingById(
                42L,
                "admin@example.com",
                true,
                false
        );

        assertSame(response, result);
        verify(personRepository, never()).findByEmail("admin@example.com");
        verify(guideRepository, never()).findByPersonPersonId(5L);
    }

    private Person activePerson(Long personId) {
        Person person = new Person();
        person.setPersonId(personId);
        person.setEmail("guide@example.com");
        person.setAccountStatus("Active");
        return person;
    }

    private Guide approvedGuide(Long guideId, Long personId) {
        Guide guide = new Guide();
        guide.setGuideId(guideId);
        guide.setPerson(activePerson(personId));
        guide.setApprovalStatus("Approved");
        return guide;
    }

    private Tourist tourist(Long touristId) {
        Tourist tourist = new Tourist();
        tourist.setTouristId(touristId);
        return tourist;
    }

    private Booking bookingForTripGuide(Guide guide, Tourist tourist) {
        Trip trip = new Trip();
        trip.setTripId(10L);
        trip.setGuide(guide);

        Booking booking = new Booking();
        booking.setBookingId(42L);
        booking.setTrip(trip);
        booking.setTourist(tourist);
        return booking;
    }

    private BookingResponseDTO response() {
        return new BookingResponseDTO(
                42L,
                null,
                10L,
                "Petra by night",
                9L,
                "Guide Name",
                8L,
                "Lina Haddad",
                null,
                2,
                null,
                "Pending"
        );
    }
}
