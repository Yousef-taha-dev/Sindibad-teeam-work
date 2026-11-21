package com.sindibad.App.service;

import com.sindibad.App.entity.Trip;
import com.sindibad.App.exception.ResourceNotFoundException;
import com.sindibad.App.mapper.BookingMapper;
import com.sindibad.App.mapper.GuideMapper;
import com.sindibad.App.mapper.TripMapper;
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

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TripDeletionBookingTest {

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
    private TripMapper tripMapper;
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
                tripMapper,
                guideMapper,
                bookingMapper,
                null,
                entityManager
        );
    }

    @Test
    void pendingBookingPreventsDeletion() {
        rejectDeletionWhenAnyBookingExists();
    }

    @Test
    void confirmedBookingPreventsDeletion() {
        rejectDeletionWhenAnyBookingExists();
    }

    @Test
    void cancelledBookingPreventsDeletion() {
        rejectDeletionWhenAnyBookingExists();
    }

    @Test
    void tripWithNoBookingsIsDeletedAfterTheLock() {
        Trip trip = trip();
        when(tripRepository.findById(10L)).thenReturn(Optional.of(trip));
        when(bookingRepository.existsByTripTripId(10L)).thenReturn(false);

        adminService.deleteTrip(10L);

        InOrder order = inOrder(entityManager, bookingRepository, tripRepository);
        order.verify(entityManager).refresh(trip, LockModeType.PESSIMISTIC_WRITE);
        order.verify(bookingRepository).existsByTripTripId(10L);
        order.verify(tripRepository).deleteById(10L);
        verify(bookingRepository, never()).sumGuestsByTripId(any());
    }

    @Test
    void missingTripIsRejectedBeforeTheLock() {
        when(tripRepository.findById(10L)).thenReturn(Optional.empty());

        ResourceNotFoundException exception = assertThrows(
                ResourceNotFoundException.class,
                () -> adminService.deleteTrip(10L)
        );

        assertEquals("Trip not found with ID: 10", exception.getMessage());
        verify(entityManager, never()).refresh(any(Trip.class), any(LockModeType.class));
        verify(bookingRepository, never()).existsByTripTripId(any());
        verify(tripRepository, never()).deleteById(any());
    }

    private void rejectDeletionWhenAnyBookingExists() {
        Trip trip = trip();
        when(tripRepository.findById(10L)).thenReturn(Optional.of(trip));
        when(bookingRepository.existsByTripTripId(10L)).thenReturn(true);

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> adminService.deleteTrip(10L)
        );

        assertEquals(
                "This trip cannot be deleted because it has bookings.",
                exception.getMessage()
        );
        InOrder order = inOrder(entityManager, bookingRepository);
        order.verify(entityManager).refresh(trip, LockModeType.PESSIMISTIC_WRITE);
        order.verify(bookingRepository).existsByTripTripId(10L);
        verify(tripRepository, never()).deleteById(any());
        verify(tripRepository, never()).delete(any());
        verify(bookingRepository, never()).sumGuestsByTripId(any());
    }

    private Trip trip() {
        Trip trip = new Trip();
        trip.setTripId(10L);
        return trip;
    }
}
