package com.sindibad.App.service;

import com.sindibad.App.dto.EarningResponseDTO;
import com.sindibad.App.entity.Earning;
import com.sindibad.App.entity.Guide;
import com.sindibad.App.entity.Person;
import com.sindibad.App.mapper.EarningMapper;
import com.sindibad.App.repository.EarningRepository;
import com.sindibad.App.repository.GuideRepository;
import com.sindibad.App.repository.PersonRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EarningGuideOwnershipTest {

    @Mock
    private EarningRepository earningRepository;
    @Mock
    private EarningMapper earningMapper;
    @Mock
    private PersonRepository personRepository;
    @Mock
    private GuideRepository guideRepository;

    private EarningServiceImpl earningService;

    @BeforeEach
    void setUp() {
        earningService = new EarningServiceImpl(
                earningRepository,
                earningMapper,
                personRepository,
                guideRepository
        );
    }

    @Test
    void guideCanReadOwnEarnings() {
        Earning earning = new Earning();
        earning.setEarningId(5L);
        EarningResponseDTO response = new EarningResponseDTO(
                5L,
                15L,
                9L,
                "Lina Haddad",
                new BigDecimal("3.00"),
                new BigDecimal("17.00")
        );

        when(personRepository.findByEmail("lina@example.com"))
                .thenReturn(Optional.of(person(3L)));
        when(guideRepository.findByPersonPersonId(3L))
                .thenReturn(Optional.of(guide(9L, 3L)));
        when(earningRepository.findByGuideGuideId(9L))
                .thenReturn(List.of(earning));
        when(earningMapper.toDto(earning)).thenReturn(response);

        List<EarningResponseDTO> result = earningService.getEarningsByGuideId(
                9L,
                "lina@example.com",
                false
        );

        assertEquals(1, result.size());
        assertSame(response, result.get(0));
        verify(earningRepository).findByGuideGuideId(9L);
        verify(earningMapper).toDto(earning);
    }

    @Test
    void guideCanReadOwnEarningsTotal() {
        when(personRepository.findByEmail("lina@example.com"))
                .thenReturn(Optional.of(person(3L)));
        when(guideRepository.findByPersonPersonId(3L))
                .thenReturn(Optional.of(guide(9L, 3L)));
        when(earningRepository.sumTotalGuideEarningsByGuideId(9L))
                .thenReturn(new BigDecimal("17.00"));

        BigDecimal total = earningService.getTotalEarningsByGuideId(
                9L,
                "lina@example.com",
                false
        );

        assertEquals(new BigDecimal("17.00"), total);
        verify(earningRepository).sumTotalGuideEarningsByGuideId(9L);
    }

    @Test
    void guideCannotReadAnotherGuidesEarnings() {
        when(personRepository.findByEmail("lina@example.com"))
                .thenReturn(Optional.of(person(3L)));
        when(guideRepository.findByPersonPersonId(3L))
                .thenReturn(Optional.of(guide(9L, 3L)));

        assertThrows(
                AccessDeniedException.class,
                () -> earningService.getEarningsByGuideId(
                        4L,
                        "lina@example.com",
                        false
                )
        );

        verify(earningRepository, never()).findByGuideGuideId(any());
        verify(earningMapper, never()).toDto(any());
    }

    @Test
    void guideCannotReadAnotherGuidesTotal() {
        when(personRepository.findByEmail("lina@example.com"))
                .thenReturn(Optional.of(person(3L)));
        when(guideRepository.findByPersonPersonId(3L))
                .thenReturn(Optional.of(guide(9L, 3L)));

        assertThrows(
                AccessDeniedException.class,
                () -> earningService.getTotalEarningsByGuideId(
                        4L,
                        "lina@example.com",
                        false
                )
        );

        verify(earningRepository, never()).sumTotalGuideEarningsByGuideId(any());
    }

    @Test
    void adminCanReadAnotherGuidesEarnings() {
        Earning earning = new Earning();
        EarningResponseDTO response = new EarningResponseDTO(
                5L,
                15L,
                4L,
                "Omar Nasser",
                new BigDecimal("3.00"),
                new BigDecimal("17.00")
        );

        when(earningRepository.findByGuideGuideId(4L))
                .thenReturn(List.of(earning));
        when(earningMapper.toDto(earning)).thenReturn(response);

        List<EarningResponseDTO> result = earningService.getEarningsByGuideId(
                4L,
                "admin@example.com",
                true
        );

        assertEquals(1, result.size());
        assertSame(response, result.get(0));
        verify(personRepository, never()).findByEmail(any());
        verify(guideRepository, never()).findByPersonPersonId(any());
        verify(earningRepository).findByGuideGuideId(4L);
    }

    @Test
    void adminCanReadAnotherGuidesTotal() {
        when(earningRepository.sumTotalGuideEarningsByGuideId(4L))
                .thenReturn(BigDecimal.ZERO);

        BigDecimal total = earningService.getTotalEarningsByGuideId(
                4L,
                "admin@example.com",
                true
        );

        assertEquals(BigDecimal.ZERO, total);
        verify(personRepository, never()).findByEmail(any());
        verify(guideRepository, never()).findByPersonPersonId(any());
        verify(earningRepository).sumTotalGuideEarningsByGuideId(4L);
    }

    @Test
    void blankEmailIsRejected() {
        assertThrows(
                AccessDeniedException.class,
                () -> earningService.getEarningsByGuideId(9L, null, false)
        );
        assertThrows(
                AccessDeniedException.class,
                () -> earningService.getEarningsByGuideId(9L, "  ", false)
        );
        assertThrows(
                AccessDeniedException.class,
                () -> earningService.getTotalEarningsByGuideId(9L, null, false)
        );
        assertThrows(
                AccessDeniedException.class,
                () -> earningService.getTotalEarningsByGuideId(9L, "  ", false)
        );

        verify(personRepository, never()).findByEmail(any());
        verify(earningRepository, never()).findByGuideGuideId(any());
        verify(earningRepository, never()).sumTotalGuideEarningsByGuideId(any());
    }

    @Test
    void missingPersonIsRejected() {
        when(personRepository.findByEmail("lina@example.com"))
                .thenReturn(Optional.empty());

        assertThrows(
                AccessDeniedException.class,
                () -> earningService.getEarningsByGuideId(
                        9L,
                        "lina@example.com",
                        false
                )
        );
        assertThrows(
                AccessDeniedException.class,
                () -> earningService.getTotalEarningsByGuideId(
                        9L,
                        "lina@example.com",
                        false
                )
        );

        verify(guideRepository, never()).findByPersonPersonId(any());
        verify(earningRepository, never()).findByGuideGuideId(any());
        verify(earningRepository, never()).sumTotalGuideEarningsByGuideId(any());
    }

    @Test
    void missingGuideProfileIsRejected() {
        when(personRepository.findByEmail("lina@example.com"))
                .thenReturn(Optional.of(person(3L)));
        when(guideRepository.findByPersonPersonId(3L))
                .thenReturn(Optional.empty());

        assertThrows(
                AccessDeniedException.class,
                () -> earningService.getEarningsByGuideId(
                        9L,
                        "lina@example.com",
                        false
                )
        );
        assertThrows(
                AccessDeniedException.class,
                () -> earningService.getTotalEarningsByGuideId(
                        9L,
                        "lina@example.com",
                        false
                )
        );

        verify(earningRepository, never()).findByGuideGuideId(any());
        verify(earningRepository, never()).sumTotalGuideEarningsByGuideId(any());
    }

    @Test
    void nullGuideIdIsRejected() {
        when(personRepository.findByEmail("lina@example.com"))
                .thenReturn(Optional.of(person(3L)));
        when(guideRepository.findByPersonPersonId(3L))
                .thenReturn(Optional.of(guide(9L, 3L)));

        assertThrows(
                AccessDeniedException.class,
                () -> earningService.getEarningsByGuideId(
                        null,
                        "lina@example.com",
                        false
                )
        );

        verify(earningRepository, never()).findByGuideGuideId(any());
        verify(earningRepository, never()).sumTotalGuideEarningsByGuideId(any());
    }

    private Person person(Long personId) {
        Person person = new Person();
        person.setPersonId(personId);
        person.setEmail("lina@example.com");
        return person;
    }

    private Guide guide(Long guideId, Long personId) {
        Guide guide = new Guide();
        guide.setGuideId(guideId);
        guide.setPerson(person(personId));
        return guide;
    }
}
