package com.sindibad.App.service;

import com.sindibad.App.dto.ImageUploadResult;
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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TripCreateImageTest {

    private static final String MALICIOUS_IMAGE_URL =
            "https://evil.example/phish.png";

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

    @Test
    void createTripIgnoresClientSuppliedImageUrl() throws Exception {
        Guide guide = approvedGuide();
        TripCategory category = category();
        LocalDateTime start = LocalDateTime.now().plusDays(3);
        LocalDateTime end = start.plusDays(1);

        when(guideRepository.findById(4L)).thenReturn(Optional.of(guide));
        when(tripCategoryRepository.findById(2L)).thenReturn(Optional.of(category));
        when(tripRepository.save(any(Trip.class))).thenAnswer(invocation -> {
            Trip trip = invocation.getArgument(0);
            trip.setTripId(10L);
            return trip;
        });

        TripResponseDTO response = adminService.createTrip(new TripRequestDTO(
                4L,
                2L,
                "Petra by night",
                "A guided walk",
                start,
                end,
                12,
                new BigDecimal("25.00"),
                MALICIOUS_IMAGE_URL
        ));

        ArgumentCaptor<Trip> saved = ArgumentCaptor.forClass(Trip.class);
        verify(tripRepository).save(saved.capture());
        verify(imageUploadService, never()).uploadImage(any(), any());
        verify(imageUploadService, never()).deleteImage(any());

        Trip persisted = saved.getValue();
        assertNull(persisted.getTripImageUrl());
        assertNull(persisted.getTripImagePublicId());
        assertEquals("Upcoming", persisted.getTripStatus());
        assertSame(guide, persisted.getGuide());
        assertSame(category, persisted.getCategory());
        assertEquals("Petra by night", persisted.getTitle());
        assertEquals("A guided walk", persisted.getDescription());
        assertEquals(start, persisted.getStartDate());
        assertEquals(end, persisted.getEndDate());
        assertEquals(12, persisted.getMaxParticipants());
        assertEquals(new BigDecimal("25.00"), persisted.getPricePerPerson());

        assertNull(response.tripImageUrl());
        assertEquals(10L, response.tripId());
        assertEquals("Upcoming", response.tripStatus());
        assertEquals("Petra by night", response.title());
        assertEquals("A guided walk", response.description());
        assertEquals(start, response.startDate());
        assertEquals(end, response.endDate());
        assertEquals(4L, response.guideId());
        assertEquals("Lina Haddad", response.guideName());
        assertEquals(2L, response.categoryId());
        assertEquals("History", response.categoryName());
        assertEquals(12, response.maxParticipants());
        assertEquals(new BigDecimal("25.00"), response.pricePerPerson());
    }

    @Test
    void cloudinaryUploadStillStoresReturnedImage() throws Exception {
        Trip trip = new Trip();
        trip.setTripId(10L);
        trip.setTitle("Petra by night");
        trip.setDescription("A guided walk");
        trip.setTripStatus("Upcoming");
        trip.setMaxParticipants(12);
        trip.setPricePerPerson(new BigDecimal("25.00"));
        trip.setGuide(approvedGuide());
        trip.setCategory(category());
        trip.setTripImageUrl(null);
        trip.setTripImagePublicId(null);

        MultipartFile image = org.mockito.Mockito.mock(MultipartFile.class);
        ImageUploadResult uploaded = new ImageUploadResult(
                "https://res.cloudinary.com/demo/image/upload/trips/abc.jpg",
                "trips/abc"
        );

        when(tripRepository.findById(10L)).thenReturn(Optional.of(trip));
        when(imageUploadService.uploadImage(image, "trips")).thenReturn(uploaded);
        when(tripRepository.saveAndFlush(any(Trip.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        TripResponseDTO response = adminService.uploadTripImage(10L, image);

        ArgumentCaptor<Trip> saved = ArgumentCaptor.forClass(Trip.class);
        verify(tripRepository).saveAndFlush(saved.capture());
        verify(imageUploadService).uploadImage(image, "trips");
        verify(imageUploadService, never()).deleteImage(any());

        Trip persisted = saved.getValue();
        assertEquals(uploaded.secureUrl(), persisted.getTripImageUrl());
        assertEquals(uploaded.publicId(), persisted.getTripImagePublicId());
        assertEquals("Upcoming", persisted.getTripStatus());
        assertEquals("Petra by night", persisted.getTitle());
        assertEquals(uploaded.secureUrl(), response.tripImageUrl());
        assertEquals("Upcoming", response.tripStatus());
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
}
