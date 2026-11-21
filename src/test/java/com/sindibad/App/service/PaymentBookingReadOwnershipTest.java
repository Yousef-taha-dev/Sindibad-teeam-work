package com.sindibad.App.service;

import com.sindibad.App.dto.PaymentResponseDTO;
import com.sindibad.App.entity.Booking;
import com.sindibad.App.entity.Payment;
import com.sindibad.App.entity.Person;
import com.sindibad.App.entity.Tourist;
import com.sindibad.App.exception.ResourceNotFoundException;
import com.sindibad.App.repository.BookingRepository;
import com.sindibad.App.repository.EarningRepository;
import com.sindibad.App.repository.PaymentRepository;
import com.sindibad.App.repository.PersonRepository;
import com.sindibad.App.repository.TouristRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PaymentBookingReadOwnershipTest {

    @Mock
    private PaymentRepository paymentRepository;
    @Mock
    private BookingRepository bookingRepository;
    @Mock
    private EarningRepository earningRepository;
    @Mock
    private PersonRepository personRepository;
    @Mock
    private TouristRepository touristRepository;

    private PaymentServiceImpl paymentService;

    @BeforeEach
    void setUp() {
        paymentService = new PaymentServiceImpl(
                paymentRepository,
                bookingRepository,
                earningRepository,
                personRepository,
                touristRepository
        );
    }

    @Test
    void touristCanReadPaymentForOwnBooking() {
        Payment payment = payment(42L, tourist(7L, 3L));

        when(paymentRepository.findByBookingBookingId(42L))
                .thenReturn(Optional.of(payment));
        when(personRepository.findByEmail("lina@example.com"))
                .thenReturn(Optional.of(person(3L)));
        when(touristRepository.findByPersonPersonId(3L))
                .thenReturn(Optional.of(tourist(7L, 3L)));

        PaymentResponseDTO response = paymentService.getPaymentByBookingId(
                42L,
                "lina@example.com",
                false
        );

        assertEquals(15L, response.paymentId());
        assertEquals(42L, response.bookingId());
        assertEquals(new BigDecimal("20.00"), response.amount());
        assertEquals("Cash", response.paymentMethod());
        assertEquals("Completed", response.paymentStatus());
        assertEquals(payment.getPaidAt(), response.paidAt());
        verify(paymentRepository).findByBookingBookingId(42L);
    }

    @Test
    void touristCannotReadAnotherTouristsBookingPayment() {
        when(paymentRepository.findByBookingBookingId(42L))
                .thenReturn(Optional.of(payment(42L, tourist(8L, 4L))));
        when(personRepository.findByEmail("lina@example.com"))
                .thenReturn(Optional.of(person(3L)));
        when(touristRepository.findByPersonPersonId(3L))
                .thenReturn(Optional.of(tourist(7L, 3L)));

        assertThrows(
                AccessDeniedException.class,
                () -> paymentService.getPaymentByBookingId(
                        42L,
                        "lina@example.com",
                        false
                )
        );
    }

    @Test
    void adminCanReadAnotherTouristsBookingPayment() {
        Payment payment = payment(42L, tourist(8L, 4L));
        when(paymentRepository.findByBookingBookingId(42L))
                .thenReturn(Optional.of(payment));

        PaymentResponseDTO response = paymentService.getPaymentByBookingId(
                42L,
                "admin@example.com",
                true
        );

        assertEquals(15L, response.paymentId());
        assertEquals(42L, response.bookingId());
        assertEquals(new BigDecimal("20.00"), response.amount());
        verify(personRepository, never()).findByEmail(any());
        verify(touristRepository, never()).findByPersonPersonId(any());
    }

    @Test
    void missingPaymentIsRejected() {
        when(paymentRepository.findByBookingBookingId(99L))
                .thenReturn(Optional.empty());

        ResourceNotFoundException exception = assertThrows(
                ResourceNotFoundException.class,
                () -> paymentService.getPaymentByBookingId(
                        99L,
                        "lina@example.com",
                        false
                )
        );

        assertEquals(
                "Payment not found for Booking ID: 99",
                exception.getMessage()
        );
        verify(personRepository, never()).findByEmail(any());
        verify(touristRepository, never()).findByPersonPersonId(any());
    }

    @Test
    void blankEmailIsRejected() {
        when(paymentRepository.findByBookingBookingId(42L))
                .thenReturn(Optional.of(payment(42L, tourist(7L, 3L))));

        assertThrows(
                AccessDeniedException.class,
                () -> paymentService.getPaymentByBookingId(42L, null, false)
        );
        assertThrows(
                AccessDeniedException.class,
                () -> paymentService.getPaymentByBookingId(42L, "  ", false)
        );

        verify(personRepository, never()).findByEmail(any());
    }

    @Test
    void missingPersonIsRejected() {
        when(paymentRepository.findByBookingBookingId(42L))
                .thenReturn(Optional.of(payment(42L, tourist(7L, 3L))));
        when(personRepository.findByEmail("lina@example.com"))
                .thenReturn(Optional.empty());

        assertThrows(
                AccessDeniedException.class,
                () -> paymentService.getPaymentByBookingId(
                        42L,
                        "lina@example.com",
                        false
                )
        );

        verify(touristRepository, never()).findByPersonPersonId(any());
    }

    @Test
    void missingTouristIsRejected() {
        when(paymentRepository.findByBookingBookingId(42L))
                .thenReturn(Optional.of(payment(42L, tourist(7L, 3L))));
        when(personRepository.findByEmail("lina@example.com"))
                .thenReturn(Optional.of(person(3L)));
        when(touristRepository.findByPersonPersonId(3L))
                .thenReturn(Optional.empty());

        assertThrows(
                AccessDeniedException.class,
                () -> paymentService.getPaymentByBookingId(
                        42L,
                        "lina@example.com",
                        false
                )
        );
    }

    @Test
    void paymentWithoutTouristIsRejected() {
        Payment payment = payment(42L, tourist(7L, 3L));
        payment.getBooking().setTourist(null);

        when(paymentRepository.findByBookingBookingId(42L))
                .thenReturn(Optional.of(payment));
        when(personRepository.findByEmail("lina@example.com"))
                .thenReturn(Optional.of(person(3L)));
        when(touristRepository.findByPersonPersonId(3L))
                .thenReturn(Optional.of(tourist(7L, 3L)));

        assertThrows(
                AccessDeniedException.class,
                () -> paymentService.getPaymentByBookingId(
                        42L,
                        "lina@example.com",
                        false
                )
        );
    }

    private Person person(Long personId) {
        Person person = new Person();
        person.setPersonId(personId);
        person.setEmail("lina@example.com");
        return person;
    }

    private Tourist tourist(Long touristId, Long personId) {
        Tourist tourist = new Tourist();
        tourist.setTouristId(touristId);
        tourist.setPerson(person(personId));
        return tourist;
    }

    private Payment payment(Long bookingId, Tourist tourist) {
        Booking booking = new Booking();
        booking.setBookingId(bookingId);
        booking.setTourist(tourist);

        Payment payment = new Payment();
        payment.setPaymentId(15L);
        payment.setBooking(booking);
        payment.setAmount(new BigDecimal("20.00"));
        payment.setPaymentMethod("Cash");
        payment.setPaymentStatus("Completed");
        payment.setPaidAt(LocalDateTime.of(2026, 11, 19, 1, 48));
        return payment;
    }
}
