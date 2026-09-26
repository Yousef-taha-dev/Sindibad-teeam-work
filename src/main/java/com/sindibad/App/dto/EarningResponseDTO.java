package com.sindibad.App.dto;

import java.math.BigDecimal;

public record EarningResponseDTO(
        Long earningId,
        Long paymentId,
        Long guideId,
        String guideName,
        BigDecimal adminShare,
        BigDecimal guideShare
) {}