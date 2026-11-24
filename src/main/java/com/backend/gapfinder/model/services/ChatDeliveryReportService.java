package com.backend.gapfinder.model.services;

import com.backend.gapfinder.model.enums.ChatDeliveryRouteEnum;
import com.backend.gapfinder.model.exceptions.AlreadyDoneException;
import com.backend.gapfinder.model.exceptions.NotFoundException;
import com.backend.gapfinder.model.models.ChatDeliveryReportModel;
import com.backend.gapfinder.model.repositories.ChatDeliveryReportRepository;
import com.backend.gapfinder.model.repositories.ChatMessageRepository;
import com.backend.gapfinder.model.repositories.projections.ChatDeliveryRangeProjection;
import com.backend.gapfinder.model.repositories.projections.ChatDeliveryRouteProjection;
import com.backend.gapfinder.view.dto.ChatDeliveryRangeDTO;
import com.backend.gapfinder.view.dto.ChatDeliveryRouteStatsDTO;
import com.backend.gapfinder.view.dto.ChatDeliveryTimeStatsDTO;
import com.backend.gapfinder.view.dto.requests.ChatDeliveryReportRequest;

import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

// BQ 17 INDIVIDUAL TYPE 1: registration and analytics of the chat delivery reports sent by the app
// (the same split as BQ 2, where OpenTableAbandonmentService registers the abandonments and counts them).
// AnalyticsService already has 9 repositories, so this question lives with its own data
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

    // ==================== BQ 17 INDIVIDUAL TYPE 1 ====================
    // Daniela Martínez: How long does a chat message take from the tap on Send until the server saves it,
    // when it goes out at once and when it waits in the offline queue?

    // Days used as the default start date, the same as the dashboard
    private static final int DEFAULT_DAYS_BACK = 30;

    // Ranges of delivery time in natural order (the same labels as the query)
    static final List<String> DELAY_RANGES = List.of("<1s", "1-3s", "3-10s", "10-60s", "1-10min", "10min+");

    // From this range on, a message sent at once is slow: the student sees the clock for 3 s or more
    static final String FIRST_SLOW_RANGE = "3-10s";

    // Times of each route, the share of queued messages, the share of slow direct messages and the messages per range
    // It decides about the app itself: a high share of slow direct messages points to the network or the server,
    // and a long time after reconnecting points to the outbox (WorkManager) taking too long to send the queue
    // since: optional, last 30 days if null (when Send was tapped)
    @Transactional(readOnly = true)
    public ChatDeliveryTimeStatsDTO getDeliveryTime(LocalDateTime since) {
        LocalDateTime start = since != null ? since : LocalDateTime.now().minusDays(DEFAULT_DAYS_BACK);

        log.info("Inicia proceso de consultar el tiempo de entrega de los mensajes del chat desde {}", start);

        Map<String, ChatDeliveryRouteProjection> rowsByRoute = new LinkedHashMap<>();
        reportRepository.summarizeByRoute(start).forEach(row -> rowsByRoute.put(row.getRoute(), row));
        ChatDeliveryRouteStatsDTO direct = routeStats(ChatDeliveryRouteEnum.DIRECT, rowsByRoute);
        ChatDeliveryRouteStatsDTO queued = routeStats(ChatDeliveryRouteEnum.QUEUED, rowsByRoute);

        long messages = direct.messages() + queued.messages();
        if (messages == 0) {
            throw new NotFoundException("No hay reportes de entrega de mensajes desde la fecha indicada");
        }

        // Messages per range, with 0 for the ranges that do not appear in the query
        Map<String, ChatDeliveryRangeProjection> rowsByRange = new LinkedHashMap<>();
        reportRepository.countByDelayRange(start).forEach(row -> rowsByRange.put(row.getDelayRange(), row));
        List<ChatDeliveryRangeDTO> distribution = DELAY_RANGES.stream()
                .map(range -> {
                    ChatDeliveryRangeProjection row = rowsByRange.get(range);
                    return new ChatDeliveryRangeDTO(range,
                            row != null && row.getDirect() != null ? row.getDirect() : 0L,
                            row != null && row.getQueued() != null ? row.getQueued() : 0L);
                })
                .toList();

        // Direct messages that took 3 s or more: the ranges from '3-10s' on
        long slowDirect = distribution.stream()
                .skip(DELAY_RANGES.indexOf(FIRST_SLOW_RANGE))
                .mapToLong(ChatDeliveryRangeDTO::direct)
                .sum();
        double slowDirectPercent = direct.messages() == 0 ? 0.0 : (double) slowDirect / direct.messages() * 100;
        double queuedPercent = (double) queued.messages() / messages * 100;

        log.info("Termina proceso de consultar el tiempo de entrega de los mensajes del chat desde {}", start);
        return new ChatDeliveryTimeStatsDTO(messages, queuedPercent, slowDirectPercent, direct, queued, distribution);
    }

    // The times of a route, or 0 messages and no times if the query did not return it
    private static ChatDeliveryRouteStatsDTO routeStats(ChatDeliveryRouteEnum route,
                                                        Map<String, ChatDeliveryRouteProjection> rowsByRoute) {
        ChatDeliveryRouteProjection row = rowsByRoute.get(route.name());
        long count = row != null && row.getMessages() != null ? row.getMessages() : 0L;
        if (count == 0) {
            return new ChatDeliveryRouteStatsDTO(route.name(), 0L, null, null, null);
        }
        return new ChatDeliveryRouteStatsDTO(route.name(), count, row.getMedianMs(), row.getP90Ms(),
                route == ChatDeliveryRouteEnum.QUEUED ? row.getMedianAfterReconnectMs() : null);
    }

    // ================== END BQ 17 INDIVIDUAL TYPE 1 ==================
}
