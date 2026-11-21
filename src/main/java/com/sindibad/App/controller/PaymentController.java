package com.sindibad.App.controller;

import com.sindibad.App.dto.PaymentRequestDTO;
import com.sindibad.App.dto.PaymentResponseDTO;
import com.sindibad.App.service.PaymentService;
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
@RequestMapping("/api/payments")
@Tag(
        name = "Payments",
        description = "Endpoints for processing and retrieving payments"
)
@SecurityRequirement(name = "bearerAuth")
public class PaymentController {

    private final PaymentService paymentService;

    public PaymentController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    // ==================== Process Payment ====================

    @Operation(
            summary = "Process a payment",
            description = "Processes payment for a booking, confirms the booking, and records earnings."
    )
    @PostMapping
    @PreAuthorize("hasAnyRole('TOURIST', 'ADMIN')")
    public ResponseEntity<PaymentResponseDTO> processPayment(
            @Valid @RequestBody PaymentRequestDTO paymentRequestDTO,
            Authentication authentication) {

        if (authentication == null
                || authentication.getName() == null
                || authentication.getName().isBlank()) {
            throw new AccessDeniedException(
                    "You can only pay for your own booking."
            );
        }

        PaymentResponseDTO response =
                paymentService.processPayment(
                        paymentRequestDTO,
                        authentication.getName(),
                        isAdmin(authentication)
                );

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }

    private boolean isAdmin(Authentication authentication) {

        if (authentication.getAuthorities() == null) {
            return false;
        }

        for (GrantedAuthority authority : authentication.getAuthorities()) {
            if ("ROLE_ADMIN".equals(authority.getAuthority())) {
                return true;
            }
        }

        return false;
    }

    // ==================== Get Payment By ID ====================

    @Operation(
            summary = "Get payment by ID",
            description = "Returns the details of a specific payment by its ID."
    )
    @GetMapping("/{paymentId}")
    @PreAuthorize("hasAnyRole('TOURIST', 'ADMIN')")
    public ResponseEntity<PaymentResponseDTO> getPaymentById(
            @PathVariable Long paymentId,
            Authentication authentication) {

        if (authentication == null
                || authentication.getName() == null
                || authentication.getName().isBlank()) {
            throw new AccessDeniedException(
                    "You can only view your own payment."
            );
        }

        return ResponseEntity.ok(
                paymentService.getPaymentById(
                        paymentId,
                        authentication.getName(),
                        isAdmin(authentication)
                )
        );
    }

    // ==================== Get Payment By Booking ====================

    @Operation(
            summary = "Get payment by booking",
            description = "Returns the payment details associated with a specific booking."
    )
    @GetMapping("/booking/{bookingId}")
    @PreAuthorize("hasAnyRole('TOURIST', 'ADMIN')")
    public ResponseEntity<PaymentResponseDTO> getPaymentByBookingId(
            @PathVariable Long bookingId,
            Authentication authentication) {

        if (authentication == null
                || authentication.getName() == null
                || authentication.getName().isBlank()) {
            throw new AccessDeniedException(
                    "You can only view your own payment."
            );
        }

        return ResponseEntity.ok(
                paymentService.getPaymentByBookingId(
                        bookingId,
                        authentication.getName(),
                        isAdmin(authentication)
                )
        );
    }

    // ==================== Get All Payments ====================

    @Operation(
            summary = "Get all payments",
            description = "Returns all payments logged in the system (Admin only)."
    )
    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<PaymentResponseDTO>> getAllPayments() {

        return ResponseEntity.ok(
                paymentService.getAllPayments()
        );
    }
}