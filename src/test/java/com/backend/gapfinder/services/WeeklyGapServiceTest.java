package com.backend.gapfinder.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.backend.gapfinder.enums.DayOfWeekEnum;
import com.backend.gapfinder.models.ClassBlockModel;
import com.backend.gapfinder.services.WeeklyGapService.TimeSlot;

import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

class WeeklyGapServiceTest {

    private static final LocalTime DAY_START = LocalTime.of(6, 30);
    private static final LocalTime DAY_END = LocalTime.of(21, 30);
    private static final Duration MIN_GAP = Duration.ofMinutes(30);

    private static ClassBlockModel block(String start, String end) {
        ClassBlockModel block = new ClassBlockModel();
        block.setSubject("Clase");
        block.setDayOfWeek(DayOfWeekEnum.MON);
        block.setStartTime(LocalTime.parse(start));
        block.setEndTime(LocalTime.parse(end));
        return block;
    }

    private static List<TimeSlot> gaps(ClassBlockModel... blocks) {
        return WeeklyGapService.calculateDayGaps(List.of(blocks), DAY_START, DAY_END, MIN_GAP);
    }

    private static TimeSlot slot(String start, String end) {
        return new TimeSlot(LocalTime.parse(start), LocalTime.parse(end));
    }

    @Test
    void dayWithoutClassesHasNoGaps() {
        assertTrue(gaps().isEmpty());
    }

    @Test
    void singleClassHasNoGaps() {
        assertTrue(gaps(block("08:00", "10:00")).isEmpty());
    }

    @Test
    void timeBeforeFirstAndAfterLastClassIsNotAGap() {
        // Classes end at 14:00: 14:00-21:30 is not a gap, and neither is 06:30-08:00
        List<TimeSlot> result = gaps(block("08:00", "10:00"), block("12:00", "14:00"));

        assertEquals(List.of(slot("10:00", "12:00")), result);
    }

    @Test
    void gapsShorterThanMinimumAreIgnored() {
        List<TimeSlot> result = gaps(
                block("07:00", "08:00"),
                block("08:15", "09:00"),   // 15 min -> ignored
                block("09:30", "10:30"),   // exactly 30 min -> gap
                block("13:00", "14:00"));  // 2.5 h -> gap

        assertEquals(List.of(slot("09:00", "09:30"), slot("10:30", "13:00")), result);
    }

    @Test
    void unsortedAndOverlappingClassesAreMerged() {
        List<TimeSlot> result = gaps(
                block("14:00", "16:00"),
                block("08:00", "11:00"),
                block("09:00", "10:00"),   // inside the 08:00-11:00 class
                block("10:30", "12:00"));  // overlaps the 08:00-11:00 class

        assertEquals(List.of(slot("12:00", "14:00")), result);
    }

    @Test
    void classesAreClampedToTheSchoolDay() {
        List<TimeSlot> result = gaps(block("05:00", "07:00"), block("21:00", "23:00"));

        assertEquals(List.of(slot("07:00", "21:00")), result);
    }

    @Test
    void weekStartIsCurrentMondayOrNextMondayOnSunday() {
        // 2026-09-28 is a Monday
        assertEquals(LocalDate.of(2026, 9, 28), WeeklyGapService.currentOrNextWeekStart(LocalDate.of(2026, 9, 28)));
        assertEquals(LocalDate.of(2026, 9, 28), WeeklyGapService.currentOrNextWeekStart(LocalDate.of(2026, 10, 3)));
        assertEquals(LocalDate.of(2026, 10, 5), WeeklyGapService.currentOrNextWeekStart(LocalDate.of(2026, 10, 4)));
    }
}
