package com.sindibad.App.service;

import com.sindibad.App.dto.BookingRequestDTO;
import com.sindibad.App.dto.BookingResponseDTO;
import com.sindibad.App.dto.TripRequestDTO;
import com.sindibad.App.dto.TripResponseDTO;
import com.sindibad.App.entity.Booking;
import com.sindibad.App.entity.Guide;
import com.sindibad.App.entity.Person;
import com.sindibad.App.entity.Tourist;
import com.sindibad.App.entity.Trip;
import com.sindibad.App.entity.TripCategory;
import com.sindibad.App.exception.ResourceNotFoundException;
import com.sindibad.App.mapper.BookingMapper;
import com.sindibad.App.mapper.GuideMapper;
import com.sindibad.App.mapper.TripMapperImpl;
import com.sindibad.App.repository.BookingRepository;
import com.sindibad.App.repository.EarningRepository;
import com.sindibad.App.repository.PaymentRepository;
import com.sindibad.App.repository.GuideRepository;
import jakarta.persistence.EntityManager;
import com.sindibad.App.repository.PersonRepository;
import com.sindibad.App.repository.TouristRepository;
import com.sindibad.App.repository.TripCategoryRepository;
import com.sindibad.App.repository.TripRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TripStatusWorkflowTest {

    @Mock
    private PersonRepository personRepository;
    @Mock
    private GuideRepository guideRepository;
    @Mock
    private EntityManager entityManager;
    @Mock
    private TouristRepository touristRepository;
    @Mock
    private TripRepository tripRepository;
    @Mock
    private TripCategoryRepository tripCategoryRepository;
    @Mock
    private BookingRepository bookingRepository;
    @Mock
    private PaymentRepository paymentRepository;
    @Mock
    private EarningRepository earningRepository;
    @Mock
    private GuideMapper guideMapper;
    @Mock
    private BookingMapper bookingMapper;
    @Mock
    private ImageUploadService imageUploadService;

    private AdminServiceImpl adminService;
    private TripServiceImpl tripService;
    private BookingServiceImpl bookingService;

    @BeforeEach
    void setUp() {
        TripMapperImpl tripMapper = new TripMapperImpl();

        adminService = new AdminServiceImpl(
                personRepository,
                guideRepository,
                touristRepository,
                tripRepository,
                tripCategoryRepository,
                bookingRepository,
                earningRepository,
                tripMapper,
                guideMapper,
                bookingMapper,
                imageUploadService
        );

        tripService = new TripServiceImpl(tripRepository, tripMapper);
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
    void createTripSetsUpcomingAndReturnsMappedTrip() {
        Guide guide = approvedGuide();
        TripCategory category = category();

        when(guideRepository.findById(4L)).thenReturn(Optional.of(guide));
        when(tripCategoryRepository.findById(2L)).thenReturn(Optional.of(category));
        when(tripRepository.save(any(Trip.class))).thenAnswer(invocation -> {
            Trip trip = invocation.getArgument(0);
            trip.setTripId(10L);
            return trip;
        });

        TripResponseDTO response = adminService.createTrip(validRequest());

        ArgumentCaptor<Trip> saved = ArgumentCaptor.forClass(Trip.class);
        verify(tripRepository).save(saved.capture());

        Trip persisted = saved.getValue();
        assertEquals("Upcoming", persisted.getTripStatus());
        assertSame(guide, persisted.getGuide());
        assertSame(category, persisted.getCategory());
        assertEquals("Petra by night", persisted.getTitle());

        assertEquals(10L, response.tripId());
        assertEquals("Upcoming", response.tripStatus());
        assertEquals("Petra by night", response.title());
        assertEquals(4L, response.guideId());
        assertEquals("Lina Haddad", response.guideName());
        assertEquals(2L, response.categoryId());
        assertEquals("History", response.categoryName());
        assertEquals(12, response.maxParticipants());
        assertEquals(new BigDecimal("25.00"), response.pricePerPerson());
    }

    @Test
    void createTripRejectsEndDateBeforeStartDate() {
        TripRequestDTO request = new TripRequestDTO(
                4L,
                2L,
                "Petra by night",
                "A guided walk",
                LocalDateTime.now().plusDays(4),
                LocalDateTime.now().plusDays(3),
                12,
                new BigDecimal("25.00"),
                null
        );

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> adminService.createTrip(request)
        );

        assertEquals("End date cannot be before start date", exception.getMessage());
        verify(guideRepository, never()).findById(any());
        verify(tripRepository, never()).save(any());
    }

    @Test
    void createTripRejectsMissingGuide() {
        when(guideRepository.findById(4L)).thenReturn(Optional.empty());

        ResourceNotFoundException exception = assertThrows(
                ResourceNotFoundException.class,
                () -> adminService.createTrip(validRequest())
        );

        assertEquals("Guide not found with ID: 4", exception.getMessage());
        verify(tripRepository, never()).save(any());
    }

    @Test
    void createTripRejectsGuideWhoIsNotApproved() {
        Guide guide = approvedGuide();
        guide.setApprovalStatus("Pending");
        when(guideRepository.findById(4L)).thenReturn(Optional.of(guide));

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> adminService.createTrip(validRequest())
        );

        assertEquals(
                "Cannot assign trip to a guide who is not approved",
                exception.getMessage()
        );
        verify(tripCategoryRepository, never()).findById(any());
        verify(tripRepository, never()).save(any());
    }

    @Test
    void createTripRejectsMissingCategory() {
        when(guideRepository.findById(4L)).thenReturn(Optional.of(approvedGuide()));
        when(tripCategoryRepository.findById(2L)).thenReturn(Optional.empty());

        ResourceNotFoundException exception = assertThrows(
                ResourceNotFoundException.class,
                () -> adminService.createTrip(validRequest())
        );

        assertEquals("Category not found with ID: 2", exception.getMessage());
        verify(tripRepository, never()).save(any());
    }

    @Test
    void activeTripListRequestsUpcomingStatus() {
        Trip trip = new Trip();
        trip.setTripId(10L);
        trip.setTitle("Petra by night");
        trip.setTripStatus("Upcoming");
        trip.setGuide(approvedGuide());
        trip.setCategory(category());
        trip.setMaxParticipants(12);
        trip.setPricePerPerson(new BigDecimal("25.00"));

        when(tripRepository.findByTripStatus("Upcoming")).thenReturn(List.of(trip));

        List<TripResponseDTO> trips = tripService.getAllActiveTrips();

        verify(tripRepository).findByTripStatus("Upcoming");
        assertEquals(1, trips.size());
        assertEquals("Upcoming", trips.get(0).tripStatus());
        assertEquals("Petra by night", trips.get(0).title());
    }

    @Test
    void bookingAcceptsUpcomingTripWhenSeatsRemain() {
        Trip trip = bookableTrip();
        Tourist tourist = new Tourist();
        tourist.setTouristId(7L);

        when(tripRepository.findById(10L)).thenReturn(Optional.of(trip));
        when(touristRepository.findById(7L)).thenReturn(Optional.of(tourist));
        when(bookingRepository.sumGuestsByTripId(10L)).thenReturn(4L);
        when(bookingRepository.save(any(Booking.class))).thenAnswer(invocation -> {
            Booking booking = invocation.getArgument(0);
            booking.setBookingId(20L);
            return booking;
        });
        when(bookingMapper.toDto(any(Booking.class))).thenReturn(bookingResponse());

        BookingResponseDTO response = bookingService.createBooking(
                7L,
                new BookingRequestDTO(10L, null, 7L, 2),
                "admin@example.com",
                true
        );

        ArgumentCaptor<Booking> saved = ArgumentCaptor.forClass(Booking.class);
        verify(bookingRepository).save(saved.capture());
        assertSame(trip, saved.getValue().getTrip());
        assertSame(tourist, saved.getValue().getTourist());
        assertEquals(2, saved.getValue().getNumberOfGuests());
        assertEquals("Pending", saved.getValue().getBookingStatus());
        assertEquals(20L, response.bookingId());
    }

    @Test
    void bookingRejectsTripThatIsNotUpcoming() {
        Trip trip = bookableTrip();
        trip.setTripStatus("Trip New");
        when(tripRepository.findById(10L)).thenReturn(Optional.of(trip));

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
        verify(touristRepository, never()).findById(any());
        verify(bookingRepository, never()).save(any());
    }

    @Test
    void bookingRejectsUpcomingTripWhenSeatsAreExceeded() {
        Trip trip = bookableTrip();
        Tourist tourist = new Tourist();
        tourist.setTouristId(7L);

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
        verify(bookingRepository, never()).save(any());
    }

    private TripRequestDTO validRequest() {
        return new TripRequestDTO(
                4L,
                2L,
                "Petra by night",
                "A guided walk",
                LocalDateTime.now().plusDays(3),
                LocalDateTime.now().plusDays(4),
                12,
                new BigDecimal("25.00"),
                null
        );
    }

    private Guide approvedGuide() {
        Person person = new Person();
        person.setPersonId(8L);
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

    private Trip bookableTrip() {
        Trip trip = new Trip();
        trip.setTripId(10L);
        trip.setTitle("Petra by night");
        trip.setTripStatus("Upcoming");
        trip.setStartDate(LocalDateTime.now().plusDays(2));
        trip.setMaxParticipants(12);
        trip.setPricePerPerson(new BigDecimal("25.00"));
        trip.setGuide(approvedGuide());
        trip.setCategory(category());
        return trip;
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
                2,
                null,
                "Pending"
        );
    }
}
