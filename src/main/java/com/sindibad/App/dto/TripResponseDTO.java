package com.sindibad.App.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record TripResponseDTO(
        Long tripId,
        Long guideId,
        String guideName,
        Long categoryId,
        String categoryName,
        String title,
        String description,
        LocalDateTime startDate,
        LocalDateTime endDate,
        Integer maxParticipants,
        BigDecimal pricePerPerson,
        String tripStatus,
        String tripImageUrl
) {}