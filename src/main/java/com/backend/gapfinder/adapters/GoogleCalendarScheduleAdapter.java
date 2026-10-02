package com.backend.gapfinder.adapters;

import com.backend.gapfinder.exceptions.ScheduleSourceUnavailableException;
import com.backend.gapfinder.mapper.GoogleEventMapper;
import com.backend.gapfinder.models.ClassBlockModel;
import com.backend.gapfinder.services.GoogleCalendarService;
import com.google.api.services.calendar.model.Event;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

// Adapter pattern - Adapter: implements the client interface (ScheduleSource) and wraps the service
// (GoogleCalendarService), translating Google Calendar events into GapFinder class blocks.
// The wrapped service is not modified and is not aware of this adapter
@Slf4j
@Component
public class GoogleCalendarScheduleAdapter implements ScheduleSource {

    // Service with the incompatible interface: returns Google's own Event objects
    private final GoogleCalendarService googleCalendarService;

    public GoogleCalendarScheduleAdapter(GoogleCalendarService googleCalendarService) {
        this.googleCalendarService = googleCalendarService;
    }

    // Get the user's events from Google Calendar and convert each one into a class block
    @Override
    public AdaptedSchedule fetchClassBlocks(Long userId) {
        List<Event> events = getGoogleEvents(userId);

        List<ClassBlockModel> classBlocks = new ArrayList<>();
        int skippedEvents = 0;

        for (Event event : events) {
            ClassBlockModel block;
            try {
                block = GoogleEventMapper.toClassBlock(event);
            } catch (IllegalArgumentException e) {
                // Events the schedule does not support (e.g. on Sunday) are skipped instead of stopping the import
                log.warn("Se omitió un evento de Google Calendar: {} ({})", event.getSummary(), e.getMessage());
                skippedEvents++;
                continue;
            }

            // All-day events are not classes: they are ignored without counting them
            if (block == null) {
                continue;
            }

            classBlocks.add(block);
        }

        return new AdaptedSchedule(classBlocks, skippedEvents);
    }

    // Call the wrapped service, translating its checked errors into a domain exception
    private List<Event> getGoogleEvents(Long userId) {
        try {
            return googleCalendarService.getEvents(userId);
        } catch (RuntimeException e) {
            // Domain errors (e.g. NotFoundException when Google is not connected) keep their type and response
            throw e;
        } catch (Exception e) {
            // Google errors (token, network, API) keep the original error as the cause
            throw new ScheduleSourceUnavailableException("No se pudo obtener el horario de Google Calendar", e);
        }
    }
}
