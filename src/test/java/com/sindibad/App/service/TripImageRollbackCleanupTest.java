package com.sindibad.App.service;

import com.sindibad.App.dto.ImageUploadResult;
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
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TripImageRollbackCleanupTest {

    private static final String NEW_PUBLIC_ID = "trips/new";
    private static final String OLD_PUBLIC_ID = "trips/old";

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
    private ImageUploadService imageUploadService;

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
                new TripMapperImpl(),
                guideMapper,
                bookingMapper,
                imageUploadService
        );
    }

    @AfterEach
    void tearDown() {
        TransactionSynchronizationManager.clearSynchronization();
    }

    @Test
    void committedTransactionRetainsTheNewImage() throws Exception {
        Trip trip = trip(null);
        when(tripRepository.findById(10L)).thenReturn(Optional.of(trip));
        when(imageUploadService.uploadImage(any(), any())).thenReturn(uploaded());
        when(tripRepository.saveAndFlush(any(Trip.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        TripResponseDTO response = adminService.uploadTripImage(10L, image());

        assertEquals(NEW_PUBLIC_ID, trip.getTripImagePublicId());
        assertEquals(uploaded().secureUrl(), response.tripImageUrl());
        verify(imageUploadService, never()).deleteImage(any());

        TransactionSynchronization synchronization = synchronization();
        synchronization.afterCommit();
        synchronization.afterCompletion(TransactionSynchronization.STATUS_COMMITTED);

        verify(imageUploadService, never()).deleteImage(any());
    }

    @Test
    void rollbackDeletesTheNewImageOnce() throws Exception {
        Trip trip = trip(OLD_PUBLIC_ID);
        when(tripRepository.findById(10L)).thenReturn(Optional.of(trip));
        when(imageUploadService.uploadImage(any(), any())).thenReturn(uploaded());
        when(tripRepository.saveAndFlush(any(Trip.class)))
                .thenThrow(new IllegalStateException("database update failed"));

        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> adminService.uploadTripImage(10L, image())
        );

        assertEquals("database update failed", exception.getMessage());
        verify(imageUploadService, never()).deleteImage(any());

        synchronization().afterCompletion(TransactionSynchronization.STATUS_ROLLED_BACK);

        verify(imageUploadService, times(1)).deleteImage(NEW_PUBLIC_ID);
        verify(imageUploadService, never()).deleteImage(OLD_PUBLIC_ID);
    }

    @Test
    void cleanupFailureDoesNotMaskTheOriginalFailure() throws Exception {
        Trip trip = trip(null);
        when(tripRepository.findById(10L)).thenReturn(Optional.of(trip));
        when(imageUploadService.uploadImage(any(), any())).thenReturn(uploaded());
        when(tripRepository.saveAndFlush(any(Trip.class)))
                .thenThrow(new IllegalStateException("database update failed"));
        doThrow(new java.io.IOException("cloudinary unavailable"))
                .when(imageUploadService).deleteImage(NEW_PUBLIC_ID);

        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> adminService.uploadTripImage(10L, image())
        );

        assertEquals("database update failed", exception.getMessage());
        assertDoesNotThrow(() ->
                synchronization().afterCompletion(TransactionSynchronization.STATUS_ROLLED_BACK)
        );
        verify(imageUploadService, times(1)).deleteImage(NEW_PUBLIC_ID);
    }

    @Test
    void oldImageIsDeletedOnlyAfterCommit() throws Exception {
        Trip trip = trip(OLD_PUBLIC_ID);
        when(tripRepository.findById(10L)).thenReturn(Optional.of(trip));
        when(imageUploadService.uploadImage(any(), any())).thenReturn(uploaded());
        when(tripRepository.saveAndFlush(any(Trip.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        adminService.uploadTripImage(10L, image());

        verify(imageUploadService, never()).deleteImage(any());

        TransactionSynchronization synchronization = synchronization();
        synchronization.afterCommit();
        synchronization.afterCompletion(TransactionSynchronization.STATUS_COMMITTED);

        verify(imageUploadService, times(1)).deleteImage(OLD_PUBLIC_ID);
        verify(imageUploadService, never()).deleteImage(NEW_PUBLIC_ID);
    }

    private TransactionSynchronization synchronization() {
        List<TransactionSynchronization> synchronizations =
                TransactionSynchronizationManager.getSynchronizations();
        assertEquals(1, synchronizations.size());
        return synchronizations.get(0);
    }

    private MultipartFile image() {
        return mock(MultipartFile.class);
    }

    private ImageUploadResult uploaded() {
        return new ImageUploadResult(
                "https://res.cloudinary.com/demo/image/upload/trips/new.jpg",
                NEW_PUBLIC_ID
        );
    }

    private Trip trip(String currentPublicId) {
        Person person = new Person();
        person.setFirstName("Lina");
        person.setLastName("Haddad");

        Guide guide = new Guide();
        guide.setGuideId(4L);
        guide.setPerson(person);

        TripCategory category = new TripCategory();
        category.setCategoryId(2L);
        category.setCategoryName("History");

        Trip trip = new Trip();
        trip.setTripId(10L);
        trip.setTitle("Petra by night");
        trip.setDescription("A guided walk");
        trip.setTripStatus("Upcoming");
        trip.setMaxParticipants(12);
        trip.setPricePerPerson(new BigDecimal("25.00"));
        trip.setGuide(guide);
        trip.setCategory(category);
        trip.setTripImagePublicId(currentPublicId);
        return trip;
    }
}
