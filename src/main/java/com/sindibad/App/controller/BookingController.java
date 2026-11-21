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
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
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
            @Valid @RequestBody BookingRequestDTO bookingDTO,
            Authentication authentication) {

        boolean admin = isAdmin(authentication);

        if (!admin
                && (authentication == null
                || authentication.getName() == null
                || authentication.getName().isBlank())) {
            throw new AccessDeniedException(
                    "You can only create your own booking."
            );
        }

        BookingResponseDTO response =
                bookingService.createBooking(
                        touristId,
                        bookingDTO,
                        authentication.getName(),
                        admin
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
            @PathVariable Long bookingId,
            Authentication authentication) {

        if (authentication == null
                || authentication.getName() == null
                || authentication.getName().isBlank()) {
            throw new AccessDeniedException(
                    "You can only view your own booking."
            );
        }

        return ResponseEntity.ok(
                bookingService.getBookingById(
                        bookingId,
                        authentication.getName(),
                        isAdmin(authentication),
                        isGuide(authentication)
                )
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
            @PathVariable Long touristId,
            Authentication authentication) {

        if (authentication == null
                || authentication.getName() == null
                || authentication.getName().isBlank()) {
            throw new AccessDeniedException(
                    "You can only view your own bookings."
            );
        }

        return ResponseEntity.ok(
                bookingService.getTouristBookings(
                        touristId,
                        authentication.getName(),
                        isAdmin(authentication)
                )
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
            @PathVariable Long bookingId,
            Authentication authentication) {

        if (authentication == null
                || authentication.getName() == null
                || authentication.getName().isBlank()) {
            throw new AccessDeniedException(
                    "You can only cancel your own booking."
            );
        }

        bookingService.cancelBooking(
                bookingId,
                authentication.getName(),
                isAdmin(authentication)
        );

        return ResponseEntity.noContent().build();
    }

    private boolean isAdmin(Authentication authentication) {

        if (authentication == null || authentication.getAuthorities() == null) {
            return false;
        }

        for (GrantedAuthority authority : authentication.getAuthorities()) {
            if ("ROLE_ADMIN".equals(authority.getAuthority())) {
                return true;
            }
        }

        return false;
    }

    private boolean isGuide(Authentication authentication) {

        if (authentication.getAuthorities() == null) {
            return false;
        }

        for (GrantedAuthority authority : authentication.getAuthorities()) {
            if ("ROLE_GUIDE".equals(authority.getAuthority())) {
                return true;
            }
        }

        return false;
    }
}