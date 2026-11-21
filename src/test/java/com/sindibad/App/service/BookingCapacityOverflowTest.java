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

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BookingCapacityOverflowTest {

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
    void requestThatExceedsRemainingCapacityIsRejected() {
        reject(8L, 10, 3);
    }

    @Test
    void requestWhoseIntAdditionWouldOverflowIsRejected() {
        reject(1L, 10, Integer.MAX_VALUE);
    }

    @Test
    void requestThatExactlyFillsRemainingCapacityIsAccepted() {
        Trip trip = bookableTrip(10);
        when(tripRepository.findById(10L)).thenReturn(Optional.of(trip));
        when(touristRepository.findById(7L)).thenReturn(Optional.of(tourist()));
        when(bookingRepository.sumGuestsByTripId(10L)).thenReturn(8L);
        when(bookingRepository.save(any(Booking.class))).thenAnswer(invocation -> invocation.getArgument(0));
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
        order.verify(bookingRepository).save(any(Booking.class));
    }

    private void reject(long occupiedGuests, int maximum, int requestedGuests) {
        Trip trip = bookableTrip(maximum);
        when(tripRepository.findById(10L)).thenReturn(Optional.of(trip));
        when(touristRepository.findById(7L)).thenReturn(Optional.of(tourist()));
        when(bookingRepository.sumGuestsByTripId(10L)).thenReturn(occupiedGuests);

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> bookingService.createBooking(
                        7L,
                        new BookingRequestDTO(10L, null, 7L, requestedGuests),
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

    private Trip bookableTrip(int maximum) {
        Trip trip = new Trip();
        trip.setTripId(10L);
        trip.setTripStatus("Upcoming");
        trip.setStartDate(LocalDateTime.now().plusDays(2));
        trip.setMaxParticipants(maximum);
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
