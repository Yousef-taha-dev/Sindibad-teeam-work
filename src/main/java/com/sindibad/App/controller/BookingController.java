package com.sindibad.App.controller;

import com.sindibad.App.dto.BookingRequestDTO;
import com.sindibad.App.dto.BookingResponseDTO;
import com.sindibad.App.service.BookingService;
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
@RequestMapping("/api/bookings")
@Tag(
        name = "Bookings",
        description = "Endpoints for managing tourist bookings"
)
@SecurityRequirement(name = "bearerAuth")
public class BookingController {

    private final BookingService bookingService;

    public BookingController(BookingService bookingService) {
        this.bookingService = bookingService;
    }

    // ==================== Create Booking ====================

    @Operation(
            summary = "Create a new booking",
            description = "Creates a new booking for a specific tourist."
    )
    @PostMapping("/tourist/{touristId}")
    @PreAuthorize("hasAnyRole('TOURIST', 'ADMIN')")
    public ResponseEntity<BookingResponseDTO> createBooking(
            @PathVariable Long touristId,
            @Valid @RequestBody BookingRequestDTO bookingDTO) {

        BookingResponseDTO response =
                bookingService.createBooking(
                        touristId,
                        bookingDTO
                );

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }

    // ==================== Get Booking ====================

    @Operation(
            summary = "Get booking by ID",
            description = "Returns the details of a specific booking."
    )
    @GetMapping("/{bookingId}")
    @PreAuthorize("hasAnyRole('TOURIST', 'GUIDE', 'ADMIN')")
    public ResponseEntity<BookingResponseDTO> getBookingById(
            @PathVariable Long bookingId) {

        return ResponseEntity.ok(
                bookingService.getBookingById(bookingId)
        );
    }

    // ==================== Get Tourist Bookings ====================

    @Operation(
            summary = "Get tourist bookings",
            description = "Returns all bookings belonging to a specific tourist."
    )
    @GetMapping("/tourist/{touristId}")
    @PreAuthorize("hasAnyRole('TOURIST', 'ADMIN')")
    public ResponseEntity<List<BookingResponseDTO>> getTouristBookings(
            @PathVariable Long touristId) {

        return ResponseEntity.ok(
                bookingService.getTouristBookings(touristId)
        );
    }

    // ==================== Cancel Booking ====================

    @Operation(
            summary = "Cancel booking",
            description = "Cancels an existing booking."
    )
    @PutMapping("/{bookingId}/cancel")
    @PreAuthorize("hasAnyRole('TOURIST', 'ADMIN')")
    public ResponseEntity<Void> cancelBooking(
            @PathVariable Long bookingId) {

        bookingService.cancelBooking(bookingId);

        return ResponseEntity.noContent().build();
    }
}