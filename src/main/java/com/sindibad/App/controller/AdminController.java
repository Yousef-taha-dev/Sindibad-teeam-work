package com.sindibad.App.controller;

import com.sindibad.App.dto.*;
import com.sindibad.App.service.AdminService;
import com.sindibad.App.service.TripCategoryService;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/admin")
public class AdminController {

    private final AdminService adminService;
    private final TripCategoryService tripCategoryService;

    public AdminController(
            AdminService adminService,
            TripCategoryService tripCategoryService) {
        this.adminService = adminService;
        this.tripCategoryService = tripCategoryService;
    }


    // ----------------- Dashboard Statistics -----------------

    // GET /api/admin/dashboard/stats
    @GetMapping("/dashboard/stats")
    public ResponseEntity<AdminDashboardStatsDTO> getDashboardStats() {

        return ResponseEntity.ok(
                adminService.getDashboardStats()
        );
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


    // POST /api/admin/trips/{id}/image
    @PostMapping(
            value = "/trips/{id}/image",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE
    )
    public ResponseEntity<TripResponseDTO> uploadTripImage(
            @PathVariable Long id,
            @RequestParam("image") MultipartFile image) {

        return ResponseEntity.ok(
                adminService.uploadTripImage(id, image)
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


    // GET /api/admin/trips
    @GetMapping("/trips")
    public ResponseEntity<List<TripResponseDTO>> getAllTrips() {

        return ResponseEntity.ok(
                adminService.getAllTrips()
        );
    }


    // DELETE /api/admin/trips/{id}
    @DeleteMapping("/trips/{id}")
    public ResponseEntity<Void> deleteTrip(
            @PathVariable Long id) {

        adminService.deleteTrip(id);

        return ResponseEntity.noContent().build();
    }


    // ----------------- Categories Management -----------------

    // POST /api/admin/categories
    @PostMapping("/categories")
    public ResponseEntity<TripCategoryDTO> createCategory(
            @RequestBody TripCategoryDTO categoryDTO) {

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(tripCategoryService.createCategory(categoryDTO));
    }


    // PUT /api/admin/categories/{categoryId}
    @PutMapping("/categories/{categoryId}")
    public ResponseEntity<TripCategoryDTO> updateCategory(
            @PathVariable Long categoryId,
            @RequestBody TripCategoryDTO categoryDTO) {

        return ResponseEntity.ok(
                tripCategoryService.updateCategory(categoryId, categoryDTO)
        );
    }


    // DELETE /api/admin/categories/{categoryId}
    @DeleteMapping("/categories/{categoryId}")
    public ResponseEntity<Void> deleteCategory(
            @PathVariable Long categoryId) {

        tripCategoryService.deleteCategory(categoryId);

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