package com.sindibad.App.service;

import com.sindibad.App.dto.BookingRequestDTO;
import com.sindibad.App.dto.BookingResponseDTO;
import com.sindibad.App.entity.Booking;
import com.sindibad.App.entity.Guide;
import com.sindibad.App.entity.Person;
import com.sindibad.App.entity.Tourist;
import com.sindibad.App.entity.Trip;
import com.sindibad.App.exception.ResourceNotFoundException;
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
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BookingCreateOwnershipTest {

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
    void touristCreatesBookingForOwnAccount() {
        Trip trip = upcomingTrip();
        Tourist tourist = tourist(7L);
        when(tripRepository.findById(10L)).thenReturn(Optional.of(trip));
        when(touristRepository.findById(7L)).thenReturn(Optional.of(tourist));
        when(personRepository.findByEmail("lina@example.com"))
                .thenReturn(Optional.of(person(3L)));
        when(touristRepository.findByPersonPersonId(3L))
                .thenReturn(Optional.of(tourist));
        when(bookingRepository.sumGuestsByTripId(10L)).thenReturn(1L);
        when(bookingRepository.save(any(Booking.class))).thenAnswer(invocation -> {
            Booking booking = invocation.getArgument(0);
            booking.setBookingId(20L);
            return booking;
        });
        when(bookingMapper.toDto(any(Booking.class))).thenReturn(response());

        BookingResponseDTO result = bookingService.createBooking(
                7L,
                new BookingRequestDTO(10L, null, 7L, 2),
                "lina@example.com",
                false
        );

        ArgumentCaptor<Booking> saved = ArgumentCaptor.forClass(Booking.class);
        verify(bookingRepository).save(saved.capture());
        assertSame(tourist, saved.getValue().getTourist());
        assertSame(trip, saved.getValue().getTrip());
        assertEquals(2, saved.getValue().getNumberOfGuests());
        assertEquals("Pending", saved.getValue().getBookingStatus());
        assertEquals(20L, result.bookingId());
    }

    @Test
    void touristCannotCreateBookingForAnotherTourist() {
        when(tripRepository.findById(10L)).thenReturn(Optional.of(upcomingTrip()));
        when(touristRepository.findById(8L)).thenReturn(Optional.of(tourist(8L)));
        when(personRepository.findByEmail("lina@example.com"))
                .thenReturn(Optional.of(person(3L)));
        when(touristRepository.findByPersonPersonId(3L))
                .thenReturn(Optional.of(tourist(7L)));

        AccessDeniedException exception = assertThrows(
                AccessDeniedException.class,
                () -> bookingService.createBooking(
                        8L,
                        new BookingRequestDTO(10L, null, 8L, 2),
                        "lina@example.com",
                        false
                )
        );

        assertEquals(
                "You can only create your own booking.",
                exception.getMessage()
        );
        verify(bookingRepository, never()).sumGuestsByTripId(any());
        verify(bookingRepository, never()).save(any());
    }

    @Test
    void mismatchedBodyTouristIdDoesNotChangePathTourist() {
        Trip trip = upcomingTrip();
        Tourist pathTourist = tourist(7L);
        when(tripRepository.findById(10L)).thenReturn(Optional.of(trip));
        when(touristRepository.findById(7L)).thenReturn(Optional.of(pathTourist));
        when(personRepository.findByEmail("lina@example.com"))
                .thenReturn(Optional.of(person(3L)));
        when(touristRepository.findByPersonPersonId(3L))
                .thenReturn(Optional.of(pathTourist));
        when(bookingRepository.sumGuestsByTripId(10L)).thenReturn(0L);
        when(bookingRepository.save(any(Booking.class))).thenAnswer(invocation ->
                invocation.getArgument(0));
        when(bookingMapper.toDto(any(Booking.class))).thenReturn(response());

        bookingService.createBooking(
                7L,
                new BookingRequestDTO(10L, null, 99L, 2),
                "lina@example.com",
                false
        );

        ArgumentCaptor<Booking> saved = ArgumentCaptor.forClass(Booking.class);
        verify(bookingRepository).save(saved.capture());
        assertSame(pathTourist, saved.getValue().getTourist());
        verify(touristRepository, never()).findById(99L);
    }

    @Test
    void adminCreatesBookingForAnotherTouristWithoutOwnershipLookup() {
        Trip trip = upcomingTrip();
        Tourist tourist = tourist(8L);
        when(tripRepository.findById(10L)).thenReturn(Optional.of(trip));
        when(touristRepository.findById(8L)).thenReturn(Optional.of(tourist));
        when(bookingRepository.sumGuestsByTripId(10L)).thenReturn(0L);
        when(bookingRepository.save(any(Booking.class))).thenAnswer(invocation ->
                invocation.getArgument(0));
        when(bookingMapper.toDto(any(Booking.class))).thenReturn(response());

        bookingService.createBooking(
                8L,
                new BookingRequestDTO(10L, null, 7L, 2),
                "admin@example.com",
                true
        );

        ArgumentCaptor<Booking> saved = ArgumentCaptor.forClass(Booking.class);
        verify(bookingRepository).save(saved.capture());
        assertSame(tourist, saved.getValue().getTourist());
        verify(personRepository, never()).findByEmail(any());
        verify(touristRepository, never()).findByPersonPersonId(any());
    }

    @Test
    void blankEmailIsRejectedForNonAdmin() {
        when(tripRepository.findById(10L)).thenReturn(Optional.of(upcomingTrip()));
        when(touristRepository.findById(7L)).thenReturn(Optional.of(tourist(7L)));

        AccessDeniedException exception = assertThrows(
                AccessDeniedException.class,
                () -> bookingService.createBooking(
                        7L,
                        new BookingRequestDTO(10L, null, 7L, 2),
                        " ",
                        false
                )
        );

        assertEquals(
                "You can only create your own booking.",
                exception.getMessage()
        );
        verify(personRepository, never()).findByEmail(any());
        verify(bookingRepository, never()).save(any());
    }

    @Test
    void missingEmailIsRejectedForNonAdmin() {
        when(tripRepository.findById(10L)).thenReturn(Optional.of(upcomingTrip()));
        when(touristRepository.findById(7L)).thenReturn(Optional.of(tourist(7L)));

        assertThrows(
                AccessDeniedException.class,
                () -> bookingService.createBooking(
                        7L,
                        new BookingRequestDTO(10L, null, 7L, 2),
                        null,
                        false
                )
        );

        verify(bookingRepository, never()).save(any());
    }

    @Test
    void tripThatIsNotUpcomingIsRejectedBeforeSave() {
        Trip trip = upcomingTrip();
        trip.setTripStatus("Trip New");
        when(tripRepository.findById(10L)).thenReturn(Optional.of(trip));

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> bookingService.createBooking(
                        7L,
                        new BookingRequestDTO(10L, null, 7L, 2),
                        "lina@example.com",
                        false
                )
        );

        assertEquals("This trip is not available for booking.", exception.getMessage());
        verify(touristRepository, never()).findById(any());
        verify(bookingRepository, never()).save(any());
    }

    @Test
    void requestBeyondCapacityIsRejected() {
        Trip trip = upcomingTrip();
        Tourist tourist = tourist(7L);
        when(tripRepository.findById(10L)).thenReturn(Optional.of(trip));
        when(touristRepository.findById(7L)).thenReturn(Optional.of(tourist));
        when(personRepository.findByEmail("lina@example.com"))
                .thenReturn(Optional.of(person(3L)));
        when(touristRepository.findByPersonPersonId(3L))
                .thenReturn(Optional.of(tourist));
        when(bookingRepository.sumGuestsByTripId(10L)).thenReturn(11L);

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> bookingService.createBooking(
                        7L,
                        new BookingRequestDTO(10L, null, 7L, 2),
                        "lina@example.com",
                        false
                )
        );

        assertEquals(
                "Requested seats exceed maximum available capacity for this trip.",
                exception.getMessage()
        );
        verify(bookingRepository, never()).save(any());
    }

    @Test
    void missingTripIsRejectedBeforeSave() {
        when(tripRepository.findById(10L)).thenReturn(Optional.empty());

        ResourceNotFoundException exception = assertThrows(
                ResourceNotFoundException.class,
                () -> bookingService.createBooking(
                        7L,
                        new BookingRequestDTO(10L, null, 7L, 2),
                        "lina@example.com",
                        false
                )
        );

        assertEquals("Trip not found with ID: 10", exception.getMessage());
        verify(bookingRepository, never()).save(any());
    }

    private Trip upcomingTrip() {
        Trip trip = new Trip();
        trip.setTripId(10L);
        trip.setTripStatus("Upcoming");
        trip.setStartDate(LocalDateTime.now().plusDays(2));
        trip.setMaxParticipants(12);
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

    private Tourist tourist(Long touristId) {
        Tourist tourist = new Tourist();
        tourist.setTouristId(touristId);
        return tourist;
    }

    private Person person(Long personId) {
        Person person = new Person();
        person.setPersonId(personId);
        person.setEmail("lina@example.com");
        return person;
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
