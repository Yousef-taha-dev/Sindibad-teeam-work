package com.sindibad.App.service;

import com.sindibad.App.dto.EarningResponseDTO;
import java.math.BigDecimal;
import java.util.List;

public interface EarningService {

    List<EarningResponseDTO> getAllEarnings();

    List<EarningResponseDTO> getEarningsByGuideId(
            Long guideId,
            String actorEmail,
            boolean admin);

    // أضف هذا السطر
    BigDecimal getTotalEarningsByGuideId(
            Long guideId,
            String actorEmail,
            boolean admin);
}