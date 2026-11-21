package com.sindibad.App.service;

import com.sindibad.App.entity.Booking;
import com.sindibad.App.entity.Person;
import com.sindibad.App.entity.Tourist;
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
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BookingPaidCancellationTest {

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
    void pendingBookingWithNoPaymentCanBeCancelled() {
        Booking booking = bookingOwnedBy(7L, "Pending");
        when(bookingRepository.findById(42L)).thenReturn(Optional.of(booking));
        when(personRepository.findByEmail("lina@example.com"))
                .thenReturn(Optional.of(person(3L)));
        when(touristRepository.findByPersonPersonId(3L))
                .thenReturn(Optional.of(tourist(7L)));
        when(paymentRepository.existsByBookingBookingId(42L)).thenReturn(false);

        bookingService.cancelBooking(42L, "lina@example.com", false);

        assertEquals("Cancelled", booking.getBookingStatus());
        verify(bookingRepository).save(booking);
    }

    @Test
    void confirmedBookingCannotBeCancelled() {
        Booking booking = bookingOwnedBy(7L, "Confirmed");
        when(bookingRepository.findById(42L)).thenReturn(Optional.of(booking));
        when(personRepository.findByEmail("lina@example.com"))
                .thenReturn(Optional.of(person(3L)));
        when(touristRepository.findByPersonPersonId(3L))
                .thenReturn(Optional.of(tourist(7L)));

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> bookingService.cancelBooking(42L, "lina@example.com", false)
        );

        assertEquals(
                "This booking has already been confirmed.",
                exception.getMessage()
        );
        assertEquals("Confirmed", booking.getBookingStatus());
        verify(bookingRepository, never()).save(any());
        verify(paymentRepository, never()).existsByBookingBookingId(any());
    }

    @Test
    void bookingWithExistingPaymentCannotBeCancelled() {
        Booking booking = bookingOwnedBy(7L, "Pending");
        when(bookingRepository.findById(42L)).thenReturn(Optional.of(booking));
        when(personRepository.findByEmail("lina@example.com"))
                .thenReturn(Optional.of(person(3L)));
        when(touristRepository.findByPersonPersonId(3L))
                .thenReturn(Optional.of(tourist(7L)));
        when(paymentRepository.existsByBookingBookingId(42L)).thenReturn(true);

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> bookingService.cancelBooking(42L, "lina@example.com", false)
        );

        assertEquals(
                "Cannot cancel a booking that has already been paid.",
                exception.getMessage()
        );
        assertEquals("Pending", booking.getBookingStatus());
        verify(bookingRepository, never()).save(any());
    }

    @Test
    void alreadyCancelledBookingIsRejected() {
        Booking booking = bookingOwnedBy(7L, "Cancelled");
        when(bookingRepository.findById(42L)).thenReturn(Optional.of(booking));
        when(personRepository.findByEmail("lina@example.com"))
                .thenReturn(Optional.of(person(3L)));
        when(touristRepository.findByPersonPersonId(3L))
                .thenReturn(Optional.of(tourist(7L)));

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> bookingService.cancelBooking(42L, "lina@example.com", false)
        );

        assertEquals("Booking is already cancelled.", exception.getMessage());
        assertEquals("Cancelled", booking.getBookingStatus());
        verify(bookingRepository, never()).save(any());
        verify(paymentRepository, never()).existsByBookingBookingId(any());
    }

    @Test
    void touristCannotCancelAnotherTouristsPendingBooking() {
        Booking booking = bookingOwnedBy(8L, "Pending");
        when(bookingRepository.findById(42L)).thenReturn(Optional.of(booking));
        when(personRepository.findByEmail("lina@example.com"))
                .thenReturn(Optional.of(person(3L)));
        when(touristRepository.findByPersonPersonId(3L))
                .thenReturn(Optional.of(tourist(7L)));

        assertThrows(
                AccessDeniedException.class,
                () -> bookingService.cancelBooking(42L, "lina@example.com", false)
        );

        assertEquals("Pending", booking.getBookingStatus());
        verify(bookingRepository, never()).save(any());
        verify(paymentRepository, never()).existsByBookingBookingId(any());
    }

    @Test
    void adminCanCancelAnotherTouristsPendingBooking() {
        Booking booking = bookingOwnedBy(8L, "Pending");
        when(bookingRepository.findById(42L)).thenReturn(Optional.of(booking));
        when(paymentRepository.existsByBookingBookingId(42L)).thenReturn(false);

        bookingService.cancelBooking(42L, "admin@example.com", true);

        assertEquals("Cancelled", booking.getBookingStatus());
        verify(bookingRepository).save(booking);
        verify(personRepository, never()).findByEmail(any());
        verify(touristRepository, never()).findByPersonPersonId(any());
    }

    @Test
    void adminCannotCancelConfirmedBooking() {
        Booking booking = bookingOwnedBy(8L, "Confirmed");
        when(bookingRepository.findById(42L)).thenReturn(Optional.of(booking));

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> bookingService.cancelBooking(42L, "admin@example.com", true)
        );

        assertEquals(
                "This booking has already been confirmed.",
                exception.getMessage()
        );
        assertEquals("Confirmed", booking.getBookingStatus());
        verify(bookingRepository, never()).save(any());
        verify(personRepository, never()).findByEmail(any());
    }

    private Booking bookingOwnedBy(Long touristId, String status) {
        Booking booking = new Booking();
        booking.setBookingId(42L);
        booking.setTourist(tourist(touristId));
        booking.setBookingStatus(status);
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
}
