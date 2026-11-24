package com.backend.gapfinder.model.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.backend.gapfinder.model.enums.MatchStatusEnum;
import com.backend.gapfinder.model.enums.NotificationTypeEnum;
import com.backend.gapfinder.model.events.MatchEvent;
import com.backend.gapfinder.model.exceptions.AlreadyDoneException;
import com.backend.gapfinder.model.exceptions.ForbiddenException;
import com.backend.gapfinder.model.exceptions.NotFoundException;
import com.backend.gapfinder.model.models.GapModel;
import com.backend.gapfinder.model.models.MatchModel;
import com.backend.gapfinder.model.models.MatchRatingModel;
import com.backend.gapfinder.model.models.UserModel;
import com.backend.gapfinder.model.repositories.MatchRatingRepository;
import com.backend.gapfinder.model.repositories.MatchRepository;
import com.backend.gapfinder.view.dto.MatchHistoryItemDTO;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DataIntegrityViolationException;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

class MatchRatingServiceTest {

    private MatchRatingRepository ratings;
    private MatchRepository matches;
    private ApplicationEventPublisher events;
    private MatchRatingService service;

    private UserModel ana;
    private UserModel luis;
    private MatchModel match;
    // What the fake repository has stored
    private List<MatchRatingModel> stored;

    @BeforeEach
    void setUp() {
        ratings = mock(MatchRatingRepository.class);
        matches = mock(MatchRepository.class);
        events = mock(ApplicationEventPublisher.class);
        service = new MatchRatingService(ratings, matches, events);

        ana = user(1L, "Ana");
        luis = user(2L, "Luis");
        match = match(100L, ana, luis, MatchStatusEnum.COMPLETED, LocalDateTime.now().minusDays(1));
        stored = new ArrayList<>();

        when(matches.findById(100L)).thenReturn(Optional.of(match));
        when(ratings.findByMatchIdAndRaterId(any(), any())).thenReturn(Optional.empty());
        when(ratings.saveAndFlush(any(MatchRatingModel.class))).thenAnswer(inv -> {
            MatchRatingModel saved = inv.getArgument(0);
            saved.setId(500L + stored.size());
            stored.add(saved);
            return saved;
        });
        when(ratings.findByMatchIdIn(anyCollection())).thenAnswer(inv -> List.copyOf(stored));
    }

    private static UserModel user(Long id, String name) {
        UserModel user = new UserModel();
        user.setId(id);
        user.setName(name);
        return user;
    }

    private static MatchModel match(Long id, UserModel proposer, UserModel acceptor,
                                    MatchStatusEnum status, LocalDateTime start) {
        GapModel proposerGap = new GapModel();
        proposerGap.setId(id * 10);
        proposerGap.setUser(proposer);
        GapModel acceptorGap = new GapModel();
        acceptorGap.setId(id * 10 + 1);
        acceptorGap.setUser(acceptor);

        MatchModel m = new MatchModel();
        m.setId(id);
        m.setProposerGap(proposerGap);
        m.setAcceptorGap(acceptorGap);
        m.setStatus(status);
        m.setStartTime(start);
        m.setEndTime(start.plusHours(1));
        m.setScore(80.0);
        return m;
    }

    private static MatchRatingModel rating(MatchModel match, UserModel rater, UserModel rated, int stars) {
        MatchRatingModel r = new MatchRatingModel();
        r.setMatch(match);
        r.setRater(rater);
        r.setRated(rated);
        r.setStars(stars);
        r.setCreatedAt(LocalDateTime.now());
        return r;
    }

    // ---------- rate

    @Test
    void aParticipantRatesAFinishedMatch() {
        MatchHistoryItemDTO item = service.rate(100L, 2L, 4, "  Muy buena charla  ");

        MatchRatingModel saved = stored.get(0);
        assertEquals(2L, saved.getRater().getId());
        assertEquals(1L, saved.getRated().getId());
        assertEquals(4, saved.getStars());
        assertEquals("Muy buena charla", saved.getComment());

        assertEquals(4, item.getMyRating().getStars());
        assertEquals(1L, item.getOtherUser().getId());
        assertFalse(item.isCanRate());
    }

    @Test
    void ratingNotifiesTheOtherStudent() {
        service.rate(100L, 1L, 5, null);

        ArgumentCaptor<MatchEvent> event = ArgumentCaptor.forClass(MatchEvent.class);
        verify(events).publishEvent(event.capture());
        assertEquals(NotificationTypeEnum.MATCH_RATED, event.getValue().type());
        assertEquals(2L, event.getValue().recipientId());
        assertEquals("Ana", event.getValue().actorName());
        assertEquals(100L, event.getValue().matchId());
    }

    @Test
    void anEmptyCommentIsStoredAsNull() {
        service.rate(100L, 1L, 3, "   ");

        assertNull(stored.get(0).getComment());
    }

    @Test
    void aMatchThatHasNotFinishedCannotBeRated() {
        match.setStatus(MatchStatusEnum.ACCEPTED);

        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> service.rate(100L, 1L, 5, null));

        // Plain IllegalStateException answers 422
        assertEquals(IllegalStateException.class, ex.getClass());
        verify(ratings, never()).saveAndFlush(any());
    }

    @Test
    void ratingTwiceIsAlreadyDone() {
        when(ratings.findByMatchIdAndRaterId(100L, 1L))
                .thenReturn(Optional.of(rating(match, ana, luis, 5)));

        assertThrows(AlreadyDoneException.class, () -> service.rate(100L, 1L, 4, null));
        verify(ratings, never()).saveAndFlush(any());
        verify(events, never()).publishEvent(any());
    }

    @Test
    void twoRetriesAtTheSameTimeAreAlreadyDone() {
        when(ratings.saveAndFlush(any(MatchRatingModel.class)))
                .thenThrow(new DataIntegrityViolationException("unique match_id, rater_id"));

        assertThrows(AlreadyDoneException.class, () -> service.rate(100L, 1L, 4, null));
        verify(events, never()).publishEvent(any());
    }

    @Test
    void someoneOutsideTheMatchCannotRateIt() {
        assertThrows(ForbiddenException.class, () -> service.rate(100L, 3L, 5, null));
        verify(ratings, never()).saveAndFlush(any());
    }

    @Test
    void starsOutOfRangeAreABadRequest() {
        assertThrows(IllegalArgumentException.class, () -> service.rate(100L, 1L, 0, null));
        assertThrows(IllegalArgumentException.class, () -> service.rate(100L, 1L, 6, null));
        assertThrows(IllegalArgumentException.class, () -> service.rate(100L, 1L, null, null));
        verify(ratings, never()).saveAndFlush(any());
    }

    @Test
    void aCommentLongerThan300IsABadRequest() {
        assertThrows(IllegalArgumentException.class,
                () -> service.rate(100L, 1L, 5, "a".repeat(301)));
    }

    @Test
    void ratingAMatchThatDoesNotExistIsNotFound() {
        when(matches.findById(999L)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> service.rate(999L, 1L, 5, null));
    }

    // ---------- history

    @Test
    void theOtherRatingIsHiddenUntilIRate() {
        stored.add(rating(match, luis, ana, 2));
        when(matches.findByProposerGapUserIdOrAcceptorGapUserId(1L, 1L)).thenReturn(List.of(match));

        MatchHistoryItemDTO before = service.getHistory(1L).get(0);
        assertNull(before.getMyRating());
        assertNull(before.getOtherRating());
        assertTrue(before.isCanRate());

        MatchHistoryItemDTO after = service.rate(100L, 1L, 5, null);
        assertEquals(5, after.getMyRating().getStars());
        assertNotNull(after.getOtherRating());
        assertEquals(2, after.getOtherRating().getStars());
    }

    @Test
    void iRatedButTheOtherHasNotYet() {
        stored.add(rating(match, ana, luis, 4));
        when(matches.findByProposerGapUserIdOrAcceptorGapUserId(1L, 1L)).thenReturn(List.of(match));

        MatchHistoryItemDTO item = service.getHistory(1L).get(0);

        assertEquals(4, item.getMyRating().getStars());
        assertNull(item.getOtherRating());
        assertFalse(item.isCanRate());
    }

    @Test
    void theHistoryIsSortedFromNewestToOldestAndOnlyCompletedCanBeRated() {
        LocalDateTime now = LocalDateTime.now();
        MatchModel old = match(1L, ana, luis, MatchStatusEnum.COMPLETED, now.minusDays(5));
        MatchModel upcoming = match(2L, luis, ana, MatchStatusEnum.ACCEPTED, now.plusDays(1));
        MatchModel rejected = match(3L, ana, luis, MatchStatusEnum.REJECTED, now.minusDays(2));
        when(matches.findByProposerGapUserIdOrAcceptorGapUserId(1L, 1L))
                .thenReturn(List.of(old, upcoming, rejected));

        List<MatchHistoryItemDTO> history = service.getHistory(1L);

        assertEquals(List.of(2L, 3L, 1L), history.stream().map(MatchHistoryItemDTO::getMatchId).toList());
        assertFalse(history.get(0).isCanRate());
        assertFalse(history.get(1).isCanRate());
        assertTrue(history.get(2).isCanRate());
        // In match 2 Ana is the acceptor, the other one is still Luis
        assertEquals(2L, history.get(0).getOtherUser().getId());
    }

    @Test
    void theRatingsOfTheHistoryAreReadInOneQuery() {
        when(matches.findByProposerGapUserIdOrAcceptorGapUserId(1L, 1L)).thenReturn(List.of(
                match(1L, ana, luis, MatchStatusEnum.COMPLETED, LocalDateTime.now().minusDays(1)),
                match(2L, ana, luis, MatchStatusEnum.COMPLETED, LocalDateTime.now().minusDays(2))));

        service.getHistory(1L);

        verify(ratings).findByMatchIdIn(List.of(1L, 2L));
        verify(ratings, never()).findByMatchIdAndRaterId(any(), any());
    }

    @Test
    void aUserWithoutMatchesGetsAnEmptyHistory() {
        when(matches.findByProposerGapUserIdOrAcceptorGapUserId(1L, 1L)).thenReturn(List.of());

        assertTrue(service.getHistory(1L).isEmpty());
        verify(ratings, never()).findByMatchIdIn(anyCollection());
    }
}
