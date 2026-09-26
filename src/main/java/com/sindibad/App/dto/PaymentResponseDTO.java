package com.sindibad.App.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record PaymentResponseDTO(
        Long paymentId,
        Long bookingId,
        BigDecimal amount,
        String paymentMethod,
        String paymentStatus,
        LocalDateTime paidAt
) {}