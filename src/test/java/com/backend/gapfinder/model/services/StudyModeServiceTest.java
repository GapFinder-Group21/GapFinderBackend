package com.backend.gapfinder.model.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.backend.gapfinder.model.exceptions.AlreadyDoneException;
import com.backend.gapfinder.model.exceptions.ForbiddenException;
import com.backend.gapfinder.model.models.StudyPeriodModel;
import com.backend.gapfinder.model.models.UserModel;
import com.backend.gapfinder.model.repositories.StudyPeriodRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

class StudyModeServiceTest {

    private StudyPeriodRepository periods;
    private UserService userService;
    private StudyModeService service;
    private LocalDateTime start;

    @BeforeEach
    void setUp() {
        periods = mock(StudyPeriodRepository.class);
        userService = mock(UserService.class);
        service = new StudyModeService(periods, userService);
        start = LocalDateTime.now().plusMinutes(5);

        when(userService.getById(1L)).thenReturn(user(1L));
        when(periods.saveAndFlush(any(StudyPeriodModel.class))).thenAnswer(inv -> {
            StudyPeriodModel saved = inv.getArgument(0);
            saved.setId(100L);
            return saved;
        });
    }

    private static UserModel user(Long id) {
        UserModel user = new UserModel();
        user.setId(id);
        return user;
    }

    private static StudyPeriodModel period(Long id, Long userId) {
        StudyPeriodModel period = new StudyPeriodModel();
        period.setId(id);
        period.setUser(user(userId));
        return period;
    }

    @Test
    void aUserCanSaveAStudyPeriod() {
        StudyPeriodModel saved = service.create(1L, start, start.plusHours(1), 7L, "uuid-1");

        assertEquals(100L, saved.getId());
        assertEquals(1L, saved.getUser().getId());
        assertEquals(start, saved.getStartTime());
        assertEquals(start.plusHours(1), saved.getEndTime());
        assertEquals(7L, saved.getGapId());
        assertEquals("uuid-1", saved.getClientId());
        assertNotNull(saved.getCreatedAt());
    }

    @Test
    void theGapIdIsOptional() {
        StudyPeriodModel saved = service.create(1L, start, start.plusMinutes(30), null, "uuid-1");

        assertEquals(null, saved.getGapId());
    }

    @Test
    void aPeriodOfExactly12HoursIsAllowed() {
        StudyPeriodModel saved = service.create(1L, start, start.plusHours(12), null, "uuid-1");

        assertEquals(100L, saved.getId());
    }

    @Test
    void aPeriodLongerThan12HoursIsABadRequest() {
        assertThrows(IllegalArgumentException.class,
                () -> service.create(1L, start, start.plusHours(12).plusMinutes(1), null, "uuid-1"));
        verify(periods, never()).saveAndFlush(any());
    }

    @Test
    void aPeriodThatEndsBeforeOrWhenItStartsIsABadRequest() {
        assertThrows(IllegalArgumentException.class,
                () -> service.create(1L, start, start, null, "uuid-1"));
        assertThrows(IllegalArgumentException.class,
                () -> service.create(1L, start, start.minusMinutes(10), null, "uuid-1"));
        verify(periods, never()).saveAndFlush(any());
    }

    @Test
    void aPeriodWithoutStartOrEndIsABadRequest() {
        assertThrows(IllegalArgumentException.class, () -> service.create(1L, null, start.plusHours(1), null, "uuid-1"));
        assertThrows(IllegalArgumentException.class, () -> service.create(1L, start, null, null, "uuid-1"));
    }

    @Test
    void aPeriodWithoutClientIdIsABadRequest() {
        assertThrows(IllegalArgumentException.class, () -> service.create(1L, start, start.plusHours(1), null, " "));
        assertThrows(IllegalArgumentException.class, () -> service.create(1L, start, start.plusHours(1), null, null));
        assertThrows(IllegalArgumentException.class,
                () -> service.create(1L, start, start.plusHours(1), null, "a".repeat(65)));
    }

    @Test
    void resendingTheSameClientIdIsAlreadyDone() {
        when(periods.existsByUserIdAndClientId(1L, "uuid-1")).thenReturn(true);

        assertThrows(AlreadyDoneException.class, () -> service.create(1L, start, start.plusHours(1), null, "uuid-1"));
        verify(periods, never()).saveAndFlush(any());
    }

    @Test
    void twoRetriesAtTheSameTimeAreAlreadyDone() {
        when(periods.saveAndFlush(any(StudyPeriodModel.class)))
                .thenThrow(new DataIntegrityViolationException("unique user_id, client_id"));

        assertThrows(AlreadyDoneException.class, () -> service.create(1L, start, start.plusHours(1), null, "uuid-1"));
    }

    @Test
    void theActivePeriodsComeFromTheRepositoryOfThatUser() {
        List<StudyPeriodModel> expected = List.of(period(5L, 1L));
        when(periods.findByUserIdAndEndTimeAfterOrderByStartTimeAsc(any(), any(LocalDateTime.class)))
                .thenReturn(expected);

        assertEquals(expected, service.getActivePeriods(1L));
    }

    @Test
    void removingAPeriodThatDoesNotExistIsNotAnError() {
        when(periods.findById(9L)).thenReturn(Optional.empty());

        service.delete(1L, 9L);

        verify(periods, never()).delete(any());
    }

    @Test
    void removingTheSamePeriodTwiceIsNotAnError() {
        StudyPeriodModel mine = period(5L, 1L);
        when(periods.findById(5L)).thenReturn(Optional.of(mine)).thenReturn(Optional.empty());

        service.delete(1L, 5L);
        service.delete(1L, 5L);

        verify(periods).delete(mine);
    }

    @Test
    void aUserCannotRemoveThePeriodOfAnotherUser() {
        when(periods.findById(5L)).thenReturn(Optional.of(period(5L, 2L)));

        assertThrows(ForbiddenException.class, () -> service.delete(1L, 5L));
        verify(periods, never()).delete(any());
    }
}
