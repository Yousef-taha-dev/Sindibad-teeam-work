package com.sindibad.App.service;

import com.sindibad.App.dto.EarningResponseDTO;
import com.sindibad.App.mapper.EarningMapper;
import com.sindibad.App.repository.EarningRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class EarningServiceImpl implements EarningService {

    private final EarningRepository earningRepository;
    private final EarningMapper earningMapper;

    public EarningServiceImpl(EarningRepository earningRepository,
                              EarningMapper earningMapper) {

        this.earningRepository = earningRepository;
        this.earningMapper = earningMapper;
    }

    @Override
    @Transactional(readOnly = true)
    public List<EarningResponseDTO> getEarningsByGuideId(Long guideId) {

        return earningRepository
                .findByGuideGuideId(guideId)
                .stream()
                .map(earningMapper::toDto)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<EarningResponseDTO> getAllEarnings() {

        return earningRepository
                .findAll()
                .stream()
                .map(earningMapper::toDto)
                .toList();
    }
}