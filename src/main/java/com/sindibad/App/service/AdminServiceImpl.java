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
import org.springframework.boot.context.config.ConfigDataResourceNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

import static java.util.stream.Collectors.toList;

@Service
public class AdminServiceImpl implements  AdminService {


    private final PersonRepository personRepository;
    private final GuideRepository guideRepository;
    private final TouristRepository touristRepository;
    private final TripRepository tripRepository;
    private final TripCategoryRepository tripCategoryRepository ;
    private final BookingRepository bookingRepository;
    private final EarningRepository earningRepository;

    private final TripMapper tripMapper;
    private final GuideMapper guideMapper;
    private  final BookingMapper bookingMapper;


    public AdminServiceImpl
            (PersonRepository personRepository,

             GuideRepository guideRepository,

             TouristRepository touristRepository,

             TripRepository tripRepository,

             TripCategoryRepository tripCategoryRepository,

             BookingRepository bookingRepository,

             EarningRepository earningRepository,

             TripMapper tripMapper,

             GuideMapper guideMapper,

            BookingMapper bookingMapper

            ) {

        this.personRepository = personRepository;
        this.guideRepository = guideRepository;
        this.touristRepository = touristRepository;
        this.tripRepository = tripRepository;
        this.tripCategoryRepository = tripCategoryRepository;
        this.bookingRepository = bookingRepository;
        this.earningRepository = earningRepository;
        this.tripMapper = tripMapper;
        this.guideMapper = guideMapper;
        this.bookingMapper=bookingMapper;
    }



    @Override
    public TripResponseDTO createTrip(TripRequestDTO tripDTO) {
        if (tripDTO.endDate().isBefore(tripDTO.startDate())){
            throw new IllegalArgumentException("End date cannot be before start date");
        }

        Guide guide = guideRepository.findById(tripDTO.guideId())
                .orElseThrow(() -> new ResourceNotFoundException("Guide not found with ID: " + tripDTO.guideId()));

        if (!"Approved".equalsIgnoreCase(guide.getApprovalStatus())) {
            throw new IllegalArgumentException("Cannot assign trip to a guide who is not approved");
        }

        TripCategory tripCategory=tripCategoryRepository.findById(tripDTO.categoryId())
                .orElseThrow(()->new ResourceNotFoundException("Category not found with ID: " + tripDTO.categoryId()));


        Trip trip=tripMapper.toEntity(tripDTO);
        trip.setGuide(guide);
        trip.setCategory(tripCategory);

        Trip savedTrip=tripRepository.save(trip);
        return tripMapper.toDto(savedTrip);


    }

    @Override
    public TripResponseDTO updateTrip(Long tripId, TripRequestDTO tripDTO) {
        Trip trip =tripRepository.findById(tripId)
                .orElseThrow(()->new ResourceNotFoundException("Trip not found with ID:"+tripId));

        if(tripDTO.endDate().isBefore(tripDTO.startDate())){
            throw new IllegalArgumentException("End date cannot be before start date");

        }

        if(!trip.getGuide().getGuideId().equals(tripDTO.guideId())){
            Guide guide=guideRepository.findById(tripDTO.guideId())
                    .orElseThrow(()->new ResourceNotFoundException("Guide not found with ID: " + tripDTO.guideId()));

            if (!"Approved".equalsIgnoreCase(guide.getApprovalStatus())) {
                throw new IllegalArgumentException("Cannot assign trip to a guide who is not approved");
            }
            trip.setGuide(guide);
        }

        if (!trip.getCategory().getCategoryId().equals(tripDTO.categoryId())) {
            TripCategory category = tripCategoryRepository.findById(tripDTO.categoryId())
                    .orElseThrow(() -> new ResourceNotFoundException("Category not found with ID: " + tripDTO.categoryId()));
            trip.setCategory(category);
        }

        trip.setTitle(tripDTO.title());
        trip.setDescription(tripDTO.description());
        trip.setStartDate(tripDTO.startDate());
        trip.setEndDate(tripDTO.endDate());
        trip.setMaxParticipants(tripDTO.maxParticipants());
        trip.setPricePerPerson(tripDTO.pricePerPerson());
        trip.setTripImageUrl(tripDTO.tripImageUrl());

        Trip updatedTrip = tripRepository.save(trip);
        return tripMapper.toDto(updatedTrip);
    }
//-------------Trips Management---------------------
    @Override
    @Transactional(readOnly = true)
    public TripResponseDTO getTripById(Long tripId) {
        Trip trip=tripRepository.findByIdWithDetails(tripId)
                .orElseThrow(()->new ResourceNotFoundException("Trip not found with ID: " + tripId));
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
    public void deleteTrip(Long tripId) {
        if(!tripRepository.existsById(tripId))
        {
            throw  new ResourceNotFoundException("Trip not found with ID: " + tripId);
        }
        tripRepository.deleteById(tripId);
    }



    //--------------Guides Management--------------------
    @Override
    @Transactional(readOnly = true)
    public List<PendingGuideDTO> getPendingGuides() {
        return guideRepository.findByApprovalStatus("Pending")
                .stream()
                .map(guideMapper::toPendingGuideDto)
                .toList();
    }

    @Override
    public void processGuideApproval(Long guideId, String status) {
        if (!"Approved".equalsIgnoreCase(status) && !"Rejected".equalsIgnoreCase(status)) {
            throw new IllegalArgumentException("Invalid status. Must be 'Approved' or 'Rejected'");
        }

        Guide guide = guideRepository.findById(guideId)
                .orElseThrow(() -> new ResourceNotFoundException("Guide not found with ID: " + guideId));

        guide.setApprovalStatus(status);
        guideRepository.save(guide);
    }

    @Override
    public void deleteGuide(Long guideId) {
        Guide guide = guideRepository.findById(guideId)
                .orElseThrow(() -> new ResourceNotFoundException("Guide not found with ID: " + guideId));

        guide.setApprovalStatus("Suspended");
        guideRepository.save(guide);
    }

   //--------- Direct Guide Bookings Management--------------

    @Override
    @Transactional(readOnly = true)
    public List<BookingResponseDTO> getGuideBookings() {
        return bookingRepository.findAllGuideBookings()
                .stream()
                .map(bookingMapper::toDto)
                .toList();
    }

    //-------------- Dashboard Statistics--------------------

    @Override
    public AdminDashboardStatsDTO getDashboardStats() {
        long totalPersons = personRepository.count();
        long totalTourists = touristRepository.count();
        long totalGuides = guideRepository.countByApprovalStatus("Approved");
        long pendingGuides = guideRepository.countByApprovalStatus("Pending");
        long totalTrips = tripRepository.count();
        long totalBookings = bookingRepository.count();

        BigDecimal totalEarnings = earningRepository.sumTotalAdminEarnings();
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
