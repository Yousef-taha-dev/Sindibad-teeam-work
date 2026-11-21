package com.sindibad.App.service;

import com.sindibad.App.dto.BookingResponseDTO;
import com.sindibad.App.entity.Booking;
import com.sindibad.App.entity.Person;
import com.sindibad.App.entity.Tourist;
import com.sindibad.App.exception.ResourceNotFoundException;
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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BookingReadOwnershipTest {

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
    void touristCanReadOwnBooking() {
        Booking booking = bookingOwnedBy(7L);
        BookingResponseDTO response = response();
        when(bookingRepository.findById(42L)).thenReturn(Optional.of(booking));
        when(personRepository.findByEmail("lina@example.com"))
                .thenReturn(Optional.of(person(3L)));
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
        verify(bookingMapper).toDto(booking);
    }

    @Test
    void touristCannotReadAnotherTouristsBooking() {
        Booking booking = bookingOwnedBy(8L);
        when(bookingRepository.findById(42L)).thenReturn(Optional.of(booking));
        when(personRepository.findByEmail("lina@example.com"))
                .thenReturn(Optional.of(person(3L)));
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
        Booking booking = bookingOwnedBy(8L);
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
        verify(bookingMapper).toDto(booking);
        verify(personRepository, never()).findByEmail("admin@example.com");
        verify(touristRepository, never()).findByPersonPersonId(3L);
    }

    @Test
    void missingBookingIsRejected() {
        when(bookingRepository.findById(42L)).thenReturn(Optional.empty());

        ResourceNotFoundException exception = assertThrows(
                ResourceNotFoundException.class,
                () -> bookingService.getBookingById(
                        42L,
                        "lina@example.com",
                        false,
                        false
                )
        );

        assertEquals("Booking not found with ID: 42", exception.getMessage());
        verify(bookingMapper, never()).toDto(org.mockito.ArgumentMatchers.any());
    }

    private Booking bookingOwnedBy(Long touristId) {
        Booking booking = new Booking();
        booking.setBookingId(42L);
        booking.setTourist(tourist(touristId));
        return booking;
    }

    private Person person(Long personId) {
        Person person = new Person();
        person.setPersonId(personId);
        person.setEmail("lina@example.com");
        return person;
    }

    private Tourist tourist(Long touristId) {
        Tourist tourist = new Tourist();
        tourist.setTouristId(touristId);
        return tourist;
    }

    private BookingResponseDTO response() {
        return new BookingResponseDTO(
                42L,
                null,
                null,
                null,
                null,
                null,
                7L,
                "Lina Haddad",
                null,
                2,
                null,
                "Pending"
        );
    }
}
