package com.sindibad.App.service;

import com.sindibad.App.dto.EarningResponseDTO;

import java.util.List;

public interface EarningService {

    List<EarningResponseDTO> getEarningsByGuideId(Long guideId);

    List<EarningResponseDTO> getAllEarnings();
}