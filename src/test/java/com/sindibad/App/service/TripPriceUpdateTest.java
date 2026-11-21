package com.sindibad.App.service;

import com.sindibad.App.dto.TripRequestDTO;
import com.sindibad.App.entity.Guide;
import com.sindibad.App.entity.Person;
import com.sindibad.App.entity.Trip;
import com.sindibad.App.entity.TripCategory;
import com.sindibad.App.mapper.BookingMapper;
import com.sindibad.App.mapper.GuideMapper;
import com.sindibad.App.mapper.TripMapperImpl;
import com.sindibad.App.repository.BookingRepository;
import com.sindibad.App.repository.EarningRepository;
import com.sindibad.App.repository.GuideRepository;
import com.sindibad.App.repository.PersonRepository;
import com.sindibad.App.repository.TouristRepository;
import com.sindibad.App.repository.TripCategoryRepository;
import com.sindibad.App.repository.TripRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
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
class TripPriceUpdateTest {

    @Mock
    private PersonRepository personRepository;
    @Mock
    private GuideRepository guideRepository;
    @Mock
    private TouristRepository touristRepository;
    @Mock
    private TripRepository tripRepository;
    @Mock
    private TripCategoryRepository tripCategoryRepository;
    @Mock
    private BookingRepository bookingRepository;
    @Mock
    private EarningRepository earningRepository;
    @Mock
    private GuideMapper guideMapper;
    @Mock
    private BookingMapper bookingMapper;
    @Mock
    private EntityManager entityManager;

    private AdminServiceImpl adminService;

    @BeforeEach
    void setUp() {
        adminService = new AdminServiceImpl(
                personRepository,
                guideRepository,
                touristRepository,
                tripRepository,
                tripCategoryRepository,
                bookingRepository,
                earningRepository,
                new TripMapperImpl(),
                guideMapper,
                bookingMapper,
                null,
                entityManager
        );
    }

    @Test
    void changedPriceWithPendingBookingIsRejectedBeforeSave() {
        Trip trip = existingTrip();
        when(tripRepository.findById(10L)).thenReturn(Optional.of(trip));
        when(bookingRepository.sumGuestsByTripId(10L)).thenReturn(2L);
        when(bookingRepository.existsByTripTripIdAndBookingStatus(10L, "Pending"))
                .thenReturn(true);

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> adminService.updateTrip(10L, request(new BigDecimal("40.00"), 12))
        );

        assertEquals(
                "The trip price cannot be changed while pending bookings exist.",
                exception.getMessage()
        );
        assertEquals("Old title", trip.getTitle());
        assertEquals(new BigDecimal("25.00"), trip.getPricePerPerson());
        assertEquals(12, trip.getMaxParticipants());
        InOrder order = inOrder(entityManager, bookingRepository);
        order.verify(entityManager).refresh(trip, LockModeType.PESSIMISTIC_WRITE);
        order.verify(bookingRepository).sumGuestsByTripId(10L);
        order.verify(bookingRepository).existsByTripTripIdAndBookingStatus(10L, "Pending");
        verify(tripRepository, never()).save(any());
    }

    @Test
    void numericallyEqualPriceIsSavedWhenPendingBookingExists() {
        Trip trip = existingTrip();
        when(tripRepository.findById(10L)).thenReturn(Optional.of(trip));
        when(bookingRepository.sumGuestsByTripId(10L)).thenReturn(2L);
        when(tripRepository.save(any(Trip.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        adminService.updateTrip(10L, request(new BigDecimal("25.0"), 12));

        ArgumentCaptor<Trip> savedTrip = ArgumentCaptor.forClass(Trip.class);
        verify(tripRepository).save(savedTrip.capture());
        assertEquals(0, new BigDecimal("25.00").compareTo(savedTrip.getValue().getPricePerPerson()));
        assertEquals("Petra by night", savedTrip.getValue().getTitle());
        verify(bookingRepository, never())
                .existsByTripTripIdAndBookingStatus(any(), any());
    }

    @Test
    void changedPriceIsSavedWhenNoPendingBookingExists() {
        Trip trip = existingTrip();
        when(tripRepository.findById(10L)).thenReturn(Optional.of(trip));
        when(bookingRepository.sumGuestsByTripId(10L)).thenReturn(0L);
        when(bookingRepository.existsByTripTripIdAndBookingStatus(10L, "Pending"))
                .thenReturn(false);
        when(tripRepository.save(any(Trip.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        adminService.updateTrip(10L, request(new BigDecimal("40.00"), 12));

        ArgumentCaptor<Trip> savedTrip = ArgumentCaptor.forClass(Trip.class);
        verify(tripRepository).save(savedTrip.capture());
        assertEquals(new BigDecimal("40.00"), savedTrip.getValue().getPricePerPerson());
        assertEquals("Petra by night", savedTrip.getValue().getTitle());
    }

    @Test
    void capacityRejectionStillPreventsSaveBeforeThePriceCheck() {
        Trip trip = existingTrip();
        when(tripRepository.findById(10L)).thenReturn(Optional.of(trip));
        when(bookingRepository.sumGuestsByTripId(10L)).thenReturn(8L);

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> adminService.updateTrip(10L, request(new BigDecimal("40.00"), 5))
        );

        assertEquals(
                "Maximum participants cannot be lower than the number of guests already booked.",
                exception.getMessage()
        );
        assertEquals("Old title", trip.getTitle());
        assertEquals(new BigDecimal("25.00"), trip.getPricePerPerson());
        InOrder order = inOrder(entityManager, bookingRepository);
        order.verify(entityManager).refresh(trip, LockModeType.PESSIMISTIC_WRITE);
        order.verify(bookingRepository).sumGuestsByTripId(10L);
        verify(bookingRepository, never())
                .existsByTripTripIdAndBookingStatus(any(), any());
        verify(tripRepository, never()).save(any());
    }

    private TripRequestDTO request(BigDecimal price, int maxParticipants) {
        return new TripRequestDTO(
                4L,
                2L,
                "Petra by night",
                "A longer guided walk",
                LocalDateTime.of(2026, 7, 1, 9, 0),
                LocalDateTime.of(2026, 7, 2, 18, 0),
                maxParticipants,
                price,
                "https://evil.example/replacement.png"
        );
    }

    private Trip existingTrip() {
        Trip trip = new Trip();
        trip.setTripId(10L);
        trip.setGuide(guide());
        trip.setCategory(category());
        trip.setTitle("Old title");
        trip.setDescription("Old description");
        trip.setStartDate(LocalDateTime.of(2026, 7, 1, 9, 0));
        trip.setEndDate(LocalDateTime.of(2026, 7, 2, 18, 0));
        trip.setMaxParticipants(12);
        trip.setPricePerPerson(new BigDecimal("25.00"));
        trip.setTripStatus("Upcoming");
        return trip;
    }

    private Guide guide() {
        Person person = new Person();
        person.setFirstName("Lina");
        person.setLastName("Haddad");

        Guide guide = new Guide();
        guide.setGuideId(4L);
        guide.setPerson(person);
        guide.setApprovalStatus("Approved");
        return guide;
    }

    private TripCategory category() {
        TripCategory category = new TripCategory();
        category.setCategoryId(2L);
        category.setCategoryName("History");
        return category;
    }
}
