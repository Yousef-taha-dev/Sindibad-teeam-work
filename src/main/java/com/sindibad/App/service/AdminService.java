
        package com.sindibad.App.service;

import com.sindibad.App.dto.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface AdminService {

    // ==================== TRIPS ====================

    TripResponseDTO createTrip(TripRequestDTO tripDTO);

    TripResponseDTO updateTrip(Long tripId, TripRequestDTO tripDTO);

    TripResponseDTO getTripById(Long tripId);

    List<TripResponseDTO> getAllTrips();

    void deleteTrip(Long tripId);

    // Upload / Replace Trip Image
    TripResponseDTO uploadTripImage(Long tripId, MultipartFile image);


    // ==================== GUIDES ====================

    List<PendingGuideDTO> getPendingGuides();

    void processGuideApproval(Long guideId, String status);

    void deleteGuide(Long guideId);

    List<BookingResponseDTO> getGuideBookings();


    // ==================== DASHBOARD ====================

    AdminDashboardStatsDTO getDashboardStats();
}
