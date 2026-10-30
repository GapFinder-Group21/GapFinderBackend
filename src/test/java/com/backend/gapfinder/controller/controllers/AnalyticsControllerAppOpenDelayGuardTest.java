package com.backend.gapfinder.controller.controllers;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.backend.gapfinder.model.exceptions.ForbiddenException;
import com.backend.gapfinder.model.models.UserModel;
import com.backend.gapfinder.model.services.AnalyticsService;
import com.backend.gapfinder.view.dto.AppOpenDelayStatsDTO;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

// The reminder time of a user (BQ 4) is personal, like the preferred activity (BQ 6)
class AnalyticsControllerAppOpenDelayGuardTest {

    private AnalyticsService analyticsService;
    private AnalyticsController controller;
    private UserModel me;
    private UserModel stranger;

    @BeforeEach
    void setUp() {
        analyticsService = mock(AnalyticsService.class);
        controller = new AnalyticsController(analyticsService);
        me = user(4L);
        stranger = user(9L);
    }

    private static UserModel user(Long id) {
        UserModel user = new UserModel();
        user.setId(id);
        return user;
    }

    @Test
    void aUserReadsTheirOwnReminderTime() {
        AppOpenDelayStatsDTO stats = mock(AppOpenDelayStatsDTO.class);
        when(analyticsService.getAppOpenDelay(4L, null)).thenReturn(stats);

        assertSame(stats, controller.getAppOpenDelayForUser(4L, null, me));
    }

    @Test
    void aUserCannotReadTheReminderTimeOfAnotherUser() {
        assertThrows(ForbiddenException.class, () -> controller.getAppOpenDelayForUser(4L, null, stranger));

        verify(analyticsService, never()).getAppOpenDelay(any(), any());
    }
}
