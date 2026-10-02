package com.backend.gapfinder.services;

import com.backend.gapfinder.adapters.AdaptedSchedule;
import com.backend.gapfinder.adapters.ScheduleSource;
import com.backend.gapfinder.dto.ClassBlockBasicDTO;
import com.backend.gapfinder.dto.responses.GoogleImportResult;
import com.backend.gapfinder.exceptions.DuplicateClassBlockException;
import com.backend.gapfinder.models.ClassBlockModel;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

// Imports the user's weekly schedule from Google Calendar and recalculates their gaps
// Adapter pattern - Client: talks to Google Calendar only through the ScheduleSource interface
// (implemented by GoogleCalendarScheduleAdapter), so it works with GapFinder class blocks and never with Google events
@Slf4j
@Service
public class GoogleScheduleImportService {

    private final ScheduleSource scheduleSource;
    private final ClassBlockService classBlockService;
    private final WeeklyGapService weeklyGapService;

    public GoogleScheduleImportService(ScheduleSource scheduleSource,
                                       ClassBlockService classBlockService,
                                       WeeklyGapService weeklyGapService) {
        this.scheduleSource = scheduleSource;
        this.classBlockService = classBlockService;
        this.weeklyGapService = weeklyGapService;
    }

    public GoogleImportResult importSchedule(Long userId) throws Exception {
        log.info("Inicia importación del horario de Google Calendar para el usuario {}", userId);

        // The adapter returns the events already converted to class blocks
        AdaptedSchedule schedule = scheduleSource.fetchClassBlocks(userId);
        List<ClassBlockBasicDTO> created = new ArrayList<>();
        int omitidos = schedule.skippedEvents();

        for (ClassBlockModel block : schedule.classBlocks()) {
            try {
                ClassBlockModel saved = classBlockService.create(userId, block);
                created.add(ClassBlockBasicDTO.fromModel(saved));
            } catch (DuplicateClassBlockException e) {
                omitidos++;
            } catch (IllegalArgumentException e) {
                log.warn("Se omitió un evento inválido: {} ({})", block.getSubject(), e.getMessage());
                omitidos++;
            }
        }

        // Google Calendar imports the current Monday-Saturday window (see GoogleCalendarService.getEvents),
        // so the gaps are recalculated for that same week
        LocalDate weekStart = LocalDate.now().with(DayOfWeek.MONDAY);
        weeklyGapService.generateWeekGaps(userId, weekStart);

        log.info("Importación terminada para el usuario {}: {} creados, {} omitidos",
                userId, created.size(), omitidos);
        return new GoogleImportResult(created, omitidos);
    }
}