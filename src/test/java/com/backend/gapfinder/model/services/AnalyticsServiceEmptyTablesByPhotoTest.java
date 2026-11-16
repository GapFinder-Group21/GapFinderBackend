package com.backend.gapfinder.model.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.backend.gapfinder.model.exceptions.NotFoundException;
import com.backend.gapfinder.model.repositories.ActivityRepository;
import com.backend.gapfinder.model.repositories.AppSessionRepository;
import com.backend.gapfinder.model.repositories.GapRepository;
import com.backend.gapfinder.model.repositories.InterestRepository;
import com.backend.gapfinder.model.repositories.MatchRepository;
import com.backend.gapfinder.model.repositories.OpenTableRepository;
import com.backend.gapfinder.model.repositories.UserLocationLogRepository;
import com.backend.gapfinder.model.repositories.projections.EmptyTablesByPhotoProjection;
import com.backend.gapfinder.view.dto.EmptyTablesByPhotoDTO;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

// BQ 14: share of finished open tables with and without photo that nobody joined
class AnalyticsServiceEmptyTablesByPhotoTest {

    private OpenTableRepository openTableRepository;
    private AnalyticsService service;

    @BeforeEach
    void setUp() {
        openTableRepository = mock(OpenTableRepository.class);
        service = new AnalyticsService(
                mock(OpenTableAbandonmentService.class), mock(GapRepository.class),
                mock(UserLocationLogRepository.class), mock(MatchRepository.class), openTableRepository,
                mock(AppSessionRepository.class), mock(UserService.class), mock(ActivityRepository.class),
                mock(InterestRepository.class));
    }

    private static EmptyTablesByPhotoProjection row(String group, long total, long empty) {
        EmptyTablesByPhotoProjection row = mock(EmptyTablesByPhotoProjection.class);
        when(row.getPhotoGroup()).thenReturn(group);
        when(row.getTotal()).thenReturn(total);
        when(row.getEmpty()).thenReturn(empty);
        return row;
    }

    // The rows are built before the stubbing, because a mock cannot be stubbed inside another stubbing
    private void queryReturns(EmptyTablesByPhotoProjection... rows) {
        List<EmptyTablesByPhotoProjection> list = List.of(rows);
        when(openTableRepository.countEmptyTablesByPhoto()).thenReturn(list);
    }

    @Test
    void returnsBothGroupsInOrderEvenWhenOneIsMissing() {
        queryReturns(row("WITHOUT_PHOTO", 8, 2));

        List<EmptyTablesByPhotoDTO> result = service.getEmptyTablesByPhoto();

        assertEquals(List.of("WITH_PHOTO", "WITHOUT_PHOTO"), result.stream().map(EmptyTablesByPhotoDTO::group).toList());
        assertEquals(0, result.get(0).total());
        assertEquals(0, result.get(0).empty());
    }

    @Test
    void keepsTheOrderWhenTheQueryReturnsThemReversed() {
        queryReturns(row("WITHOUT_PHOTO", 10, 6), row("WITH_PHOTO", 5, 1));

        List<EmptyTablesByPhotoDTO> result = service.getEmptyTablesByPhoto();

        assertEquals(List.of("WITH_PHOTO", "WITHOUT_PHOTO"), result.stream().map(EmptyTablesByPhotoDTO::group).toList());
    }

    @Test
    void computesTheEmptyPercentageOfEachGroup() {
        queryReturns(row("WITH_PHOTO", 5, 1), row("WITHOUT_PHOTO", 8, 2));

        List<EmptyTablesByPhotoDTO> result = service.getEmptyTablesByPhoto();

        assertEquals(20.0, result.get(0).emptyPercentage());
        assertEquals(25.0, result.get(1).emptyPercentage());
    }

    @Test
    void aGroupWithoutFinishedTablesHasZeroPercentWithoutDividingByZero() {
        queryReturns(row("WITH_PHOTO", 4, 1));

        List<EmptyTablesByPhotoDTO> result = service.getEmptyTablesByPhoto();

        assertEquals(0.0, result.get(1).emptyPercentage());
        assertFalse(result.get(1).highestEmpty());
    }

    @Test
    void marksOnlyTheGroupWithTheHighestEmptyPercentage() {
        queryReturns(row("WITH_PHOTO", 10, 1), row("WITHOUT_PHOTO", 10, 6));

        List<EmptyTablesByPhotoDTO> result = service.getEmptyTablesByPhoto();

        assertFalse(result.get(0).highestEmpty());
        assertTrue(result.get(1).highestEmpty());
        assertEquals(1, result.stream().filter(EmptyTablesByPhotoDTO::highestEmpty).count());
    }

    @Test
    void aTieMarksNoGroup() {
        queryReturns(row("WITH_PHOTO", 4, 2), row("WITHOUT_PHOTO", 10, 5));

        List<EmptyTablesByPhotoDTO> result = service.getEmptyTablesByPhoto();

        assertEquals(50.0, result.get(0).emptyPercentage());
        assertEquals(50.0, result.get(1).emptyPercentage());
        assertTrue(result.stream().noneMatch(EmptyTablesByPhotoDTO::highestEmpty));
    }

    @Test
    void noGroupIsMarkedWhenNoTableEndedEmpty() {
        queryReturns(row("WITH_PHOTO", 3, 0), row("WITHOUT_PHOTO", 7, 0));

        List<EmptyTablesByPhotoDTO> result = service.getEmptyTablesByPhoto();

        assertTrue(result.stream().noneMatch(EmptyTablesByPhotoDTO::highestEmpty));
    }

    @Test
    void withoutFinishedOpenTablesItIsNotFound() {
        queryReturns();

        assertThrows(NotFoundException.class, () -> service.getEmptyTablesByPhoto());
    }
}
