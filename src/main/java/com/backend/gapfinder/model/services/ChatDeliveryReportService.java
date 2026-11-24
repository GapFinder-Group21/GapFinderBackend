package com.backend.gapfinder.model.services;

import com.backend.gapfinder.model.enums.ChatDeliveryRouteEnum;
import com.backend.gapfinder.model.exceptions.AlreadyDoneException;
import com.backend.gapfinder.model.models.ChatDeliveryReportModel;
import com.backend.gapfinder.model.repositories.ChatDeliveryReportRepository;
import com.backend.gapfinder.model.repositories.ChatMessageRepository;
import com.backend.gapfinder.view.dto.requests.ChatDeliveryReportRequest;

import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

// BQ 17 INDIVIDUAL TYPE 1: registration of the chat delivery reports sent by the app
// (the same split as BQ 2, where OpenTableAbandonmentService registers the abandonments)
@Slf4j
@Service
public class ChatDeliveryReportService {

    // A report can be at most this old and a message can take at most this long: the app queue gives up
    // long before (8 attempts with exponential backoff), so anything longer is not believable
    static final Duration MAX_AGE = Duration.ofDays(7);

    private final ChatDeliveryReportRepository reportRepository;
    private final ChatMessageRepository chatMessageRepository;
    private final UserService userService;

    // Dependency injection constructor
    public ChatDeliveryReportService(ChatDeliveryReportRepository reportRepository,
                                     ChatMessageRepository chatMessageRepository,
                                     UserService userService) {
        this.reportRepository = reportRepository;
        this.chatMessageRepository = chatMessageRepository;
        this.userService = userService;
    }

    // Saves how long a chat message of the user took to reach the server
    // 400: invalid data. 422: the user did not send a message with that clientId. 409: already reported
    @Transactional
    public ChatDeliveryReportModel register(Long userId, ChatDeliveryReportRequest request) {
        log.info("Inicia proceso de registrar el tiempo de entrega de un mensaje del usuario {}", userId);

        if (request == null) {
            throw new IllegalArgumentException("Falta el reporte de entrega");
        }
        String clientId = validateClientId(request.getClientId());
        ChatDeliveryRouteEnum route = parseRoute(request.getRoute());
        long elapsedMs = validateElapsed(request.getElapsedMs());
        Long afterReconnectMs = resolveAfterReconnect(route, request.getAfterReconnectMs(), elapsedMs);

        // Only messages that the server really saved count, and only the sender can report them → 422
        if (!chatMessageRepository.existsBySenderIdAndClientId(userId, clientId)) {
            throw new IllegalStateException("No enviaste un mensaje con ese clientId");
        }
        // The app queue can send the same report twice → 409 (the app takes it as done)
        if (reportRepository.existsByUserIdAndClientId(userId, clientId)) {
            throw new AlreadyDoneException("Este mensaje ya fue reportado");
        }

        LocalDateTime now = LocalDateTime.now();
        ChatDeliveryReportModel report = new ChatDeliveryReportModel();
        report.setUser(userService.getById(userId));
        report.setClientId(clientId);
        report.setRoute(route);
        report.setElapsedMs(elapsedMs);
        report.setAfterReconnectMs(afterReconnectMs);
        report.setTappedAt(resolveTappedAt(request.getTappedAt(), elapsedMs, now));
        report.setCreatedAt(now);

        ChatDeliveryReportModel saved;
        try {
            saved = reportRepository.saveAndFlush(report);
        } catch (DataIntegrityViolationException ex) {
            // Two copies arrived at the same time: the unique (user_id, client_id) kept only one
            throw new AlreadyDoneException("Este mensaje ya fue reportado");
        }

        log.info("Termina proceso de registrar el tiempo de entrega de un mensaje del usuario {} ({} en {} ms)",
                userId, route, elapsedMs);
        return saved;
    }

    // The client id must be the one of a chat message: not blank and at most 64 characters
    private static String validateClientId(String clientId) {
        if (clientId == null || clientId.isBlank()) {
            throw new IllegalArgumentException("Falta el clientId del mensaje");
        }
        if (clientId.length() > ChatService.MAX_CLIENT_ID_LENGTH) {
            throw new IllegalArgumentException("El clientId no puede tener más de "
                    + ChatService.MAX_CLIENT_ID_LENGTH + " caracteres");
        }
        return clientId;
    }

    // DIRECT or QUEUED, without caring about upper or lower case
    static ChatDeliveryRouteEnum parseRoute(String route) {
        if (route != null) {
            for (ChatDeliveryRouteEnum value : ChatDeliveryRouteEnum.values()) {
                if (value.name().equalsIgnoreCase(route.trim())) {
                    return value;
                }
            }
        }
        throw new IllegalArgumentException("La ruta debe ser DIRECT o QUEUED");
    }

    // The delivery time cannot be negative (the phone clock changed in the middle) or longer than 7 days
    private static long validateElapsed(Long elapsedMs) {
        if (elapsedMs == null || elapsedMs < 0 || elapsedMs > MAX_AGE.toMillis()) {
            throw new IllegalArgumentException("El tiempo de entrega debe estar entre 0 ms y 7 días");
        }
        return elapsedMs;
    }

    // A DIRECT message never waited for the connection, so its value is ignored. For a QUEUED one the time
    // after the connection came back is part of the whole wait, so it cannot be longer than it
    static Long resolveAfterReconnect(ChatDeliveryRouteEnum route, Long afterReconnectMs, long elapsedMs) {
        if (route == ChatDeliveryRouteEnum.DIRECT || afterReconnectMs == null) {
            return null;
        }
        if (afterReconnectMs < 0 || afterReconnectMs > elapsedMs) {
            throw new IllegalArgumentException("El tiempo después de volver la conexión debe estar entre 0 y el tiempo de entrega");
        }
        return afterReconnectMs;
    }

    // The time that is saved: the one from the phone when it is believable, otherwise the server time minus the
    // delivery time (when the tap should have happened)
    static LocalDateTime resolveTappedAt(LocalDateTime requested, long elapsedMs, LocalDateTime now) {
        if (requested == null || requested.isAfter(now) || requested.isBefore(now.minus(MAX_AGE))) {
            return now.minus(elapsedMs, ChronoUnit.MILLIS);
        }
        return requested;
    }
}
