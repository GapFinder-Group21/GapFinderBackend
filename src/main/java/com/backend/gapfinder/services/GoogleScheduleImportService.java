package com.backend.gapfinder.services;

import com.backend.gapfinder.dto.ClassBlockBasicDTO;
import com.backend.gapfinder.dto.responses.GoogleImportResult;
import com.backend.gapfinder.exceptions.DuplicateClassBlockException;
import com.backend.gapfinder.mapper.GoogleEventMapper;
import com.backend.gapfinder.models.ClassBlockModel;
import com.google.api.services.calendar.model.Event;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

// Imports the user's weekly schedule from Google Calendar and recalculates their gaps
@Slf4j
@Service
public class GoogleScheduleImportService {

    private final GoogleCalendarService googleCalendarService;
    private final ClassBlockService classBlockService;
    private final WeeklyGapService weeklyGapService;

    public GoogleScheduleImportService(GoogleCalendarService googleCalendarService,
                                       ClassBlockService classBlockService,
                                       WeeklyGapService weeklyGapService) {
        this.googleCalendarService = googleCalendarService;
        this.classBlockService = classBlockService;
        this.weeklyGapService = weeklyGapService;
    }

    public GoogleImportResult importSchedule(Long userId) throws Exception {
        log.info("Inicia importación del horario de Google Calendar para el usuario {}", userId);

        List<Event> events = googleCalendarService.getEvents(userId);
        List<ClassBlockBasicDTO> created = new ArrayList<>();
        int omitidos = 0;

        for (Event event : events) {
            ClassBlockModel block = GoogleEventMapper.toClassBlock(event);
            if (block == null) continue;

            try {
                ClassBlockModel saved = classBlockService.create(userId, block);
                created.add(ClassBlockBasicDTO.fromModel(saved));
            } catch (DuplicateClassBlockException e) {
                omitidos++;
            } catch (IllegalArgumentException e) {
                log.warn("Se omitió un evento inválido: {} ({})", event.getSummary(), e.getMessage());
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