package com.sindibad.App.service;

import com.sindibad.App.dto.BookingRequestDTO;
import com.sindibad.App.dto.BookingResponseDTO;
import com.sindibad.App.entity.Booking;
import com.sindibad.App.entity.Guide;
import com.sindibad.App.entity.Person;
import com.sindibad.App.entity.Tourist;
import com.sindibad.App.entity.Trip;
import com.sindibad.App.mapper.BookingMapper;
import com.sindibad.App.repository.BookingRepository;
import com.sindibad.App.repository.GuideRepository;
import com.sindibad.App.repository.PaymentRepository;
import com.sindibad.App.repository.PersonRepository;
import com.sindibad.App.repository.TouristRepository;
import com.sindibad.App.repository.TripRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BookingCapacityLockTest {

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
    void successfulBookingLocksTheTripBeforeCountingGuests() {
        Trip trip = bookableTrip(12);
        Tourist tourist = tourist();
        when(tripRepository.findById(10L)).thenReturn(Optional.of(trip));
        when(touristRepository.findById(7L)).thenReturn(Optional.of(tourist));
        when(bookingRepository.sumGuestsByTripId(10L)).thenReturn(4L);
        when(bookingRepository.save(any(Booking.class))).thenAnswer(invocation -> {
            Booking booking = invocation.getArgument(0);
            booking.setBookingId(20L);
            return booking;
        });
        when(bookingMapper.toDto(any(Booking.class))).thenReturn(response());

        bookingService.createBooking(
                7L,
                new BookingRequestDTO(10L, null, 7L, 2),
                "admin@example.com",
                true
        );

        InOrder order = inOrder(entityManager, bookingRepository);
        order.verify(entityManager).refresh(trip, LockModeType.PESSIMISTIC_WRITE);
        order.verify(bookingRepository).sumGuestsByTripId(10L);
        verify(bookingRepository).save(any(Booking.class));
    }

    @Test
    void capacityRejectionStillPreventsSave() {
        Trip trip = bookableTrip(12);
        Tourist tourist = tourist();
        when(tripRepository.findById(10L)).thenReturn(Optional.of(trip));
        when(touristRepository.findById(7L)).thenReturn(Optional.of(tourist));
        when(bookingRepository.sumGuestsByTripId(10L)).thenReturn(11L);

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> bookingService.createBooking(
                        7L,
                        new BookingRequestDTO(10L, null, 7L, 2),
                        "admin@example.com",
                        true
                )
        );

        assertEquals(
                "Requested seats exceed maximum available capacity for this trip.",
                exception.getMessage()
        );
        InOrder order = inOrder(entityManager, bookingRepository);
        order.verify(entityManager).refresh(trip, LockModeType.PESSIMISTIC_WRITE);
        order.verify(bookingRepository).sumGuestsByTripId(10L);
        verify(bookingRepository, never()).save(any());
    }

    @Test
    void capacityCheckUsesTheRefreshedMaximum() {
        Trip trip = bookableTrip(12);
        Tourist tourist = tourist();
        when(tripRepository.findById(10L)).thenReturn(Optional.of(trip));
        when(touristRepository.findById(7L)).thenReturn(Optional.of(tourist));
        doAnswer(invocation -> {
            Trip locked = invocation.getArgument(0);
            locked.setMaxParticipants(3);
            return null;
        }).when(entityManager).refresh(trip, LockModeType.PESSIMISTIC_WRITE);
        when(bookingRepository.sumGuestsByTripId(10L)).thenReturn(2L);

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> bookingService.createBooking(
                        7L,
                        new BookingRequestDTO(10L, null, 7L, 2),
                        "admin@example.com",
                        true
                )
        );

        assertEquals(
                "Requested seats exceed maximum available capacity for this trip.",
                exception.getMessage()
        );
        verify(bookingRepository, never()).save(any());
    }

    @Test
    void tripThatLosesUpcomingStatusDuringRefreshIsRejected() {
        Trip trip = bookableTrip(12);
        Tourist tourist = tourist();
        when(tripRepository.findById(10L)).thenReturn(Optional.of(trip));
        when(touristRepository.findById(7L)).thenReturn(Optional.of(tourist));
        doAnswer(invocation -> {
            Trip locked = invocation.getArgument(0);
            locked.setTripStatus("Completed");
            return null;
        }).when(entityManager).refresh(trip, LockModeType.PESSIMISTIC_WRITE);

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> bookingService.createBooking(
                        7L,
                        new BookingRequestDTO(10L, null, 7L, 2),
                        "admin@example.com",
                        true
                )
        );

        assertEquals("This trip is not available for booking.", exception.getMessage());
        verify(bookingRepository, never()).sumGuestsByTripId(any());
        verify(bookingRepository, never()).save(any());
    }

    @Test
    void tripWhoseStartBecomesReachedDuringRefreshIsRejected() {
        rejectWhenRefreshSetsStart(LocalDateTime.of(2026, 6, 1, 12, 0));
    }

    @Test
    void tripWhoseStartBecomesPastDuringRefreshIsRejected() {
        LocalDateTime now = LocalDateTime.of(2026, 6, 1, 12, 0);
        rejectWhenRefreshSetsStart(now.minusMinutes(1));
    }

    private void rejectWhenRefreshSetsStart(LocalDateTime refreshedStart) {
        LocalDateTime now = LocalDateTime.of(2026, 6, 1, 12, 0);
        Clock clock = Clock.fixed(now.toInstant(ZoneOffset.UTC), ZoneOffset.UTC);
        BookingServiceImpl clockedService = new BookingServiceImpl(
                bookingRepository,
                tripRepository,
                touristRepository,
                personRepository,
                bookingMapper,
                paymentRepository,
                guideRepository,
                entityManager,
                clock
        );
        Trip trip = bookableTrip(12);
        trip.setStartDate(now.plusDays(1));
        when(tripRepository.findById(10L)).thenReturn(Optional.of(trip));
        when(touristRepository.findById(7L)).thenReturn(Optional.of(tourist()));
        doAnswer(invocation -> {
            Trip locked = invocation.getArgument(0);
            locked.setStartDate(refreshedStart);
            return null;
        }).when(entityManager).refresh(trip, LockModeType.PESSIMISTIC_WRITE);

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> clockedService.createBooking(
                        7L,
                        new BookingRequestDTO(10L, null, 7L, 2),
                        "admin@example.com",
                        true
                )
        );

        assertEquals("This trip has already started.", exception.getMessage());
        verify(bookingRepository, never()).sumGuestsByTripId(any());
        verify(bookingRepository, never()).save(any());
    }

    private Trip bookableTrip(int maxParticipants) {
        Trip trip = new Trip();
        trip.setTripId(10L);
        trip.setTripStatus("Upcoming");
        trip.setStartDate(LocalDateTime.now().plusDays(2));
        trip.setMaxParticipants(maxParticipants);
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

    private Tourist tourist() {
        Tourist tourist = new Tourist();
        tourist.setTouristId(7L);
        return tourist;
    }

    private BookingResponseDTO response() {
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
                2,
                null,
                "Pending"
        );
    }
}
