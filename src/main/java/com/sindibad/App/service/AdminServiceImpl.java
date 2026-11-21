package com.sindibad.App.service;

import com.sindibad.App.dto.*;
import com.sindibad.App.entity.Guide;
import com.sindibad.App.entity.Trip;
import com.sindibad.App.entity.TripCategory;
import com.sindibad.App.exception.ResourceNotFoundException;
import com.sindibad.App.mapper.BookingMapper;
import com.sindibad.App.mapper.GuideMapper;
import com.sindibad.App.mapper.TripMapper;
import com.sindibad.App.repository.*;

import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import jakarta.persistence.PersistenceContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.multipart.MultipartFile;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.List;

@Service
public class AdminServiceImpl implements AdminService {

    private static final Logger log =
            LoggerFactory.getLogger(AdminServiceImpl.class);

    private final PersonRepository personRepository;
    private final GuideRepository guideRepository;
    private final TouristRepository touristRepository;
    private final TripRepository tripRepository;
    private final TripCategoryRepository tripCategoryRepository;
    private final BookingRepository bookingRepository;
    private final EarningRepository earningRepository;

    private final TripMapper tripMapper;
    private final GuideMapper guideMapper;
    private final BookingMapper bookingMapper;

    private final ImageUploadService imageUploadService;

    @PersistenceContext
    private EntityManager entityManager;

    @Autowired
    public AdminServiceImpl(
            PersonRepository personRepository,
            GuideRepository guideRepository,
            TouristRepository touristRepository,
            TripRepository tripRepository,
            TripCategoryRepository tripCategoryRepository,
            BookingRepository bookingRepository,
            EarningRepository earningRepository,
            TripMapper tripMapper,
            GuideMapper guideMapper,
            BookingMapper bookingMapper,
            ImageUploadService imageUploadService) {
        this(
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
                null
        );
    }

    AdminServiceImpl(
            PersonRepository personRepository,
            GuideRepository guideRepository,
            TouristRepository touristRepository,
            TripRepository tripRepository,
            TripCategoryRepository tripCategoryRepository,
            BookingRepository bookingRepository,
            EarningRepository earningRepository,
            TripMapper tripMapper,
            GuideMapper guideMapper,
            BookingMapper bookingMapper,
            ImageUploadService imageUploadService,
            EntityManager entityManager) {

        this.personRepository = personRepository;
        this.guideRepository = guideRepository;
        this.touristRepository = touristRepository;
        this.tripRepository = tripRepository;
        this.tripCategoryRepository = tripCategoryRepository;
        this.bookingRepository = bookingRepository;
        this.earningRepository = earningRepository;

        this.tripMapper = tripMapper;
        this.guideMapper = guideMapper;
        this.bookingMapper = bookingMapper;

        this.imageUploadService = imageUploadService;
        this.entityManager = entityManager;
    }

    // ==================== Trips Management ====================

    @Override
    @Transactional
    public TripResponseDTO createTrip(TripRequestDTO tripDTO) {

        if (tripDTO.endDate().isBefore(tripDTO.startDate())) {
            throw new IllegalArgumentException(
                    "End date cannot be before start date"
            );
        }

        Guide guide = guideRepository.findById(tripDTO.guideId())
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Guide not found with ID: " + tripDTO.guideId()
                        )
                );

        if (!"Approved".equalsIgnoreCase(guide.getApprovalStatus())) {
            throw new IllegalArgumentException(
                    "Cannot assign trip to a guide who is not approved"
            );
        }

        TripCategory tripCategory =
                tripCategoryRepository.findById(tripDTO.categoryId())
                        .orElseThrow(() ->
                                new ResourceNotFoundException(
                                        "Category not found with ID: "
                                                + tripDTO.categoryId()
                                )
                        );

        Trip trip = tripMapper.toEntity(tripDTO);

        trip.setGuide(guide);
        trip.setCategory(tripCategory);
        trip.setTripStatus("Upcoming");

        Trip savedTrip = tripRepository.save(trip);

        return tripMapper.toDto(savedTrip);
    }

    @Override
    @Transactional
    public TripResponseDTO updateTrip(
            Long tripId,
            TripRequestDTO tripDTO) {

        Trip trip = tripRepository.findById(tripId)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Trip not found with ID: " + tripId
                        )
                );

        if (tripDTO.endDate().isBefore(tripDTO.startDate())) {
            throw new IllegalArgumentException(
                    "End date cannot be before start date"
            );
        }

        entityManager.refresh(trip, LockModeType.PESSIMISTIC_WRITE);

        long bookedGuests =
                bookingRepository.sumGuestsByTripId(trip.getTripId());

        if (tripDTO.maxParticipants() < bookedGuests) {
            throw new IllegalArgumentException(
                    "Maximum participants cannot be lower than the number of guests already booked."
            );
        }

        if (trip.getPricePerPerson().compareTo(tripDTO.pricePerPerson()) != 0
                && bookingRepository.existsByTripTripIdAndBookingStatus(
                        trip.getTripId(), "Pending")) {
            throw new IllegalArgumentException(
                    "The trip price cannot be changed while pending bookings exist."
            );
        }

        if (bookedGuests > 0
                && (!trip.getStartDate().equals(tripDTO.startDate())
                || !trip.getEndDate().equals(tripDTO.endDate())
                || !trip.getGuide().getGuideId().equals(tripDTO.guideId()))) {
            throw new IllegalArgumentException(
                    "Trip dates and assigned guide cannot be changed while active bookings exist."
            );
        }

        // Update Guide
        if (!trip.getGuide().getGuideId().equals(tripDTO.guideId())) {

            Guide guide = guideRepository.findById(tripDTO.guideId())
                    .orElseThrow(() ->
                            new ResourceNotFoundException(
                                    "Guide not found with ID: "
                                            + tripDTO.guideId()
                            )
                    );

            if (!"Approved".equalsIgnoreCase(
                    guide.getApprovalStatus())) {

                throw new IllegalArgumentException(
                        "Cannot assign trip to a guide who is not approved"
                );
            }

            trip.setGuide(guide);
        }

        // Update Category
        if (!trip.getCategory().getCategoryId()
                .equals(tripDTO.categoryId())) {

            TripCategory category =
                    tripCategoryRepository.findById(
                            tripDTO.categoryId()
                    ).orElseThrow(() ->
                            new ResourceNotFoundException(
                                    "Category not found with ID: "
                                            + tripDTO.categoryId()
                            )
                    );

            trip.setCategory(category);
        }

        trip.setTitle(tripDTO.title());
        trip.setDescription(tripDTO.description());
        trip.setStartDate(tripDTO.startDate());
        trip.setEndDate(tripDTO.endDate());
        trip.setMaxParticipants(tripDTO.maxParticipants());
        trip.setPricePerPerson(tripDTO.pricePerPerson());

        // Keep the existing image during normal trip updates.

        Trip updatedTrip = tripRepository.save(trip);

        return tripMapper.toDto(updatedTrip);
    }

    @Override
    @Transactional(readOnly = true)
    public TripResponseDTO getTripById(Long tripId) {

        Trip trip = tripRepository.findByIdWithDetails(tripId)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Trip not found with ID: " + tripId
                        )
                );

        return tripMapper.toDto(trip);
    }

    @Override
    @Transactional(readOnly = true)
    public List<TripResponseDTO> getAllTrips() {

        return tripRepository.findAllWithDetails()
                .stream()
                .map(tripMapper::toDto)
                .toList();
    }

    @Override
    @Transactional
    public void deleteTrip(Long tripId) {

        Trip trip = tripRepository.findById(tripId)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Trip not found with ID: " + tripId
                        )
                );

        entityManager.refresh(trip, LockModeType.PESSIMISTIC_WRITE);

        if (bookingRepository.existsByTripTripId(trip.getTripId())) {
            throw new IllegalArgumentException(
                    "This trip cannot be deleted because it has bookings."
            );
        }

        String imagePublicId = trip.getTripImagePublicId();

        if (imagePublicId != null && !imagePublicId.isBlank()) {
            TransactionSynchronizationManager.registerSynchronization(
                    new TransactionSynchronization() {

                        @Override
                        public void afterCommit() {

                            try {
                                imageUploadService.deleteImage(imagePublicId);

                            } catch (Exception exception) {

                                log.error(
                                        "Failed to delete old Cloudinary image: {}",
                                        imagePublicId,
                                        exception
                                );
                            }
                        }
                    }
            );
        }

        tripRepository.deleteById(tripId);
    }

    // ==================== Upload / Replace Trip Image ====================

    @Override
    @Transactional
    public TripResponseDTO uploadTripImage(
            Long tripId,
            MultipartFile image) {

        // 1. Check whether the trip exists
        Trip trip = tripRepository.findById(tripId)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Trip not found with ID: " + tripId
                        )
                );

        // 2. Upload new image to Cloudinary
        // Handle IOException because uploadImage may throw it.
        ImageUploadResult uploaded;

        try {
            uploaded = imageUploadService.uploadImage(image, "trips");

        } catch (IOException exception) {

            log.error(
                    "Failed to upload trip image to Cloudinary",
                    exception
            );

            throw new RuntimeException(
                    "Failed to upload trip image",
                    exception
            );
        }

        String oldPublicId = trip.getTripImagePublicId();
        boolean rollbackCleanupRegistered = registerTripImageSynchronization(
                uploaded.publicId(),
                oldPublicId
        );

        try {

            // 3. Update image information
            trip.setTripImageUrl(uploaded.secureUrl());
            trip.setTripImagePublicId(uploaded.publicId());

            // 4. Save and flush database changes
            Trip savedTrip = tripRepository.saveAndFlush(trip);

            return tripMapper.toDto(savedTrip);

        } catch (RuntimeException exception) {

            // The rollback callback owns cleanup when it was registered.
            if (!rollbackCleanupRegistered) {
                deleteNewCloudinaryImage(uploaded.publicId());
            }

            throw exception;
        }
    }

    private boolean registerTripImageSynchronization(
            String newPublicId,
            String oldPublicId) {

        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            return false;
        }

        TransactionSynchronizationManager.registerSynchronization(
                new TransactionSynchronization() {

                    @Override
                    public void afterCommit() {
                        if (oldPublicId != null
                                && !oldPublicId.isBlank()
                                && !oldPublicId.equals(newPublicId)) {

                            try {
                                imageUploadService.deleteImage(oldPublicId);

                            } catch (Exception exception) {
                                log.error(
                                        "Failed to delete old Cloudinary image: {}",
                                        oldPublicId,
                                        exception
                                );
                            }
                        }
                    }

                    @Override
                    public void afterCompletion(int status) {
                        if (status != STATUS_COMMITTED) {
                            deleteNewCloudinaryImage(newPublicId);
                        }
                    }
                }
        );

        return true;
    }

    private void deleteNewCloudinaryImage(String publicId) {
        try {
            imageUploadService.deleteImage(publicId);

        } catch (Exception exception) {
            log.error(
                    "Failed to clean up new Cloudinary image: {}",
                    publicId,
                    exception
            );
        }
    }

    // ==================== Guides Management ====================

    @Override
    @Transactional(readOnly = true)
    public List<PendingGuideDTO> getPendingGuides() {

        return guideRepository.findByApprovalStatus("Pending")
                .stream()
                .map(guideMapper::toPendingGuideDto)
                .toList();
    }

    @Override
    @Transactional
    public void processGuideApproval(
            Long guideId,
            String status) {

        if (!"Approved".equalsIgnoreCase(status)
                && !"Rejected".equalsIgnoreCase(status)) {

            throw new IllegalArgumentException(
                    "Invalid status. Must be 'Approved' or 'Rejected'"
            );
        }

        Guide guide = guideRepository.findById(guideId)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Guide not found with ID: " + guideId
                        )
                );

        guide.setApprovalStatus(status);

        guideRepository.save(guide);
    }

    @Override
    @Transactional
    public void deleteGuide(Long guideId) {

        Guide guide = guideRepository.findById(guideId)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Guide not found with ID: " + guideId
                        )
                );

        // Soft delete / suspension
        guide.setApprovalStatus("Suspended");

        guideRepository.save(guide);
    }

    // ==================== Direct Guide Bookings ====================

    @Override
    @Transactional(readOnly = true)
    public List<BookingResponseDTO> getGuideBookings() {

        return bookingRepository.findAllGuideBookings()
                .stream()
                .map(bookingMapper::toDto)
                .toList();
    }

    // ==================== Dashboard Statistics ====================

    @Override
    @Transactional(readOnly = true)
    public AdminDashboardStatsDTO getDashboardStats() {

        long totalPersons = personRepository.count();

        long totalTourists = touristRepository.count();

        long totalGuides =
                guideRepository.countByApprovalStatus("Approved");

        long pendingGuides =
                guideRepository.countByApprovalStatus("Pending");

        long totalTrips = tripRepository.count();

        long totalBookings = bookingRepository.count();

        BigDecimal totalEarnings =
                earningRepository.sumTotalAdminEarnings();

        if (totalEarnings == null) {
            totalEarnings = BigDecimal.ZERO;
        }

        return new AdminDashboardStatsDTO(
                totalPersons,
                totalTourists,
                totalGuides,
                pendingGuides,
                totalTrips,
                totalBookings,
                totalEarnings
        );
    }
}