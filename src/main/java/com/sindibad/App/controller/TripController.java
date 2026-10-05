package com.sindibad.App.controller;

import com.sindibad.App.dto.TripRequestDTO;
import com.sindibad.App.dto.TripResponseDTO;
import com.sindibad.App.service.TripService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/trips")
@Tag(
        name = "Trips",
        description = "Endpoints for browsing, searching, and managing trips"
)
@SecurityRequirement(name = "bearerAuth")
public class TripController {

    private final TripService tripService;

    public TripController(TripService tripService) {
        this.tripService = tripService;
    }

    // ==================== Get All Active Trips ====================

    @Operation(
            summary = "Get all active trips",
            description = "Returns all upcoming trips available for booking."
    )
    @GetMapping
    public ResponseEntity<List<TripResponseDTO>> getAllActiveTrips() {

        return ResponseEntity.ok(
                tripService.getAllActiveTrips()
        );
    }

    // ==================== Get Trip By ID ====================

    @Operation(
            summary = "Get trip by ID",
            description = "Returns details of a specific trip."
    )
    @GetMapping("/{tripId}")
    public ResponseEntity<TripResponseDTO> getTripById(
            @PathVariable Long tripId) {

        return ResponseEntity.ok(
                tripService.getTripById(tripId)
        );
    }

    // ==================== Get Trips By Category ====================

    @Operation(
            summary = "Get trips by category",
            description = "Returns all trips belonging to a specific category."
    )
    @GetMapping("/category/{categoryId}")
    public ResponseEntity<List<TripResponseDTO>> getTripsByCategory(
            @PathVariable Long categoryId) {

        return ResponseEntity.ok(
                tripService.getTripsByCategory(categoryId)
        );
    }

    // ==================== Get Trips By Guide ====================

    @Operation(
            summary = "Get trips By Guide",
            description = "Returns all trips assigned to a specific guide."
    )
    @GetMapping("/guide/{guideId}")
    public ResponseEntity<List<TripResponseDTO>> getTripsByGuideId(
            @PathVariable Long guideId) {

        return ResponseEntity.ok(
                tripService.getTripsByGuideId(guideId)
        );
    }

    // ==================== Search Trips ====================

    @Operation(
            summary = "Search trips",
            description = "Searches for trips by keyword or title."
    )
    @GetMapping("/search")
    public ResponseEntity<List<TripResponseDTO>> searchTrips(
            @RequestParam String keyword) {

        return ResponseEntity.ok(
                tripService.searchTrips(keyword)
        );
    }

    // ==================== Create Trip ====================

    @Operation(
            summary = "Create a new trip",
            description = "Allows the admin to create a group trip and assign an approved guide."
    )
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<TripResponseDTO> createTrip(
            @Valid @RequestBody TripRequestDTO tripRequestDTO) {

        TripResponseDTO response =
                tripService.createTrip(tripRequestDTO);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }

    // ==================== Update Trip ====================

    @Operation(
            summary = "Update an existing trip",
            description = "Allows the admin to update trip details and guide assignment."
    )
    @PutMapping("/{tripId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<TripResponseDTO> updateTrip(
            @PathVariable Long tripId,
            @Valid @RequestBody TripRequestDTO tripRequestDTO) {

        return ResponseEntity.ok(
                tripService.updateTrip(
                        tripId,
                        tripRequestDTO
                )
        );
    }

    // ==================== Cancel Trip ====================

    @Operation(
            summary = "Cancel a trip",
            description = "Allows the admin to cancel a scheduled trip."
    )
    @PutMapping("/{tripId}/cancel")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> cancelTrip(
            @PathVariable Long tripId) {

        tripService.cancelTrip(tripId);

        return ResponseEntity.noContent().build();
    }
}