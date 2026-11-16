package com.backend.gapfinder.controller.controllers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.backend.gapfinder.model.exceptions.ForbiddenException;
import com.backend.gapfinder.model.models.UserModel;
import com.backend.gapfinder.model.services.MatchRatingService;
import com.backend.gapfinder.view.dto.MatchHistoryItemDTO;
import com.backend.gapfinder.view.dto.requests.MatchRatingRequest;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

class MatchRatingControllerTest {

    private MatchRatingService service;
    private MatchRatingController controller;
    private UserModel me;

    @BeforeEach
    void setUp() {
        service = mock(MatchRatingService.class);
        controller = new MatchRatingController(service);
        me = new UserModel();
        me.setId(1L);
    }

    @Test
    void aUserReadsTheirOwnHistory() {
        MatchHistoryItemDTO item = new MatchHistoryItemDTO();
        item.setMatchId(100L);
        when(service.getHistory(1L)).thenReturn(List.of(item));

        assertEquals(List.of(item), controller.getHistory(1L, me));
    }

    @Test
    void aUserCannotReadTheHistoryOfAnotherUser() {
        assertThrows(ForbiddenException.class, () -> controller.getHistory(2L, me));
        verify(service, never()).getHistory(anyLong());
    }

    @Test
    void theRaterIsAlwaysTheAuthenticatedUser() {
        MatchRatingRequest request = new MatchRatingRequest();
        request.setStars(5);
        request.setComment("Genial");

        controller.rate(100L, request, me);

        verify(service).rate(100L, 1L, 5, "Genial");
    }

    @Test
    void ratingWithoutAnAuthenticatedUserIsForbidden() {
        assertThrows(ForbiddenException.class, () -> controller.rate(100L, new MatchRatingRequest(), null));
        verify(service, never()).rate(any(), any(), any(), any());
    }
}
