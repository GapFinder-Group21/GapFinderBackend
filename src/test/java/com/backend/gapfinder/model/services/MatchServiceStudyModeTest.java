package com.backend.gapfinder.model.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.backend.gapfinder.model.models.GapModel;
import com.backend.gapfinder.model.models.MatchModel;
import com.backend.gapfinder.model.models.UserModel;
import com.backend.gapfinder.model.repositories.GapRepository;
import com.backend.gapfinder.model.repositories.MatchRepository;
import com.backend.gapfinder.model.strategies.EffortStrategy;
import com.backend.gapfinder.model.strategies.OverlapStrategy;
import com.backend.gapfinder.model.strategies.SameCareerStrategy;
import com.backend.gapfinder.model.strategies.SharedInterestStrategy;
import com.backend.gapfinder.view.dto.responses.MatchCandidateResponseDTO;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.modelmapper.ModelMapper;
import org.springframework.context.ApplicationEventPublisher;

import java.time.LocalDateTime;
import java.util.List;

// Study mode hides a user from the matches: the candidates and the match requests respect it
class MatchServiceStudyModeTest {

    private MatchRepository matchRepository;
    private GapRepository gapRepository;
    private GapService gapService;
    private MatchService service;
    private LocalDateTime start;

    @BeforeEach
    void setUp() {
        matchRepository = mock(MatchRepository.class);
        gapRepository = mock(GapRepository.class);
        gapService = mock(GapService.class);
        // The constructor of MatchService is not changed by study mode (9 arguments)
        service = new MatchService(matchRepository, gapRepository, gapService,
                mock(OverlapStrategy.class), mock(SameCareerStrategy.class), mock(SharedInterestStrategy.class),
                mock(EffortStrategy.class), mock(ModelMapper.class), mock(ApplicationEventPublisher.class));

        start = LocalDateTime.now().plusDays(1);
        when(gapService.getById(10L)).thenReturn(gap(10L, 1L));
        when(gapService.getById(20L)).thenReturn(gap(20L, 2L));
        when(matchRepository.save(any(MatchModel.class))).thenAnswer(call -> call.getArgument(0));
    }

    private GapModel gap(Long gapId, Long userId) {
        UserModel user = new UserModel();
        user.setId(userId);
        GapModel gap = new GapModel();
        gap.setId(gapId);
        gap.setUser(user);
        gap.setStartTime(start);
        gap.setEndTime(start.plusHours(2));
        return gap;
    }

    @Test
    void aRequestToAGapInStudyModeIsABusinessRuleRefusal() {
        when(gapRepository.isGapInStudyPeriod(eq(20L), any(LocalDateTime.class))).thenReturn(true);

        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> service.sendMatchRequest(10L, 20L, 0.0));

        // Plain IllegalStateException answers 422, not 409
        assertEquals(IllegalStateException.class, ex.getClass());
        verify(matchRepository, never()).save(any());
    }

    @Test
    void aRequestToAGapThatIsNotInStudyModeIsSent() {
        when(gapRepository.isGapInStudyPeriod(eq(20L), any(LocalDateTime.class))).thenReturn(false);

        MatchModel saved = service.sendMatchRequest(10L, 20L, 0.0);

        assertNotNull(saved);
        verify(matchRepository).save(any(MatchModel.class));
    }

    @Test
    void withMyOwnGapInStudyModeThereAreNoCandidates() {
        when(gapRepository.isGapInStudyPeriod(eq(10L), any(LocalDateTime.class))).thenReturn(true);

        List<MatchCandidateResponseDTO> candidates = service.findCandidates(10L, false, false, false);

        assertEquals(0, candidates.size());
        // The search does not even ask for the overlapping gaps of the other users
        verify(gapRepository, never()).findOverlappingGapsExcludingUser(any(), any(), any(), any());
    }

    @Test
    void withMyOwnGapNotInStudyModeTheCandidatesAreSearched() {
        when(gapRepository.isGapInStudyPeriod(eq(10L), any(LocalDateTime.class))).thenReturn(false);
        when(gapRepository.findOverlappingGapsExcludingUser(eq(1L), any(), any(), any())).thenReturn(List.of());

        service.findCandidates(10L, false, false, false);

        verify(gapRepository).findOverlappingGapsExcludingUser(eq(1L), eq(start), eq(start.plusHours(2)), any());
    }
}
