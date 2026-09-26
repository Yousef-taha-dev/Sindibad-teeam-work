package com.sindibad.App.dto;

import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record TripRequestDTO(
        @NotNull(message = "Guide ID is required")
        Long guideId,

        @NotNull(message = "Category ID is required")
        Long categoryId,

        @NotBlank(message = "Title is required")
        @Size(max = 200, message = "Title cannot exceed 200 characters")
        String title,

        @NotBlank(message = "Description is required")
        String description,

        @NotNull(message = "Start date is required")
        @Future(message = "Start date must be in the future")
        LocalDateTime startDate,

        @NotNull(message = "End date is required")
        @Future(message = "End date must be in the future")
        LocalDateTime endDate,

        @NotNull(message = "Max participants is required")
        @Min(value = 1, message = "Participants must be at least 1")
        Integer maxParticipants,

        @NotNull(message = "Price per person is required")
        @DecimalMin(value = "0.0", inclusive = false, message = "Price must be greater than 0")
        BigDecimal pricePerPerson,

        String tripImageUrl
) {}