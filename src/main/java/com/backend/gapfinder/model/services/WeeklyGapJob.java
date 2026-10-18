package com.backend.gapfinder.services;

import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;

// Weekly job: every Sunday it calculates the gaps of the coming week for every user with a schedule
@Slf4j
@Component
public class WeeklyGapJob {

    private final WeeklyGapService weeklyGapService;

    public WeeklyGapJob(WeeklyGapService weeklyGapService) {
        this.weeklyGapService = weeklyGapService;
    }

    // Runs every Sunday at 20:00 (Bogotá time) by default
    @Scheduled(cron = "${gaps.weekly-cron:0 0 20 * * SUN}", zone = "America/Bogota")
    public void generateNextWeekGaps() {
        LocalDate weekStart = WeeklyGapService.currentOrNextWeekStart(LocalDate.now());
        log.info("Inicia proceso semanal de generación de gaps para la semana del {}", weekStart);

        List<Long> userIds = weeklyGapService.getUserIdsWithSchedule();
        int failed = 0;

        // Each user runs in its own transaction, so one failure does not stop the rest
        for (Long userId : userIds) {
            try {
                weeklyGapService.generateWeekGaps(userId, weekStart);
            } catch (Exception ex) {
                failed++;
                log.error("No se pudieron generar los gaps del usuario {}: {}", userId, ex.getMessage());
            }
        }

        log.info("Termina proceso semanal de generación de gaps: {} usuarios, {} con error", userIds.size(), failed);
    }
}
