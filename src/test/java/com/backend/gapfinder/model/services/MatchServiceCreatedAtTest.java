package com.backend.gapfinder.model.services;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
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

import org.junit.jupiter.api.Test;
import org.modelmapper.ModelMapper;
import org.springframework.context.ApplicationEventPublisher;

import java.time.LocalDateTime;

// BQ 10 filters by when the request was made, so the match has to store it when it is created
class MatchServiceCreatedAtTest {

    private static GapModel gap(Long gapId, Long userId, LocalDateTime start, LocalDateTime end) {
        UserModel user = new UserModel();
        user.setId(userId);
        GapModel gap = new GapModel();
        gap.setId(gapId);
        gap.setUser(user);
        gap.setStartTime(start);
        gap.setEndTime(end);
        return gap;
    }

    @Test
    void sendingARequestStoresWhenItWasMade() {
        MatchRepository matchRepository = mock(MatchRepository.class);
        GapService gapService = mock(GapService.class);
        MatchService service = new MatchService(matchRepository, mock(GapRepository.class), gapService,
                mock(OverlapStrategy.class), mock(SameCareerStrategy.class), mock(SharedInterestStrategy.class),
                mock(EffortStrategy.class), mock(ModelMapper.class), mock(ApplicationEventPublisher.class));

        LocalDateTime start = LocalDateTime.now().plusDays(1);
        when(gapService.getById(10L)).thenReturn(gap(10L, 1L, start, start.plusHours(2)));
        when(gapService.getById(20L)).thenReturn(gap(20L, 2L, start, start.plusHours(2)));
        when(matchRepository.save(any(MatchModel.class))).thenAnswer(call -> call.getArgument(0));

        LocalDateTime before = LocalDateTime.now();
        MatchModel saved = service.sendMatchRequest(10L, 20L, 0.0);
        LocalDateTime after = LocalDateTime.now();

        assertNotNull(saved.getCreatedAt());
        assertFalse(saved.getCreatedAt().isBefore(before));
        assertFalse(saved.getCreatedAt().isAfter(after));
        // The meeting is tomorrow, but the request was made now: that is the date BQ 10 uses
        assertTrue(saved.getCreatedAt().isBefore(saved.getStartTime()));
    }
}
