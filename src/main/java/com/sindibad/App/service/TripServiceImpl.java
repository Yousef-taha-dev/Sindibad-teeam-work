package com.sindibad.App.service;

import com.sindibad.App.dto.TripResponseDTO;
import com.sindibad.App.entity.Trip;
import com.sindibad.App.exception.ResourceNotFoundException;
import com.sindibad.App.mapper.TripMapper;
import com.sindibad.App.repository.TripRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class TripServiceImpl implements TripService {

    private final TripRepository tripRepository;
    private final TripMapper tripMapper;

    public TripServiceImpl(
            TripRepository tripRepository,
            TripMapper tripMapper) {

        this.tripRepository = tripRepository;
        this.tripMapper = tripMapper;
    }

    // ==================== Get All Active Trips ====================

    @Override
    @Transactional(readOnly = true)
    public List<TripResponseDTO> getAllActiveTrips() {

        return tripRepository.findByTripStatus("Upcoming")
                .stream()
                .map(tripMapper::toDto)
                .toList();
    }

    // ==================== Get Trip By ID ====================

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

    // ==================== Get Trips By Category ====================

    @Override
    @Transactional(readOnly = true)
    public List<TripResponseDTO> getTripsByCategory(Long categoryId) {

        return tripRepository.findByCategoryCategoryId(categoryId)
                .stream()
                .map(tripMapper::toDto)
                .toList();
    }

    // ==================== Get Trips By Guide ====================

    @Override
    @Transactional(readOnly = true)
    public List<TripResponseDTO> getTripsByGuideId(Long guideId) {

        return tripRepository.findByGuideGuideId(guideId)
                .stream()
                .map(tripMapper::toDto)
                .toList();
    }

    // ==================== Search Trips ====================

    @Override
    @Transactional(readOnly = true)
    public List<TripResponseDTO> searchTrips(String keyword) {

        return tripRepository.findByTitleContainingIgnoreCase(keyword)
                .stream()
                .map(tripMapper::toDto)
                .toList();
    }
}