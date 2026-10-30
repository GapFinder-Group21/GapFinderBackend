package com.backend.gapfinder.model.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.backend.gapfinder.model.enums.DayOfWeekEnum;
import com.backend.gapfinder.model.models.ClassBlockModel;
import com.backend.gapfinder.model.models.GapModel;
import com.backend.gapfinder.model.models.UserModel;
import com.backend.gapfinder.model.repositories.ClassBlockRepository;
import com.backend.gapfinder.model.repositories.GapRepository;
import com.backend.gapfinder.model.repositories.MatchRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

// The gap ids must not change when the app asks to generate the week again (it does it on every refresh)
class WeeklyGapServiceRegenerateTest {

    private static final LocalDate MONDAY = LocalDate.of(2026, 10, 26);

    private ClassBlockRepository classBlocks;
    private GapRepository gaps;
    private MatchRepository matches;
    private UserService userService;
    private WeeklyGapService service;
    private UserModel user;

    // What the "database" holds, so each generate call sees the result of the previous one
    private List<GapModel> stored;
    private long nextId;

    @BeforeEach
    void setUp() {
        classBlocks = mock(ClassBlockRepository.class);
        gaps = mock(GapRepository.class);
        matches = mock(MatchRepository.class);
        userService = mock(UserService.class);
        service = new WeeklyGapService(classBlocks, gaps, matches, userService,
                LocalTime.of(6, 30), LocalTime.of(21, 30), 30);

        user = new UserModel();
        user.setId(7L);
        when(userService.getById(7L)).thenReturn(user);

        // Monday: classes 08:00-10:00 and 12:00-14:00 -> one gap 10:00-12:00
        ClassBlockModel first = block("08:00", "10:00");
        ClassBlockModel second = block("12:00", "14:00");
        when(classBlocks.findByUserId(7L)).thenReturn(List.of(first, second));

        stored = new ArrayList<>();
        nextId = 100;
        when(gaps.findByUserAndStartBetween(anyLong(), any(), any())).thenAnswer(inv -> new ArrayList<>(stored));
        when(gaps.saveAll(anyList())).thenAnswer(inv -> {
            List<GapModel> toSave = inv.getArgument(0);
            for (GapModel g : toSave) {
                g.setId(nextId++);
                stored.add(g);
            }
            return toSave;
        });
        org.mockito.Mockito.doAnswer(inv -> {
            List<GapModel> toDelete = inv.getArgument(0);
            stored.removeAll(toDelete);
            return null;
        }).when(gaps).deleteAll(anyList());
    }

    private static ClassBlockModel block(String start, String end) {
        ClassBlockModel block = new ClassBlockModel();
        block.setSubject("Clase");
        block.setDayOfWeek(DayOfWeekEnum.MON);
        block.setStartTime(LocalTime.parse(start));
        block.setEndTime(LocalTime.parse(end));
        return block;
    }

    @Test
    void generatingFiveTimesKeepsTheSameGapIds() {
        List<GapModel> firstCall = service.generateWeekGaps(7L, MONDAY);
        assertEquals(1, firstCall.size());
        Long id = firstCall.get(0).getId();

        for (int i = 0; i < 4; i++) {
            List<GapModel> again = service.generateWeekGaps(7L, MONDAY);
            assertEquals(1, again.size());
            assertEquals(id, again.get(0).getId());
        }

        // Only the first call inserted something, and nothing was ever deleted
        assertEquals(1, stored.size());
        verify(gaps, never()).deleteAll(org.mockito.ArgumentMatchers.<Iterable<GapModel>>argThat(
                list -> list.iterator().hasNext()));
    }

    @Test
    void aGapThatNoLongerExistsIsDeletedAndANewOneIsInserted() {
        service.generateWeekGaps(7L, MONDAY);
        Long oldId = stored.get(0).getId();

        // The second class moves to 13:00-15:00: the free time is now 10:00-13:00
        when(classBlocks.findByUserId(7L)).thenReturn(List.of(block("08:00", "10:00"), block("13:00", "15:00")));
        List<GapModel> after = service.generateWeekGaps(7L, MONDAY);

        assertEquals(1, after.size());
        assertEquals(LocalDateTime.of(2026, 10, 26, 13, 0), after.get(0).getEndTime());
        assertTrue(after.get(0).getId() != oldId);
        assertEquals(1, stored.size());
    }

    @Test
    void aGapWithAMatchIsNeverDeletedNorChanged() {
        service.generateWeekGaps(7L, MONDAY);
        GapModel matched = stored.get(0);
        when(matches.existsByProposerGapIdOrAcceptorGapId(matched.getId(), matched.getId())).thenReturn(true);

        // The classes disappear: the gap with a match stays
        when(classBlocks.findByUserId(7L)).thenReturn(List.of());
        service.generateWeekGaps(7L, MONDAY);

        ArgumentCaptor<Iterable<GapModel>> deleted = ArgumentCaptor.forClass(Iterable.class);
        verify(gaps, org.mockito.Mockito.atLeastOnce()).deleteAll(deleted.capture());
        assertTrue(deleted.getAllValues().stream().noneMatch(list -> list.iterator().hasNext()));
        assertTrue(stored.contains(matched));
    }
}
