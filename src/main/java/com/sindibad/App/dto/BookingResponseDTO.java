package com.sindibad.App.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record BookingResponseDTO(
        Long bookingId,
        String bookingType,
        Long tripId,
        String tripTitle,
        Long guideId,
        String guideName,
        Long touristId,
        String touristName,
        LocalDateTime bookingDate,
        Integer numberOfGuests,
        BigDecimal totalPrice,
        String bookingStatus
) {}