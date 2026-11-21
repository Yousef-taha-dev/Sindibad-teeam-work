package com.sindibad.App.service;

import com.sindibad.App.dto.EarningResponseDTO;
import com.sindibad.App.entity.Guide;
import com.sindibad.App.entity.Person;
import com.sindibad.App.mapper.EarningMapper;
import com.sindibad.App.repository.EarningRepository;
import com.sindibad.App.repository.GuideRepository;
import com.sindibad.App.repository.PersonRepository;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
public class EarningServiceImpl implements EarningService {

    private final EarningRepository earningRepository;
    private final EarningMapper earningMapper;
    private final PersonRepository personRepository;
    private final GuideRepository guideRepository;

    public EarningServiceImpl(EarningRepository earningRepository,
                              EarningMapper earningMapper,
                              PersonRepository personRepository,
                              GuideRepository guideRepository) {

        this.earningRepository = earningRepository;
        this.earningMapper = earningMapper;
        this.personRepository = personRepository;
        this.guideRepository = guideRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public List<EarningResponseDTO> getEarningsByGuideId(
            Long guideId,
            String actorEmail,
            boolean admin) {

        if (!admin) {
            assertGuideOwnsEarnings(guideId, actorEmail);
        }

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

    @Override
    @Transactional(readOnly = true)
    public BigDecimal getTotalEarningsByGuideId(
            Long guideId,
            String actorEmail,
            boolean admin) {

        if (!admin) {
            assertGuideOwnsEarnings(guideId, actorEmail);
        }

        return earningRepository.sumTotalGuideEarningsByGuideId(guideId);
    }

    private void assertGuideOwnsEarnings(
            Long requestedGuideId,
            String actorEmail) {

        if (actorEmail == null || actorEmail.isBlank()) {
            throw new AccessDeniedException(
                    "You can only view your own earnings."
            );
        }

        Person person = personRepository.findByEmail(actorEmail)
                .orElseThrow(() ->
                        new AccessDeniedException(
                                "You can only view your own earnings."
                        )
                );

        Guide actor = guideRepository
                .findByPersonPersonId(person.getPersonId())
                .orElseThrow(() ->
                        new AccessDeniedException(
                                "You can only view your own earnings."
                        )
                );

        if (actor.getGuideId() == null
                || requestedGuideId == null
                || !actor.getGuideId().equals(requestedGuideId)) {

            throw new AccessDeniedException(
                    "You can only view your own earnings."
            );
        }
    }
}