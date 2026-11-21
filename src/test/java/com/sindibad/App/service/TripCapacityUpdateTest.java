package com.sindibad.App.service;

import com.sindibad.App.dto.TripRequestDTO;
import com.sindibad.App.dto.TripResponseDTO;
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
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TripCapacityUpdateTest {

    private static final String EXISTING_IMAGE_URL =
            "https://res.cloudinary.com/demo/image/upload/trips/current.jpg";

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
    void requestedCapacityBelowBookedGuestsIsRejected() {
        Trip trip = existingTrip();
        when(tripRepository.findById(10L)).thenReturn(Optional.of(trip));
        when(bookingRepository.sumGuestsByTripId(10L)).thenReturn(8L);

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> adminService.updateTrip(10L, request(4L, 2L, 5))
        );

        assertEquals(
                "Maximum participants cannot be lower than the number of guests already booked.",
                exception.getMessage()
        );
        assertEquals("Old title", trip.getTitle());
        assertEquals(12, trip.getMaxParticipants());
        InOrder order = inOrder(entityManager, bookingRepository);
        order.verify(entityManager).refresh(trip, LockModeType.PESSIMISTIC_WRITE);
        order.verify(bookingRepository).sumGuestsByTripId(10L);
        verify(tripRepository, never()).save(any());
    }

    @Test
    void requestedCapacityEqualToBookedGuestsIsAccepted() {
        Trip saved = updateAcceptedTrip(8, 8);

        assertEquals(8, saved.getMaxParticipants());
        assertEquals("Petra by night", saved.getTitle());
        assertEquals(new BigDecimal("40.00"), saved.getPricePerPerson());
    }

    @Test
    void requestedCapacityAboveBookedGuestsUpdatesTheOtherFields() {
        TripCategory replacementCategory = category(3L, "Desert");
        when(tripCategoryRepository.findById(3L)).thenReturn(Optional.of(replacementCategory));

        Trip saved = updateAcceptedTrip(8, 10, 4L, 3L);

        assertEquals(10, saved.getMaxParticipants());
        assertEquals("Petra by night", saved.getTitle());
        assertEquals("A longer guided walk", saved.getDescription());
        assertEquals(LocalDateTime.of(2026, 7, 1, 9, 0), saved.getStartDate());
        assertEquals(LocalDateTime.of(2026, 7, 2, 18, 0), saved.getEndDate());
        assertEquals(new BigDecimal("40.00"), saved.getPricePerPerson());
        assertEquals(4L, saved.getGuide().getGuideId());
        assertSame(replacementCategory, saved.getCategory());
        assertEquals(EXISTING_IMAGE_URL, saved.getTripImageUrl());
        assertEquals("Upcoming", saved.getTripStatus());
    }

    private Trip updateAcceptedTrip(int bookedGuests, int requestedMaximum) {
        return updateAcceptedTrip(bookedGuests, requestedMaximum, 4L, 2L);
    }

    private Trip updateAcceptedTrip(
            int bookedGuests,
            int requestedMaximum,
            long guideId,
            long categoryId) {
        Trip trip = existingTrip();
        when(tripRepository.findById(10L)).thenReturn(Optional.of(trip));
        when(bookingRepository.sumGuestsByTripId(10L)).thenReturn((long) bookedGuests);
        when(tripRepository.save(any(Trip.class))).thenAnswer(invocation -> invocation.getArgument(0));

        TripResponseDTO response = adminService.updateTrip(
                10L,
                request(guideId, categoryId, requestedMaximum)
        );

        ArgumentCaptor<Trip> savedTrip = ArgumentCaptor.forClass(Trip.class);
        verify(tripRepository).save(savedTrip.capture());
        assertEquals(requestedMaximum, response.maxParticipants());
        assertEquals("Petra by night", response.title());
        return savedTrip.getValue();
    }

    private TripRequestDTO request(long guideId, long categoryId, int maxParticipants) {
        return new TripRequestDTO(
                guideId,
                categoryId,
                "Petra by night",
                "A longer guided walk",
                LocalDateTime.of(2026, 7, 1, 9, 0),
                LocalDateTime.of(2026, 7, 2, 18, 0),
                maxParticipants,
                new BigDecimal("40.00"),
                "https://evil.example/replacement.png"
        );
    }

    private Trip existingTrip() {
        Trip trip = new Trip();
        trip.setTripId(10L);
        trip.setGuide(guide(4L, "Lina", "Haddad"));
        trip.setCategory(category(2L, "History"));
        trip.setTitle("Old title");
        trip.setDescription("Old description");
        trip.setStartDate(LocalDateTime.of(2026, 7, 1, 9, 0));
        trip.setEndDate(LocalDateTime.of(2026, 7, 2, 18, 0));
        trip.setMaxParticipants(12);
        trip.setPricePerPerson(new BigDecimal("25.00"));
        trip.setTripStatus("Upcoming");
        trip.setTripImageUrl(EXISTING_IMAGE_URL);
        trip.setTripImagePublicId("trips/current");
        return trip;
    }

    private Guide guide(long guideId, String firstName, String lastName) {
        Person person = new Person();
        person.setFirstName(firstName);
        person.setLastName(lastName);

        Guide guide = new Guide();
        guide.setGuideId(guideId);
        guide.setPerson(person);
        guide.setApprovalStatus("Approved");
        return guide;
    }

    private TripCategory category(long categoryId, String name) {
        TripCategory category = new TripCategory();
        category.setCategoryId(categoryId);
        category.setCategoryName(name);
        return category;
    }
}
