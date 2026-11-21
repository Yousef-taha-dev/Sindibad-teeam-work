package com.sindibad.App.service;

import com.sindibad.App.dto.BookingResponseDTO;
import com.sindibad.App.entity.Booking;
import com.sindibad.App.entity.Person;
import com.sindibad.App.entity.Tourist;
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
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

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
class BookingTouristListOwnershipTest {

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
    void touristCanListOwnBookings() {
        Tourist tourist = tourist(7L, 3L);
        Booking booking = new Booking();
        booking.setBookingId(42L);
        booking.setTourist(tourist);
        BookingResponseDTO response = response();

        when(touristRepository.findById(7L)).thenReturn(Optional.of(tourist));
        when(personRepository.findByEmail("lina@example.com"))
                .thenReturn(Optional.of(person(3L)));
        when(touristRepository.findByPersonPersonId(3L))
                .thenReturn(Optional.of(tourist));
        when(bookingRepository.findByTouristTouristId(7L))
                .thenReturn(List.of(booking));
        when(bookingMapper.toDto(booking)).thenReturn(response);

        List<BookingResponseDTO> result = bookingService.getTouristBookings(
                7L,
                "lina@example.com",
                false
        );

        assertEquals(1, result.size());
        assertSame(response, result.get(0));
        verify(bookingRepository).findByTouristTouristId(7L);
        verify(bookingMapper).toDto(booking);
    }

    @Test
    void touristCannotListAnotherTouristsBookings() {
        when(touristRepository.findById(8L)).thenReturn(Optional.of(tourist(8L, 4L)));
        when(personRepository.findByEmail("lina@example.com"))
                .thenReturn(Optional.of(person(3L)));
        when(touristRepository.findByPersonPersonId(3L))
                .thenReturn(Optional.of(tourist(7L, 3L)));

        assertThrows(
                AccessDeniedException.class,
                () -> bookingService.getTouristBookings(8L, "lina@example.com", false)
        );

        verify(bookingRepository, never()).findByTouristTouristId(any());
        verify(bookingMapper, never()).toDto(any());
    }

    @Test
    void adminCanListAnyExistingTouristsBookings() {
        Tourist tourist = tourist(8L, 4L);
        Booking booking = new Booking();
        booking.setBookingId(42L);
        booking.setTourist(tourist);
        BookingResponseDTO response = response();

        when(touristRepository.findById(8L)).thenReturn(Optional.of(tourist));
        when(bookingRepository.findByTouristTouristId(8L))
                .thenReturn(List.of(booking));
        when(bookingMapper.toDto(booking)).thenReturn(response);

        List<BookingResponseDTO> result = bookingService.getTouristBookings(
                8L,
                "admin@example.com",
                true
        );

        assertEquals(1, result.size());
        assertSame(response, result.get(0));
        verify(personRepository, never()).findByEmail(any());
        verify(touristRepository, never()).findByPersonPersonId(any());
        verify(bookingRepository).findByTouristTouristId(8L);
        verify(bookingMapper).toDto(booking);
    }

    @Test
    void missingTouristIsRejected() {
        when(touristRepository.findById(8L)).thenReturn(Optional.empty());

        ResourceNotFoundException exception = assertThrows(
                ResourceNotFoundException.class,
                () -> bookingService.getTouristBookings(8L, "lina@example.com", false)
        );

        assertEquals("Tourist not found with ID: 8", exception.getMessage());
        verify(bookingRepository, never()).findByTouristTouristId(any());
        verify(bookingMapper, never()).toDto(any());
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

    private BookingResponseDTO response() {
        return new BookingResponseDTO(
                42L,
                null,
                null,
                null,
                null,
                null,
                7L,
                "Lina Haddad",
                null,
                2,
                null,
                "Pending"
        );
    }
}
