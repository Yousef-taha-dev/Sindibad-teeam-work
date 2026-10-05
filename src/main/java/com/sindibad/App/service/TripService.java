package com.sindibad.App.service;

import com.sindibad.App.dto.TripRequestDTO;
import com.sindibad.App.dto.TripResponseDTO;

import java.util.List;

public interface TripService {

    List<TripResponseDTO> getAllActiveTrips();

    TripResponseDTO getTripById(Long tripId);

    List<TripResponseDTO> getTripsByCategory(Long categoryId);

    List<TripResponseDTO> getTripsByGuideId(Long guideId);

    List<TripResponseDTO> searchTrips(String keyword);

    TripResponseDTO createTrip(TripRequestDTO tripRequestDTO);

    TripResponseDTO updateTrip(Long tripId, TripRequestDTO tripRequestDTO);

    void cancelTrip(Long tripId);
}