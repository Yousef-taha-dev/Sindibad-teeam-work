package com.sindibad.App.dto;

public record ReviewResponseDTO(
        Long reviewId,
        Long bookingId,
        String touristName,
        String tripTitle,
        Integer rating,
        String comment
) {}