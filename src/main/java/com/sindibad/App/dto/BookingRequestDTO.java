package com.sindibad.App.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record BookingRequestDTO(
        Long tripId,
        Long guideId,

        @NotNull(message = "Tourist ID is required")
        Long touristId,

        @NotNull(message = "Number of guests is required")
        @Min(value = 1, message = "Guests count must be at least 1")
        Integer numberOfGuests
) {}