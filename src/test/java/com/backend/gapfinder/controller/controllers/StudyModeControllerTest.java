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
import com.backend.gapfinder.model.models.StudyPeriodModel;
import com.backend.gapfinder.model.models.UserModel;
import com.backend.gapfinder.model.services.StudyModeService;
import com.backend.gapfinder.view.dto.StudyPeriodDTO;
import com.backend.gapfinder.view.dto.requests.StudyPeriodRequest;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;

class StudyModeControllerTest {

    private StudyModeService service;
    private StudyModeController controller;
    private UserModel me;
    private UserModel stranger;
    private LocalDateTime start;

    @BeforeEach
    void setUp() {
        service = mock(StudyModeService.class);
        controller = new StudyModeController(service);
        me = user(1L);
        stranger = user(3L);
        start = LocalDateTime.now().plusMinutes(5);
    }

    private static UserModel user(Long id) {
        UserModel user = new UserModel();
        user.setId(id);
        return user;
    }

    private StudyPeriodModel period(Long id) {
        StudyPeriodModel period = new StudyPeriodModel();
        period.setId(id);
        period.setUser(me);
        period.setStartTime(start);
        period.setEndTime(start.plusHours(1));
        period.setGapId(7L);
        period.setClientId("uuid-" + id);
        period.setCreatedAt(LocalDateTime.now());
        return period;
    }

    @Test
    void aUserReadsTheirStudyPeriods() {
        when(service.getActivePeriods(1L)).thenReturn(List.of(period(5L)));

        List<StudyPeriodDTO> result = controller.getActivePeriods(1L, me);

        assertEquals(1, result.size());
        assertEquals(5L, result.get(0).getId());
        assertEquals(1L, result.get(0).getUserId());
        assertEquals(7L, result.get(0).getGapId());
        assertEquals("uuid-5", result.get(0).getClientId());
    }

    @Test
    void aUserCannotReadTheStudyPeriodsOfAnotherUser() {
        assertThrows(ForbiddenException.class, () -> controller.getActivePeriods(1L, stranger));
        verify(service, never()).getActivePeriods(anyLong());
    }

    @Test
    void aUserSavesAStudyPeriodWithTheDataOfTheRequest() {
        StudyPeriodRequest request = new StudyPeriodRequest();
        request.setStartTime(start);
        request.setEndTime(start.plusHours(1));
        request.setGapId(7L);
        request.setClientId("uuid-9");
        when(service.create(1L, start, start.plusHours(1), 7L, "uuid-9")).thenReturn(period(9L));

        StudyPeriodDTO saved = controller.create(1L, request, me);

        assertEquals(9L, saved.getId());
        verify(service).create(1L, start, start.plusHours(1), 7L, "uuid-9");
    }

    @Test
    void aUserCannotSaveAStudyPeriodForAnotherUser() {
        assertThrows(ForbiddenException.class, () -> controller.create(1L, new StudyPeriodRequest(), stranger));
        verify(service, never()).create(any(), any(), any(), any(), any());
    }

    @Test
    void aUserRemovesTheirStudyPeriod() {
        controller.delete(1L, 5L, me);

        verify(service).delete(1L, 5L);
    }

    @Test
    void aUserCannotRemoveAStudyPeriodInTheNameOfAnotherUser() {
        assertThrows(ForbiddenException.class, () -> controller.delete(1L, 5L, stranger));
        assertThrows(ForbiddenException.class, () -> controller.delete(1L, 5L, null));
        verify(service, never()).delete(anyLong(), anyLong());
    }
}
