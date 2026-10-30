package com.backend.gapfinder.model.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.backend.gapfinder.model.enums.MatchStatusEnum;
import com.backend.gapfinder.model.models.GapModel;
import com.backend.gapfinder.model.models.MatchModel;
import com.backend.gapfinder.model.models.UserModel;
import com.backend.gapfinder.model.repositories.GapRepository;
import com.backend.gapfinder.model.repositories.MatchRepository;
import com.backend.gapfinder.model.strategies.EffortStrategy;
import com.backend.gapfinder.model.strategies.OverlapStrategy;
import com.backend.gapfinder.model.strategies.SameCareerStrategy;
import com.backend.gapfinder.model.strategies.SharedInterestStrategy;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.modelmapper.ModelMapper;
import org.springframework.context.ApplicationEventPublisher;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

// BQ 10: respondedAt is only set when a user accepts or rejects, never when the request expires or is cancelled
class MatchServiceRespondedAtTest {

    private MatchRepository matchRepository;
    private MatchService service;
    private MatchModel match;

    @BeforeEach
    void setUp() {
        matchRepository = mock(MatchRepository.class);
        service = new MatchService(matchRepository, mock(GapRepository.class), mock(GapService.class),
                mock(OverlapStrategy.class), mock(SameCareerStrategy.class), mock(SharedInterestStrategy.class),
                mock(EffortStrategy.class), mock(ModelMapper.class), mock(ApplicationEventPublisher.class));

        match = new MatchModel();
        match.setId(100L);
        match.setProposerGap(gap(10L, 1L));
        match.setAcceptorGap(gap(20L, 2L));
        match.setStartTime(LocalDateTime.now().plusHours(1));
        match.setEndTime(LocalDateTime.now().plusHours(2));
        match.setStatus(MatchStatusEnum.PENDING);

        when(matchRepository.findById(100L)).thenReturn(Optional.of(match));
        when(matchRepository.save(any(MatchModel.class))).thenAnswer(call -> call.getArgument(0));
    }

    private static GapModel gap(Long gapId, Long userId) {
        UserModel user = new UserModel();
        user.setId(userId);
        GapModel gap = new GapModel();
        gap.setId(gapId);
        gap.setUser(user);
        return gap;
    }

    @Test
    void rejectingSetsRespondedAt() {
        MatchModel saved = service.rejectMatch(100L, 2L);

        assertEquals(MatchStatusEnum.REJECTED, saved.getStatus());
        assertNotNull(saved.getRespondedAt());
    }

    @Test
    void acceptingSetsRespondedAt() {
        MatchModel saved = service.acceptMatch(100L, 2L);

        assertEquals(MatchStatusEnum.ACCEPTED, saved.getStatus());
        assertNotNull(saved.getRespondedAt());
    }

    @Test
    void cancellingLeavesRespondedAtNull() {
        MatchModel saved = service.cancelMatch(100L, 1L);

        assertEquals(MatchStatusEnum.REJECTED, saved.getStatus());
        assertNull(saved.getRespondedAt());
    }

    @Test
    void expiringAPendingRequestLeavesRespondedAtNull() {
        match.setEndTime(LocalDateTime.now().minusMinutes(1));
        when(matchRepository.findByStatus(MatchStatusEnum.ACCEPTED)).thenReturn(List.of());
        when(matchRepository.findByStatus(MatchStatusEnum.PENDING)).thenReturn(List.of(match));

        service.completeExpiredMatches();

        assertEquals(MatchStatusEnum.REJECTED, match.getStatus());
        assertNull(match.getRespondedAt());
    }
}
