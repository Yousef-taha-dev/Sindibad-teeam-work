package com.sindibad.App.controller;

import com.sindibad.App.dto.*;
import com.sindibad.App.service.AdminService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin")
public class AdminController {

    private final AdminService adminService;

    public AdminController(AdminService adminService) {
        this.adminService = adminService;
    }

    // ----------------- Dashboard Statistics -----------------

    // GET /api/admin/dashboard/stats
    @GetMapping("/dashboard/stats")
    public ResponseEntity<AdminDashboardStatsDTO> getDashboardStats() {
        return ResponseEntity.ok(adminService.getDashboardStats());
    }

    // ----------------- Trips Management -----------------

    // POST /api/admin/trips
    @PostMapping("/trips")
    public ResponseEntity<TripResponseDTO> createTrip(
            @Valid @RequestBody TripRequestDTO tripRequestDTO) {

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(adminService.createTrip(tripRequestDTO));
    }

    // PUT /api/admin/trips/{id}
    @PutMapping("/trips/{id}")
    public ResponseEntity<TripResponseDTO> updateTrip(
            @PathVariable Long id,
            @Valid @RequestBody TripRequestDTO tripRequestDTO) {

        return ResponseEntity.ok(
                adminService.updateTrip(id, tripRequestDTO)
        );
    }

    // GET /api/admin/trips/{id}
    @GetMapping("/trips/{id}")
    public ResponseEntity<TripResponseDTO> getTripById(
            @PathVariable Long id) {

        return ResponseEntity.ok(
                adminService.getTripById(id)
        );
    }

    // DELETE /api/admin/trips/{id}
    @DeleteMapping("/trips/{id}")
    public ResponseEntity<Void> deleteTrip(
            @PathVariable Long id) {

        adminService.deleteTrip(id);

        return ResponseEntity.noContent().build();
    }

    // ----------------- Guides Management -----------------

    // GET /api/admin/guides/pending
    @GetMapping("/guides/pending")
    public ResponseEntity<List<PendingGuideDTO>> getPendingGuides() {

        return ResponseEntity.ok(
                adminService.getPendingGuides()
        );
    }

    // PUT /api/admin/guides/{id}/approval?status=Approved
    @PutMapping("/guides/{id}/approval")
    public ResponseEntity<Void> processGuideApproval(
            @PathVariable Long id,
            @RequestParam String status) {

        adminService.processGuideApproval(id, status);

        return ResponseEntity.ok().build();
    }

    // DELETE /api/admin/guides/{id}
    @DeleteMapping("/guides/{id}")
    public ResponseEntity<Void> deleteGuide(
            @PathVariable Long id) {

        adminService.deleteGuide(id);

        return ResponseEntity.noContent().build();
    }

    // ----------------- Direct Guide Bookings -----------------

    // GET /api/admin/bookings/guide-direct
    @GetMapping("/bookings/guide-direct")
    public ResponseEntity<List<BookingResponseDTO>> getGuideBookings() {

        return ResponseEntity.ok(
                adminService.getGuideBookings()
        );
    }
}

