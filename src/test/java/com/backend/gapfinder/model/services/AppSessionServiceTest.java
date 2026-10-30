package com.backend.gapfinder.model.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.backend.gapfinder.model.models.AppSessionModel;
import com.backend.gapfinder.model.models.UserModel;
import com.backend.gapfinder.model.repositories.AppSessionRepository;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.LocalDateTime;

// An app opening queued offline arrives later: the phone tells when it happened
class AppSessionServiceTest {

    private static final LocalDateTime NOW = LocalDateTime.of(2026, 10, 30, 14, 30);

    @Test
    void anOpeningFromTheLastDaysKeepsTheTimeOfThePhone() {
        LocalDateTime tenAm = NOW.minusHours(4).minusMinutes(28);

        assertEquals(tenAm, AppSessionService.resolveOpenedAt(tenAm, NOW));
    }

    @Test
    void withoutATimeTheServerUsesNow() {
        assertEquals(NOW, AppSessionService.resolveOpenedAt(null, NOW));
    }

    @Test
    void aTimeInTheFutureIsReplacedByNow() {
        assertEquals(NOW, AppSessionService.resolveOpenedAt(NOW.plusHours(2), NOW));
    }

    @Test
    void aTimeOlderThanSevenDaysIsReplacedByNow() {
        assertEquals(NOW, AppSessionService.resolveOpenedAt(NOW.minusDays(8), NOW));
    }

    @Test
    void exactlyAtTheLimitOfSevenDaysIsKept() {
        LocalDateTime limit = NOW.minusDays(7).plusMinutes(1);

        assertEquals(limit, AppSessionService.resolveOpenedAt(limit, NOW));
    }

    @Test
    void registerSavesTheTimeSentByThePhone() {
        AppSessionRepository repository = mock(AppSessionRepository.class);
        UserService userService = mock(UserService.class);
        UserModel user = new UserModel();
        user.setId(5L);
        when(userService.getById(5L)).thenReturn(user);
        when(repository.save(any(AppSessionModel.class))).thenAnswer(inv -> inv.getArgument(0));
        AppSessionService service = new AppSessionService(repository, userService);

        LocalDateTime opened = LocalDateTime.now().minusHours(3);
        service.register(5L, opened);

        ArgumentCaptor<AppSessionModel> saved = ArgumentCaptor.forClass(AppSessionModel.class);
        verify(repository).save(saved.capture());
        assertEquals(opened, saved.getValue().getOpenedAt());
    }

    @Test
    void registerWithoutATimeKeepsWorkingAsBefore() {
        AppSessionRepository repository = mock(AppSessionRepository.class);
        UserService userService = mock(UserService.class);
        when(userService.getById(5L)).thenReturn(new UserModel());
        when(repository.save(any(AppSessionModel.class))).thenAnswer(inv -> inv.getArgument(0));
        AppSessionService service = new AppSessionService(repository, userService);

        LocalDateTime before = LocalDateTime.now();
        AppSessionModel saved = service.register(5L);

        assertTrue(!saved.getOpenedAt().isBefore(before));
    }
}
