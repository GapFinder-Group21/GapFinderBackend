package com.backend.gapfinder.model.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.backend.gapfinder.model.enums.MatchStatusEnum;
import com.backend.gapfinder.model.exceptions.ForbiddenException;
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

import java.util.Optional;

class MatchServiceCancelTest {

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
    void proposerCancelsAPendingRequestAndItEndsRejected() {
        MatchModel result = service.cancelMatch(100L, 1L);

        assertEquals(MatchStatusEnum.REJECTED, result.getStatus());
        verify(matchRepository).save(match);
    }

    @Test
    void theReceiverCannotCancelTheRequest() {
        assertThrows(ForbiddenException.class, () -> service.cancelMatch(100L, 2L));
        assertEquals(MatchStatusEnum.PENDING, match.getStatus());
        verify(matchRepository, never()).save(any(MatchModel.class));
    }

    @Test
    void aRequestThatIsNoLongerPendingCannotBeCancelled() {
        match.setStatus(MatchStatusEnum.ACCEPTED);

        IllegalStateException error = assertThrows(IllegalStateException.class, () -> service.cancelMatch(100L, 1L));

        assertEquals("Solo se puede cancelar una solicitud que sigue pendiente", error.getMessage());
        assertEquals(MatchStatusEnum.ACCEPTED, match.getStatus());
    }
}
