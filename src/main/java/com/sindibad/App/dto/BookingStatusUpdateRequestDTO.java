package com.sindibad.App.dto;

import jakarta.validation.constraints.NotBlank;

public record BookingStatusUpdateRequestDTO(
        @NotBlank(message = "Booking status is required")
        String bookingStatus
) {}