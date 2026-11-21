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
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TripDateGuideUpdateTest {

    private static final LocalDateTime STORED_START =
            LocalDateTime.of(2026, 7, 1, 9, 0);
    private static final LocalDateTime STORED_END =
            LocalDateTime.of(2026, 7, 2, 18, 0);
    private static final String REJECTION =
            "Trip dates and assigned guide cannot be changed while active bookings exist.";

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
    void changedStartDateWithActiveGuestsIsRejectedBeforeSave() {
        Trip trip = existingTrip();
        reject(
                trip,
                request(4L, STORED_START.plusDays(1), STORED_END, "Old title")
        );
        assertEquals(STORED_START, trip.getStartDate());
        assertEquals(STORED_END, trip.getEndDate());
    }

    @Test
    void changedEndDateWithActiveGuestsIsRejectedBeforeSave() {
        Trip trip = existingTrip();
        reject(trip, request(4L, STORED_START, STORED_END.plusDays(1), "Old title"));
        assertEquals(STORED_END, trip.getEndDate());
    }

    @Test
    void changedGuideWithActiveGuestsIsRejectedBeforeLookup() {
        Trip trip = existingTrip();
        Guide originalGuide = trip.getGuide();

        reject(trip, request(9L, STORED_START, STORED_END, "Old title"));

        assertSame(originalGuide, trip.getGuide());
        verify(guideRepository, never()).findById(anyLong());
    }

    @Test
    void unchangedDatesAndGuideStillAllowTitleUpdate() {
        Trip trip = existingTrip();
        when(tripRepository.findById(10L)).thenReturn(Optional.of(trip));
        when(bookingRepository.sumGuestsByTripId(10L)).thenReturn(4L);
        when(tripRepository.save(any(Trip.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        adminService.updateTrip(
                10L,
                request(4L, STORED_START, STORED_END, "Petra by night")
        );

        assertEquals("Petra by night", trip.getTitle());
        assertEquals(STORED_START, trip.getStartDate());
        assertEquals(STORED_END, trip.getEndDate());
        assertEquals(4L, trip.getGuide().getGuideId());
        InOrder order = inOrder(entityManager, bookingRepository, tripRepository);
        order.verify(entityManager).refresh(trip, LockModeType.PESSIMISTIC_WRITE);
        order.verify(bookingRepository).sumGuestsByTripId(10L);
        order.verify(tripRepository).save(trip);
    }

    @Test
    void zeroGuestSumAllowsDateAndApprovedGuideChanges() {
        Trip trip = existingTrip();
        Guide replacement = guide(9L, "Omar", "Nasser", "Approved");
        when(tripRepository.findById(10L)).thenReturn(Optional.of(trip));
        when(bookingRepository.sumGuestsByTripId(10L)).thenReturn(0L);
        when(guideRepository.findById(9L)).thenReturn(Optional.of(replacement));
        when(tripRepository.save(any(Trip.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        LocalDateTime newStart = LocalDateTime.of(2026, 8, 2, 9, 0);
        LocalDateTime newEnd = LocalDateTime.of(2026, 8, 3, 18, 0);
        adminService.updateTrip(10L, request(9L, newStart, newEnd, "Old title"));

        assertEquals(newStart, trip.getStartDate());
        assertEquals(newEnd, trip.getEndDate());
        assertSame(replacement, trip.getGuide());
        assertEquals("Approved", replacement.getApprovalStatus());
        InOrder order = inOrder(entityManager, bookingRepository, tripRepository);
        order.verify(entityManager).refresh(trip, LockModeType.PESSIMISTIC_WRITE);
        order.verify(bookingRepository).sumGuestsByTripId(10L);
        order.verify(tripRepository).save(trip);
    }

    private void reject(Trip trip, TripRequestDTO request) {
        when(tripRepository.findById(10L)).thenReturn(Optional.of(trip));
        when(bookingRepository.sumGuestsByTripId(10L)).thenReturn(3L);

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> adminService.updateTrip(10L, request)
        );

        assertEquals(REJECTION, exception.getMessage());
        assertEquals("Old title", trip.getTitle());
        assertEquals("Old description", trip.getDescription());
        assertEquals(STORED_START, trip.getStartDate());
        assertEquals(STORED_END, trip.getEndDate());
        assertEquals(4L, trip.getGuide().getGuideId());
        assertEquals(12, trip.getMaxParticipants());
        assertEquals(new BigDecimal("25.00"), trip.getPricePerPerson());
        InOrder order = inOrder(entityManager, bookingRepository);
        order.verify(entityManager).refresh(trip, LockModeType.PESSIMISTIC_WRITE);
        order.verify(bookingRepository).sumGuestsByTripId(10L);
        verify(tripRepository, never()).save(any());
    }

    private TripRequestDTO request(
            long guideId,
            LocalDateTime start,
            LocalDateTime end,
            String title) {
        return new TripRequestDTO(
                guideId,
                2L,
                title,
                "Old description",
                start,
                end,
                12,
                new BigDecimal("25.00"),
                null
        );
    }

    private Trip existingTrip() {
        Trip trip = new Trip();
        trip.setTripId(10L);
        trip.setGuide(guide(4L, "Lina", "Haddad", "Approved"));
        trip.setCategory(category());
        trip.setTitle("Old title");
        trip.setDescription("Old description");
        trip.setStartDate(STORED_START);
        trip.setEndDate(STORED_END);
        trip.setMaxParticipants(12);
        trip.setPricePerPerson(new BigDecimal("25.00"));
        trip.setTripStatus("Upcoming");
        return trip;
    }

    private Guide guide(
            long guideId,
            String firstName,
            String lastName,
            String approvalStatus) {
        Person person = new Person();
        person.setFirstName(firstName);
        person.setLastName(lastName);

        Guide guide = new Guide();
        guide.setGuideId(guideId);
        guide.setPerson(person);
        guide.setApprovalStatus(approvalStatus);
        return guide;
    }

    private TripCategory category() {
        TripCategory category = new TripCategory();
        category.setCategoryId(2L);
        category.setCategoryName("History");
        return category;
    }
}
