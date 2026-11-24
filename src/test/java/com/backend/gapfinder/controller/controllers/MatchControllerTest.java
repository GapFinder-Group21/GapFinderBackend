package com.backend.gapfinder.controller.controllers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.backend.gapfinder.model.exceptions.ForbiddenException;
import com.backend.gapfinder.model.models.GapModel;
import com.backend.gapfinder.model.models.MatchModel;
import com.backend.gapfinder.model.models.UserModel;
import com.backend.gapfinder.model.services.GapService;
import com.backend.gapfinder.model.services.MatchService;
import com.backend.gapfinder.view.dto.MatchCompleteDTO;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.modelmapper.ModelMapper;

import java.util.List;

class MatchControllerTest {

    private MatchService matchService;
    private GapService gapService;
    private ModelMapper modelMapper;
    private MatchController controller;

    private UserModel proposer;
    private UserModel acceptor;
    private UserModel stranger;
    private GapModel proposerGap;
    private MatchModel match;

    @BeforeEach
    void setUp() {
        matchService = mock(MatchService.class);
        gapService = mock(GapService.class);
        modelMapper = mock(ModelMapper.class);
        controller = new MatchController(matchService, gapService, modelMapper);

        proposer = user(1L);
        acceptor = user(2L);
        stranger = user(3L);

        proposerGap = gap(10L, proposer);
        match = new MatchModel();
        match.setId(100L);
        match.setProposerGap(proposerGap);
        match.setAcceptorGap(gap(20L, acceptor));

        when(matchService.getById(100L)).thenReturn(match);
        when(gapService.getById(10L)).thenReturn(proposerGap);
        when(modelMapper.map(any(MatchModel.class), org.mockito.ArgumentMatchers.eq(MatchCompleteDTO.class)))
                .thenReturn(new MatchCompleteDTO());
    }

    private static UserModel user(Long id) {
        UserModel user = new UserModel();
        user.setId(id);
        return user;
    }

    private static GapModel gap(Long id, UserModel owner) {
        GapModel gap = new GapModel();
        gap.setId(id);
        gap.setUser(owner);
        return gap;
    }

    // ---------- GET /matches/{id} ----------

    @Test
    void bothUsersOfTheMatchCanReadIt() {
        controller.getMatch(100L, proposer);
        controller.getMatch(100L, acceptor);

        verify(modelMapper, org.mockito.Mockito.times(2))
                .map(match, MatchCompleteDTO.class);
    }

    @Test
    void anotherUserCannotReadTheMatch() {
        assertThrows(ForbiddenException.class, () -> controller.getMatch(100L, stranger));
        verify(modelMapper, never()).map(any(MatchModel.class), org.mockito.ArgumentMatchers.eq(MatchCompleteDTO.class));
    }

    // ---------- GET /matches/user/{userId} and GET /matches ----------

    @Test
    void userReadsTheirOwnMatches() {
        when(matchService.getByUser(1L)).thenReturn(List.of(match));
        when(modelMapper.map(any(List.class), any(java.lang.reflect.Type.class))).thenReturn(List.of(new MatchCompleteDTO()));

        assertEquals(1, controller.getByUser(1L, proposer).size());
    }

    @Test
    void userCannotReadTheMatchesOfAnotherUser() {
        assertThrows(ForbiddenException.class, () -> controller.getByUser(1L, stranger));
        verify(matchService, never()).getByUser(any());
    }

    @Test
    void getAllOnlyAsksForTheMatchesOfTheCaller() {
        when(matchService.getByUser(2L)).thenReturn(List.of());
        when(modelMapper.map(any(List.class), any(java.lang.reflect.Type.class))).thenReturn(List.of());

        controller.getAll(acceptor);

        verify(matchService).getByUser(2L);
        verify(matchService, never()).getAll();
    }

    // ---------- GET /matches/gap/{gapId}/candidates ----------

    @Test
    void ownerOfTheGapFindsCandidates() {
        when(matchService.findCandidates(10L, false, false, false)).thenReturn(List.of());

        assertEquals(0, controller.findCandidates(10L, false, false, false, proposer).size());
    }

    @Test
    void anotherUserCannotFindCandidatesOfAGapThatIsNotTheirs() {
        assertThrows(ForbiddenException.class,
                () -> controller.findCandidates(10L, false, false, false, stranger));
        verify(matchService, never()).findCandidates(any(), org.mockito.ArgumentMatchers.anyBoolean(),
                org.mockito.ArgumentMatchers.anyBoolean(), org.mockito.ArgumentMatchers.anyBoolean());
    }

    // ---------- POST /matches/{id}/cancel ----------

    @Test
    void cancelUsesTheIdOfTheAuthenticatedUser() {
        when(matchService.cancelMatch(100L, 1L)).thenReturn(match);

        controller.cancelMatch(100L, proposer);

        verify(matchService).cancelMatch(100L, 1L);
    }
}
