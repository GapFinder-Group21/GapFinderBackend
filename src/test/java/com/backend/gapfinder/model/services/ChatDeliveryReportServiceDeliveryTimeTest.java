package com.backend.gapfinder.model.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.backend.gapfinder.model.exceptions.NotFoundException;
import com.backend.gapfinder.model.repositories.ChatDeliveryReportRepository;
import com.backend.gapfinder.model.repositories.ChatMessageRepository;
import com.backend.gapfinder.model.repositories.projections.ChatDeliveryRangeProjection;
import com.backend.gapfinder.model.repositories.projections.ChatDeliveryRouteProjection;
import com.backend.gapfinder.view.dto.ChatDeliveryRangeDTO;
import com.backend.gapfinder.view.dto.ChatDeliveryTimeStatsDTO;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.LocalDateTime;
import java.util.List;

// BQ 17: how long chat messages take to reach the server, at once and through the offline queue
class ChatDeliveryReportServiceDeliveryTimeTest {

    private ChatDeliveryReportRepository reportRepository;
    private ChatDeliveryReportService service;

    @BeforeEach
    void setUp() {
        reportRepository = mock(ChatDeliveryReportRepository.class);
        service = new ChatDeliveryReportService(reportRepository, mock(ChatMessageRepository.class),
                mock(UserService.class));
    }

    private static ChatDeliveryRouteProjection route(String route, long messages, double medianMs, double p90Ms,
                                                     Double medianAfterReconnectMs) {
        ChatDeliveryRouteProjection row = mock(ChatDeliveryRouteProjection.class);
        when(row.getRoute()).thenReturn(route);
        when(row.getMessages()).thenReturn(messages);
        when(row.getMedianMs()).thenReturn(medianMs);
        when(row.getP90Ms()).thenReturn(p90Ms);
        when(row.getMedianAfterReconnectMs()).thenReturn(medianAfterReconnectMs);
        return row;
    }

    private static ChatDeliveryRangeProjection range(String range, long direct, long queued) {
        ChatDeliveryRangeProjection row = mock(ChatDeliveryRangeProjection.class);
        when(row.getDelayRange()).thenReturn(range);
        when(row.getDirect()).thenReturn(direct);
        when(row.getQueued()).thenReturn(queued);
        return row;
    }

    // The rows are built before the stubbing, because a mock cannot be stubbed inside another stubbing
    private void queriesReturn(List<ChatDeliveryRouteProjection> routes, List<ChatDeliveryRangeProjection> ranges) {
        when(reportRepository.summarizeByRoute(any())).thenReturn(routes);
        when(reportRepository.countByDelayRange(any())).thenReturn(ranges);
    }

    @Test
    void returnsTheTimesOfBothRoutes() {
        List<ChatDeliveryRouteProjection> routes = List.of(
                route("DIRECT", 40, 380.0, 1_900.0, null),
                route("QUEUED", 10, 95_000.0, 600_000.0, 4_200.0));
        List<ChatDeliveryRangeProjection> ranges = List.of(range("<1s", 30, 0), range("1-3s", 8, 0),
                range("3-10s", 2, 1), range("1-10min", 0, 6), range("10min+", 0, 3));
        queriesReturn(routes, ranges);

        ChatDeliveryTimeStatsDTO result = service.getDeliveryTime(LocalDateTime.now().minusDays(7));

        assertEquals(50, result.messages());
        assertEquals(40, result.direct().messages());
        assertEquals(380.0, result.direct().medianMs());
        assertEquals(1_900.0, result.direct().p90Ms());
        assertNull(result.direct().medianAfterReconnectMs());
        assertEquals(10, result.queued().messages());
        assertEquals(95_000.0, result.queued().medianMs());
        assertEquals(4_200.0, result.queued().medianAfterReconnectMs());
        assertEquals(20.0, result.queuedPercent(), 0.001); // 10 of 50
    }

    @Test
    void returnsTheSixRangesInNaturalOrderIncludingTheEmptyOnes() {
        List<ChatDeliveryRouteProjection> routes = List.of(route("DIRECT", 3, 500.0, 900.0, null));
        List<ChatDeliveryRangeProjection> ranges = List.of(range("1-3s", 1, 0), range("<1s", 2, 0));
        queriesReturn(routes, ranges);

        List<ChatDeliveryRangeDTO> distribution = service.getDeliveryTime(null).distribution();

        assertEquals(List.of("<1s", "1-3s", "3-10s", "10-60s", "1-10min", "10min+"),
                distribution.stream().map(ChatDeliveryRangeDTO::range).toList());
        assertEquals(2, distribution.get(0).direct());
        assertEquals(1, distribution.get(1).direct());
        assertEquals(0, distribution.get(2).direct());
        assertEquals(0, distribution.get(5).queued());
    }

    @Test
    void slowDirectMessagesAreTheOnesThatTookThreeSecondsOrMore() {
        // 10 direct: 6 under 1 s, 2 between 1 and 3 s, 1 between 3 and 10 s, 1 between 10 and 60 s → 2 slow = 20%
        // The queued ones never count as slow direct messages
        List<ChatDeliveryRouteProjection> routes = List.of(
                route("DIRECT", 10, 600.0, 4_000.0, null),
                route("QUEUED", 5, 120_000.0, 700_000.0, 2_000.0));
        List<ChatDeliveryRangeProjection> ranges = List.of(range("<1s", 6, 0), range("1-3s", 2, 0),
                range("3-10s", 1, 0), range("10-60s", 1, 1), range("1-10min", 0, 3), range("10min+", 0, 1));
        queriesReturn(routes, ranges);

        assertEquals(20.0, service.getDeliveryTime(null).slowDirectPercent(), 0.001);
    }

    @Test
    void aRouteWithoutReportsHasZeroMessagesAndNoTimes() {
        List<ChatDeliveryRouteProjection> routes = List.of(route("QUEUED", 4, 30_000.0, 50_000.0, 1_500.0));
        List<ChatDeliveryRangeProjection> ranges = List.of(range("10-60s", 0, 4));
        queriesReturn(routes, ranges);

        ChatDeliveryTimeStatsDTO result = service.getDeliveryTime(null);

        assertEquals(0, result.direct().messages());
        assertNull(result.direct().medianMs());
        assertNull(result.direct().p90Ms());
        assertEquals(0.0, result.slowDirectPercent()); // no direct messages: 0, not a division by zero
        assertEquals(100.0, result.queuedPercent(), 0.001);
    }

    @Test
    void withoutReportsThereIsNoAnswer() {
        queriesReturn(List.of(), List.of());

        assertThrows(NotFoundException.class, () -> service.getDeliveryTime(null));
    }

    @Test
    void withoutADateItUsesTheLastThirtyDays() {
        List<ChatDeliveryRouteProjection> routes = List.of(route("DIRECT", 1, 300.0, 300.0, null));
        List<ChatDeliveryRangeProjection> ranges = List.of(range("<1s", 1, 0));
        queriesReturn(routes, ranges);

        service.getDeliveryTime(null);

        ArgumentCaptor<LocalDateTime> start = ArgumentCaptor.forClass(LocalDateTime.class);
        verify(reportRepository).summarizeByRoute(start.capture());
        LocalDateTime expected = LocalDateTime.now().minusDays(30);
        assertTrue(start.getValue().isAfter(expected.minusMinutes(1)) && start.getValue().isBefore(expected.plusMinutes(1)));
    }
}
