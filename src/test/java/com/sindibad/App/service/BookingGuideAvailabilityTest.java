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
class BookingGuideAvailabilityTest {

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
    void approvedGuideWithActiveAccountProceedsThroughExistingChecks() {
        Trip trip = bookableTrip(guide("Approved", "Active"));
        when(tripRepository.findById(10L)).thenReturn(Optional.of(trip));
        when(touristRepository.findById(7L)).thenReturn(Optional.of(tourist()));
        when(bookingRepository.sumGuestsByTripId(10L)).thenReturn(2L);
        when(bookingRepository.save(any(Booking.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
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

    @Test
    void rejectedGuideIsRejectedBeforeGuestCount() {
        reject(
                guide("Rejected", "Active"),
                "This trip cannot be booked because its guide is not approved."
        );
    }

    @Test
    void pendingGuideIsRejectedBeforeGuestCount() {
        reject(
                guide("Pending", "Active"),
                "This trip cannot be booked because its guide is not approved."
        );
    }

    @Test
    void inactiveGuidePersonIsRejectedBeforeGuestCount() {
        reject(
                guide("Approved", "Inactive"),
                "This trip cannot be booked because the guide account is not active."
        );
    }

    @Test
    void missingGuideIsRejectedBeforeGuestCount() {
        reject(
                null,
                "This trip cannot be booked because it has no assigned guide."
        );
    }

    @Test
    void missingGuidePersonIsRejectedBeforeGuestCount() {
        Guide guide = new Guide();
        guide.setGuideId(4L);
        guide.setApprovalStatus("Approved");
        reject(
                guide,
                "This trip cannot be booked because its guide has no account."
        );
    }

    private void reject(Guide guide, String message) {
        Trip trip = bookableTrip(guide);
        when(tripRepository.findById(10L)).thenReturn(Optional.of(trip));
        when(touristRepository.findById(7L)).thenReturn(Optional.of(tourist()));

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> bookingService.createBooking(
                        7L,
                        new BookingRequestDTO(10L, null, 7L, 2),
                        "admin@example.com",
                        true
                )
        );

        assertEquals(message, exception.getMessage());
        verify(entityManager).refresh(trip, LockModeType.PESSIMISTIC_WRITE);
        verify(bookingRepository, never()).sumGuestsByTripId(any());
        verify(bookingRepository, never()).save(any());
    }

    private Trip bookableTrip(Guide guide) {
        Trip trip = new Trip();
        trip.setTripId(10L);
        trip.setTripStatus("Upcoming");
        trip.setStartDate(LocalDateTime.now().plusDays(2));
        trip.setMaxParticipants(10);
        trip.setGuide(guide);
        return trip;
    }

    private Guide guide(String approvalStatus, String accountStatus) {
        Person person = new Person();
        person.setPersonId(6L);
        person.setAccountStatus(accountStatus);

        Guide guide = new Guide();
        guide.setGuideId(4L);
        guide.setPerson(person);
        guide.setApprovalStatus(approvalStatus);
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
