package com.backend.gapfinder.model.services;

import com.backend.gapfinder.model.enums.DayOfWeekEnum;
import com.backend.gapfinder.model.models.ClassBlockModel;
import com.backend.gapfinder.model.models.GapModel;
import com.backend.gapfinder.model.models.UserModel;
import com.backend.gapfinder.model.repositories.ClassBlockRepository;
import com.backend.gapfinder.model.repositories.GapRepository;
import com.backend.gapfinder.model.repositories.MatchRepository;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

// Calculates and stores the weekly gaps of a user from their class schedule
@Slf4j
@Service
public class WeeklyGapService {

    // Free time between two classes of the same day
    record TimeSlot(LocalTime start, LocalTime end) {}

    private final ClassBlockRepository classBlockRepository;
    private final GapRepository gapRepository;
    private final MatchRepository matchRepository;
    private final UserService userService;

    // Classes only happen inside this range, anything outside is not a gap
    private final LocalTime dayStart;
    private final LocalTime dayEnd;

    // Free time shorter than this between two classes is not a gap
    private final Duration minGap;

    public WeeklyGapService(ClassBlockRepository classBlockRepository,
                            GapRepository gapRepository,
                            MatchRepository matchRepository,
                            UserService userService,
                            @Value("${gaps.day-start:06:30}") LocalTime dayStart,
                            @Value("${gaps.day-end:21:30}") LocalTime dayEnd,
                            @Value("${gaps.min-minutes:30}") long minGapMinutes) {
        this.classBlockRepository = classBlockRepository;
        this.gapRepository = gapRepository;
        this.matchRepository = matchRepository;
        this.userService = userService;
        this.dayStart = dayStart;
        this.dayEnd = dayEnd;
        this.minGap = Duration.ofMinutes(minGapMinutes);
    }

    // Recalculate the gaps of a user for the week (Monday to Saturday) that starts on weekStart
    @Transactional
    public List<GapModel> generateWeekGaps(Long userId, LocalDate weekStart) {
        log.info("Inicia proceso de generación de gaps del usuario {} para la semana del {}", userId, weekStart);

        if (weekStart.getDayOfWeek() != DayOfWeek.MONDAY) {
            throw new IllegalArgumentException("La semana debe empezar un lunes (weekStart)");
        }

        UserModel user = userService.getById(userId);
        List<ClassBlockModel> classBlocks = classBlockRepository.findByUserId(userId);

        // Gaps already used in a match are never touched. The other ones of the week are updated in place:
        // a gap that is still free keeps its id (the app and other users keep pointing to it), only the
        // gaps that no longer exist are deleted and only the new ones are inserted
        List<GapModel> existing = gapRepository.findByUserAndStartBetween(
                userId, weekStart.atStartOfDay(), weekStart.plusWeeks(1).atStartOfDay());
        List<GapModel> keptGaps = new ArrayList<>();
        List<GapModel> unmatched = new ArrayList<>();
        for (GapModel gap : existing) {
            if (matchRepository.existsByProposerGapIdOrAcceptorGapId(gap.getId(), gap.getId())) {
                keptGaps.add(gap);
            } else {
                unmatched.add(gap);
            }
        }

        List<GapModel> current = new ArrayList<>();
        List<GapModel> newGaps = new ArrayList<>();
        for (DayOfWeekEnum day : DayOfWeekEnum.values()) {
            LocalDate date = weekStart.plusDays(day.ordinal());

            List<ClassBlockModel> dayBlocks = classBlocks.stream()
                    .filter(block -> block.getDayOfWeek() == day)
                    .toList();

            for (TimeSlot slot : calculateDayGaps(dayBlocks, dayStart, dayEnd, minGap)) {
                LocalDateTime start = date.atTime(slot.start());
                LocalDateTime end = date.atTime(slot.end());

                boolean overlapsKept = keptGaps.stream()
                        .anyMatch(g -> g.getStartTime().isBefore(end) && g.getEndTime().isAfter(start));
                if (overlapsKept) {
                    continue;
                }

                // The same free time as before: the gap stays as it is
                GapModel same = unmatched.stream()
                        .filter(g -> g.getStartTime().equals(start) && g.getEndTime().equals(end))
                        .findFirst()
                        .orElse(null);
                if (same != null) {
                    unmatched.remove(same);
                    current.add(same);
                    continue;
                }

                GapModel gap = new GapModel();
                gap.setUser(user);
                gap.setStartTime(start);
                gap.setEndTime(end);
                newGaps.add(gap);
            }
        }

        // What is left in unmatched is free time that does not exist anymore
        gapRepository.deleteAll(unmatched);
        List<GapModel> saved = gapRepository.saveAll(newGaps);
        current.addAll(saved);

        log.info("Termina proceso de generación de gaps del usuario {}: {} nuevos, {} sin cambios, {} eliminados, {} conservados por tener match",
                userId, saved.size(), current.size() - saved.size(), unmatched.size(), keptGaps.size());
        return current;
    }

    // Ids of the users that have a class schedule, used by the weekly job
    @Transactional(readOnly = true)
    public List<Long> getUserIdsWithSchedule() {
        return classBlockRepository.findDistinctUserIds();
    }

    // Monday of the current week, or of the next week when today is Sunday
    public static LocalDate currentOrNextWeekStart(LocalDate today) {
        if (today.getDayOfWeek() == DayOfWeek.SUNDAY) {
            return today.plusDays(1);
        }
        return today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
    }

    // Free time between consecutive classes of one day, ignoring time before the first and after the last class
    static List<TimeSlot> calculateDayGaps(List<ClassBlockModel> dayBlocks,
                                           LocalTime dayStart, LocalTime dayEnd, Duration minGap) {
        List<TimeSlot> classes = dayBlocks.stream()
                .filter(block -> block.getStartTime() != null && block.getEndTime() != null)
                .map(block -> new TimeSlot(
                        max(block.getStartTime(), dayStart),
                        min(block.getEndTime(), dayEnd)))
                .filter(slot -> slot.start().isBefore(slot.end()))
                .sorted(Comparator.comparing(TimeSlot::start))
                .toList();

        List<TimeSlot> gaps = new ArrayList<>();
        if (classes.isEmpty()) {
            return gaps;
        }

        // Overlapping classes are treated as one continuous busy block
        LocalTime busyUntil = classes.get(0).end();
        for (TimeSlot next : classes.subList(1, classes.size())) {
            if (!Duration.between(busyUntil, next.start()).minus(minGap).isNegative()) {
                gaps.add(new TimeSlot(busyUntil, next.start()));
            }
            busyUntil = max(busyUntil, next.end());
        }

        return gaps;
    }

    private static LocalTime max(LocalTime a, LocalTime b) {
        return a.isAfter(b) ? a : b;
    }

    private static LocalTime min(LocalTime a, LocalTime b) {
        return a.isBefore(b) ? a : b;
    }

    // Get the gaps of a user for the week that starts on weekStart
    @Transactional(readOnly = true)
    public List<GapModel> getWeekGaps(Long userId, LocalDate weekStart) {
        userService.getById(userId);
        return gapRepository.findByUserAndStartBetween(
                userId, weekStart.atStartOfDay(), weekStart.plusWeeks(1).atStartOfDay());
    }
}
