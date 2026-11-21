package com.sindibad.App.service;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.sindibad.App.entity.Trip;
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
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TripDeletionImageCleanupTest {

    private static final String PUBLIC_ID = "trips/petra";

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
    private ImageUploadService imageUploadService;
    @Mock
    private EntityManager entityManager;

    private AdminServiceImpl adminService;

    @BeforeEach
    void setUp() {
        TransactionSynchronizationManager.initSynchronization();
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
                imageUploadService,
                entityManager
        );
    }

    @AfterEach
    void clearTransactionSynchronization() {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.clearSynchronization();
        }
    }

    @Test
    void nullPublicIdDeletesTheTripWithoutImageCleanup() throws Exception {
        deleteWithoutImage(null);
    }

    @Test
    void blankPublicIdDeletesTheTripWithoutImageCleanup() throws Exception {
        deleteWithoutImage("   ");
    }

    @Test
    void imageIsDeletedOnlyWhenTheRegisteredCallbackCommits() throws Exception {
        when(tripRepository.findById(10L)).thenReturn(Optional.of(trip(PUBLIC_ID)));
        when(bookingRepository.existsByTripTripId(10L)).thenReturn(false);

        adminService.deleteTrip(10L);

        verify(tripRepository).deleteById(10L);
        verify(imageUploadService, never()).deleteImage(any());

        List<TransactionSynchronization> synchronizations =
                TransactionSynchronizationManager.getSynchronizations();
        assertEquals(1, synchronizations.size());

        synchronizations.get(0).afterCommit();

        verify(imageUploadService).deleteImage(PUBLIC_ID);
    }

    @Test
    void bookingPreventsDeletionAndImageCleanup() throws Exception {
        when(tripRepository.findById(10L)).thenReturn(Optional.of(trip(PUBLIC_ID)));
        when(bookingRepository.existsByTripTripId(10L)).thenReturn(true);

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> adminService.deleteTrip(10L)
        );

        assertEquals(
                "This trip cannot be deleted because it has bookings.",
                exception.getMessage()
        );
        assertTrue(TransactionSynchronizationManager.getSynchronizations().isEmpty());
        verify(tripRepository, never()).deleteById(any());
        verify(imageUploadService, never()).deleteImage(any());
    }

    @Test
    void databaseDeletionFailureDoesNotDeleteTheImage() throws Exception {
        when(tripRepository.findById(10L)).thenReturn(Optional.of(trip(PUBLIC_ID)));
        when(bookingRepository.existsByTripTripId(10L)).thenReturn(false);
        doThrow(new RuntimeException("database delete failed"))
                .when(tripRepository).deleteById(10L);

        assertThrows(RuntimeException.class, () -> adminService.deleteTrip(10L));

        verify(imageUploadService, never()).deleteImage(any());
    }

    @Test
    void cloudinaryFailureInsideAfterCommitDoesNotEscape() throws Exception {
        when(tripRepository.findById(10L)).thenReturn(Optional.of(trip(PUBLIC_ID)));
        when(bookingRepository.existsByTripTripId(10L)).thenReturn(false);
        doThrow(new IOException("cloudinary unavailable"))
                .when(imageUploadService).deleteImage(PUBLIC_ID);

        adminService.deleteTrip(10L);

        Logger logger = (Logger) LoggerFactory.getLogger(AdminServiceImpl.class);
        ListAppender<ILoggingEvent> appender = new ListAppender<>();
        appender.start();
        logger.addAppender(appender);
        try {
            TransactionSynchronization synchronization =
                    TransactionSynchronizationManager.getSynchronizations().get(0);

            assertDoesNotThrow(synchronization::afterCommit);
        } finally {
            logger.detachAppender(appender);
        }

        assertTrue(appender.list.stream().anyMatch(event ->
                event.getLevel() == Level.ERROR
                        && event.getFormattedMessage().contains(PUBLIC_ID)
        ));
    }

    private void deleteWithoutImage(String publicId) throws Exception {
        when(tripRepository.findById(10L)).thenReturn(Optional.of(trip(publicId)));
        when(bookingRepository.existsByTripTripId(10L)).thenReturn(false);

        adminService.deleteTrip(10L);

        verify(tripRepository).deleteById(10L);
        assertTrue(TransactionSynchronizationManager.getSynchronizations().isEmpty());
        verify(imageUploadService, never()).deleteImage(any());
    }

    private Trip trip(String publicId) {
        Trip trip = new Trip();
        trip.setTripId(10L);
        trip.setTripImagePublicId(publicId);
        return trip;
    }
}
