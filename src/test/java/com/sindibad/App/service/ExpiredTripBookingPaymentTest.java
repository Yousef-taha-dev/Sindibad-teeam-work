package com.sindibad.App.service;

import com.sindibad.App.dto.BookingRequestDTO;
import com.sindibad.App.dto.BookingResponseDTO;
import com.sindibad.App.dto.PaymentRequestDTO;
import com.sindibad.App.dto.PaymentResponseDTO;
import com.sindibad.App.entity.Booking;
import com.sindibad.App.entity.Guide;
import com.sindibad.App.entity.Payment;
import com.sindibad.App.entity.Person;
import com.sindibad.App.entity.Tourist;
import com.sindibad.App.entity.Trip;
import com.sindibad.App.mapper.BookingMapper;
import com.sindibad.App.repository.BookingRepository;
import com.sindibad.App.repository.GuideRepository;
import jakarta.persistence.EntityManager;
import com.sindibad.App.repository.EarningRepository;
import com.sindibad.App.repository.PaymentRepository;
import com.sindibad.App.repository.PersonRepository;
import com.sindibad.App.repository.TouristRepository;
import com.sindibad.App.repository.TripRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ExpiredTripBookingPaymentTest {

    private static final LocalDateTime NOW =
            LocalDateTime.of(2026, 6, 1, 12, 0);
    private static final Clock CLOCK =
            Clock.fixed(NOW.toInstant(ZoneOffset.UTC), ZoneOffset.UTC);

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
    @Mock
    private EarningRepository earningRepository;

    private BookingServiceImpl bookingService;
    private PaymentServiceImpl paymentService;

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
                entityManager,
                CLOCK
        );
        paymentService = new PaymentServiceImpl(
                paymentRepository,
                bookingRepository,
                earningRepository,
                personRepository,
                touristRepository,
                CLOCK
        );
    }

    @Test
    void bookingIsRejectedWhenStartIsInThePast() {
        when(tripRepository.findById(10L))
                .thenReturn(Optional.of(trip(NOW.minusMinutes(1))));

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> bookingService.createBooking(
                        7L,
                        new BookingRequestDTO(10L, null, 7L, 1),
                        "admin@example.com",
                        true
                )
        );

        assertEquals("This trip has already started.", exception.getMessage());
        verify(bookingRepository, never()).save(any());
    }

    @Test
    void bookingIsRejectedWhenStartEqualsNow() {
        when(tripRepository.findById(10L)).thenReturn(Optional.of(trip(NOW)));

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> bookingService.createBooking(
                        7L,
                        new BookingRequestDTO(10L, null, 7L, 1),
                        "admin@example.com",
                        true
                )
        );

        assertEquals("This trip has already started.", exception.getMessage());
        verify(touristRepository, never()).findById(any());
        verify(bookingRepository, never()).save(any());
    }

    @Test
    void bookingSucceedsWhenStartIsInTheFuture() {
        Trip trip = trip(NOW.plusMinutes(1));
        Tourist tourist = new Tourist();
        tourist.setTouristId(7L);
        when(tripRepository.findById(10L)).thenReturn(Optional.of(trip));
        when(touristRepository.findById(7L)).thenReturn(Optional.of(tourist));
        when(bookingRepository.sumGuestsByTripId(10L)).thenReturn(0L);
        when(bookingRepository.save(any(Booking.class))).thenAnswer(invocation -> {
            Booking booking = invocation.getArgument(0);
            booking.setBookingId(20L);
            return booking;
        });
        when(bookingMapper.toDto(any(Booking.class))).thenReturn(bookingResponse());

        BookingResponseDTO response = bookingService.createBooking(
                7L,
                new BookingRequestDTO(10L, null, 99L, 1),
                "admin@example.com",
                true
        );

        ArgumentCaptor<Booking> saved = ArgumentCaptor.forClass(Booking.class);
        verify(bookingRepository).save(saved.capture());
        assertSame(tourist, saved.getValue().getTourist());
        assertEquals("Pending", saved.getValue().getBookingStatus());
        assertEquals(20L, response.bookingId());
    }

    @Test
    void paymentIsRejectedWhenTripHasAlreadyStarted() {
        Booking booking = pendingBooking(NOW.minusDays(1));
        when(bookingRepository.findById(42L)).thenReturn(Optional.of(booking));
        when(paymentRepository.existsByBookingBookingId(42L)).thenReturn(false);

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> paymentService.processPayment(
                        new PaymentRequestDTO(42L, new BigDecimal("1.00"), "Cash"),
                        "admin@example.com",
                        true
                )
        );

        assertEquals("This trip has already started.", exception.getMessage());
        assertEquals("Pending", booking.getBookingStatus());
        verify(paymentRepository, never()).save(any());
        verify(bookingRepository, never()).save(any());
        verify(earningRepository, never()).save(any());
    }

    @Test
    void paymentIsRejectedWhenStartEqualsNow() {
        Booking booking = pendingBooking(NOW);
        when(bookingRepository.findById(42L)).thenReturn(Optional.of(booking));
        when(paymentRepository.existsByBookingBookingId(42L)).thenReturn(false);

        assertThrows(
                IllegalArgumentException.class,
                () -> paymentService.processPayment(
                        new PaymentRequestDTO(42L, new BigDecimal("1.00"), "Cash"),
                        "admin@example.com",
                        true
                )
        );

        assertEquals("Pending", booking.getBookingStatus());
        verify(paymentRepository, never()).save(any());
        verify(bookingRepository, never()).save(any());
    }

    @Test
    void paymentSucceedsWhenStartIsInTheFuture() {
        Booking booking = pendingBooking(NOW.plusHours(1));
        when(bookingRepository.findById(42L)).thenReturn(Optional.of(booking));
        when(paymentRepository.existsByBookingBookingId(42L)).thenReturn(false);
        when(paymentRepository.save(any(Payment.class))).thenAnswer(invocation -> {
            Payment payment = invocation.getArgument(0);
            payment.setPaymentId(100L);
            return payment;
        });

        PaymentResponseDTO response = paymentService.processPayment(
                new PaymentRequestDTO(42L, new BigDecimal("1.00"), "Cash"),
                "admin@example.com",
                true
        );

        assertEquals(new BigDecimal("20.00"), response.amount());
        assertEquals("Completed", response.paymentStatus());
        assertEquals("Confirmed", booking.getBookingStatus());
        verify(paymentRepository).save(any(Payment.class));
        verify(bookingRepository).save(booking);
        verify(earningRepository).save(any());
    }

    private Trip trip(LocalDateTime start) {
        Trip trip = new Trip();
        trip.setTripId(10L);
        trip.setTripStatus("Upcoming");
        trip.setStartDate(start);
        trip.setMaxParticipants(4);
        trip.setGuide(approvedActiveGuide());
        return trip;
    }

    private Guide approvedActiveGuide() {
        Person person = new Person();
        person.setPersonId(6L);
        person.setAccountStatus("Active");

        Guide guide = new Guide();
        guide.setGuideId(4L);
        guide.setPerson(person);
        guide.setApprovalStatus("Approved");
        return guide;
    }

    private Booking pendingBooking(LocalDateTime start) {
        Person person = new Person();
        person.setPersonId(6L);
        person.setAccountStatus("Active");

        Guide guide = new Guide();
        guide.setGuideId(9L);
        guide.setPerson(person);
        guide.setApprovalStatus("Approved");

        Trip trip = trip(start);
        trip.setPricePerPerson(new BigDecimal("10.00"));
        trip.setGuide(guide);

        Tourist tourist = new Tourist();
        tourist.setTouristId(8L);

        Booking booking = new Booking();
        booking.setBookingId(42L);
        booking.setTrip(trip);
        booking.setTourist(tourist);
        booking.setNumberOfGuests(2);
        booking.setBookingStatus("Pending");
        return booking;
    }

    private BookingResponseDTO bookingResponse() {
        return new BookingResponseDTO(
                20L,
                null,
                10L,
                "Petra by night",
                null,
                null,
                7L,
                null,
                null,
                1,
                null,
                "Pending"
        );
    }
}
