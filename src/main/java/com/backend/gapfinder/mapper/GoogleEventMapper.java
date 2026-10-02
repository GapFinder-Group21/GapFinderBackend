package com.backend.gapfinder.mapper;

import com.backend.gapfinder.enums.DayOfWeekEnum;
import com.backend.gapfinder.models.ClassBlockModel;
import com.google.api.services.calendar.model.Event;
import com.google.api.services.calendar.model.EventDateTime;

import org.springframework.web.util.HtmlUtils;

import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.time.ZoneId;

public class GoogleEventMapper {

    private static DayOfWeekEnum mapJavaDayOfWeek(DayOfWeek day) {
        return switch (day) {
            case MONDAY -> DayOfWeekEnum.MON;
            case TUESDAY -> DayOfWeekEnum.TUE;
            case WEDNESDAY -> DayOfWeekEnum.WED;
            case THURSDAY -> DayOfWeekEnum.THU;
            case FRIDAY -> DayOfWeekEnum.FRI;
            case SATURDAY -> DayOfWeekEnum.SAT;
            case SUNDAY -> throw new IllegalArgumentException("El horario no admite clases los domingos");
        };
    }

    // Convierte una ocurrencia real de evento en un solo bloque de clase.
    public static ClassBlockModel toClassBlock(Event event) {
        EventDateTime start = event.getStart();
        EventDateTime end = event.getEnd();

        if (start.getDateTime() == null || end.getDateTime() == null) {
            return null;
        }

        LocalDateTime startDateTime = toLocalDateTime(start);
        LocalDateTime endDateTime = toLocalDateTime(end);

        ClassBlockModel block = new ClassBlockModel();
        block.setSubject(cleanText(event.getSummary()));
        block.setLocation(extractClassroom(event.getLocation()));
        block.setDayOfWeek(mapJavaDayOfWeek(startDateTime.getDayOfWeek()));
        block.setStartTime(startDateTime.toLocalTime());
        block.setEndTime(endDateTime.toLocalTime());

        return block;
    }

    private static String extractClassroom(String location) {
        if (location == null || location.isBlank()) {
            return null;
        }

        String cleanLocation = cleanText(location);
        String marker = "Salón:";

        int index = cleanLocation.indexOf(marker);

        if (index != -1) {
            return cleanLocation
                    .substring(index + marker.length())
                    .trim();
        }

        return cleanLocation.trim();
    }

    private static String cleanText(String text) {
        if (text == null) {
            return null;
        }

        return HtmlUtils.htmlUnescape(text)
                .replace('\u00A0', ' ')
                .trim();
    }

    private static LocalDateTime toLocalDateTime(EventDateTime eventDateTime) {
        long millis = eventDateTime.getDateTime() != null
                ? eventDateTime.getDateTime().getValue()
                : eventDateTime.getDate().getValue();

        return LocalDateTime.ofInstant(
                java.time.Instant.ofEpochMilli(millis),
                ZoneId.systemDefault());
    }
}