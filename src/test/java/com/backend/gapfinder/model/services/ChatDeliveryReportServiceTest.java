package com.backend.gapfinder.model.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.backend.gapfinder.model.enums.ChatDeliveryRouteEnum;
import com.backend.gapfinder.model.exceptions.AlreadyDoneException;
import com.backend.gapfinder.model.models.ChatDeliveryReportModel;
import com.backend.gapfinder.model.models.UserModel;
import com.backend.gapfinder.model.repositories.ChatDeliveryReportRepository;
import com.backend.gapfinder.model.repositories.ChatMessageRepository;
import com.backend.gapfinder.view.dto.requests.ChatDeliveryReportRequest;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.dao.DataIntegrityViolationException;

import java.time.LocalDateTime;

// BQ 17: the app reports how long each chat message took to reach the server
class ChatDeliveryReportServiceTest {

    private static final Long ME = 5L;
    private static final String CLIENT_ID = "6f1c2b9e-1111-4c1d-9a55-0d6f2f3a7b10";
    private static final LocalDateTime NOW = LocalDateTime.of(2026, 11, 24, 10, 0);

    private ChatDeliveryReportRepository reportRepository;
    private ChatMessageRepository chatMessageRepository;
    private ChatDeliveryReportService service;

    @BeforeEach
    void setUp() {
        reportRepository = mock(ChatDeliveryReportRepository.class);
        chatMessageRepository = mock(ChatMessageRepository.class);
        UserService userService = mock(UserService.class);
        UserModel user = new UserModel();
        user.setId(ME);
        when(userService.getById(ME)).thenReturn(user);
        when(chatMessageRepository.existsBySenderIdAndClientId(ME, CLIENT_ID)).thenReturn(true);
        when(reportRepository.saveAndFlush(any(ChatDeliveryReportModel.class))).thenAnswer(inv -> inv.getArgument(0));
        service = new ChatDeliveryReportService(reportRepository, chatMessageRepository, userService);
    }

    private static ChatDeliveryReportRequest request(String route, Long elapsedMs, Long afterReconnectMs) {
        ChatDeliveryReportRequest request = new ChatDeliveryReportRequest();
        request.setClientId(CLIENT_ID);
        request.setRoute(route);
        request.setElapsedMs(elapsedMs);
        request.setAfterReconnectMs(afterReconnectMs);
        request.setTappedAt(LocalDateTime.now().minusMinutes(2));
        return request;
    }

    private ChatDeliveryReportModel saved() {
        ArgumentCaptor<ChatDeliveryReportModel> captor = ArgumentCaptor.forClass(ChatDeliveryReportModel.class);
        verify(reportRepository).saveAndFlush(captor.capture());
        return captor.getValue();
    }

    @Test
    void aQueuedReportKeepsBothTimesOfThePhone() {
        ChatDeliveryReportRequest request = request("QUEUED", 94_200L, 3_100L);

        service.register(ME, request);

        ChatDeliveryReportModel report = saved();
        assertEquals(ME, report.getUser().getId());
        assertEquals(CLIENT_ID, report.getClientId());
        assertEquals(ChatDeliveryRouteEnum.QUEUED, report.getRoute());
        assertEquals(94_200L, report.getElapsedMs());
        assertEquals(3_100L, report.getAfterReconnectMs());
        assertEquals(request.getTappedAt(), report.getTappedAt());
    }

    @Test
    void aDirectReportIgnoresTheTimeAfterTheConnectionCameBack() {
        service.register(ME, request("direct", 420L, 200L));

        ChatDeliveryReportModel report = saved();
        assertEquals(ChatDeliveryRouteEnum.DIRECT, report.getRoute());
        assertNull(report.getAfterReconnectMs());
    }

    @Test
    void anUnknownRouteIsABadRequest() {
        assertThrows(IllegalArgumentException.class, () -> service.register(ME, request("PUSH", 420L, null)));
        assertThrows(IllegalArgumentException.class, () -> service.register(ME, request(null, 420L, null)));
    }

    @Test
    void aNegativeMissingOrTooLongDeliveryTimeIsABadRequest() {
        assertThrows(IllegalArgumentException.class, () -> service.register(ME, request("DIRECT", -1L, null)));
        assertThrows(IllegalArgumentException.class, () -> service.register(ME, request("DIRECT", null, null)));
        long eightDays = 8L * 24 * 60 * 60 * 1000;
        assertThrows(IllegalArgumentException.class, () -> service.register(ME, request("QUEUED", eightDays, null)));
    }

    @Test
    void theTimeAfterReconnectingCannotBeLongerThanTheWholeWait() {
        assertThrows(IllegalArgumentException.class, () -> service.register(ME, request("QUEUED", 5_000L, 6_000L)));
        assertThrows(IllegalArgumentException.class, () -> service.register(ME, request("QUEUED", 5_000L, -1L)));
    }

    @Test
    void aMessageTheUserDidNotSendCannotBeReported() {
        when(chatMessageRepository.existsBySenderIdAndClientId(ME, CLIENT_ID)).thenReturn(false);

        assertThrows(IllegalStateException.class, () -> service.register(ME, request("DIRECT", 420L, null)));
        verify(reportRepository, never()).saveAndFlush(any());
    }

    @Test
    void aMessageReportedTwiceIsAConflict() {
        when(reportRepository.existsByUserIdAndClientId(ME, CLIENT_ID)).thenReturn(true);

        assertThrows(AlreadyDoneException.class, () -> service.register(ME, request("QUEUED", 94_200L, 3_100L)));
        verify(reportRepository, never()).saveAndFlush(any());
    }

    @Test
    void twoCopiesAtTheSameTimeAreAConflict() {
        when(reportRepository.saveAndFlush(any(ChatDeliveryReportModel.class)))
                .thenThrow(new DataIntegrityViolationException("duplicate key"));

        assertThrows(AlreadyDoneException.class, () -> service.register(ME, request("DIRECT", 420L, null)));
    }

    @Test
    void theTapTimeOfThePhoneIsKeptWhenItIsBelievable() {
        LocalDateTime tap = NOW.minusMinutes(3);

        assertEquals(tap, ChatDeliveryReportService.resolveTappedAt(tap, 1_000L, NOW));
    }

    @Test
    void withoutATapTimeOrWithAWrongOneTheServerUsesNowMinusTheDeliveryTime() {
        assertEquals(NOW.minusSeconds(90), ChatDeliveryReportService.resolveTappedAt(null, 90_000L, NOW));
        assertEquals(NOW.minusSeconds(90), ChatDeliveryReportService.resolveTappedAt(NOW.plusHours(1), 90_000L, NOW));
        assertEquals(NOW.minusSeconds(90), ChatDeliveryReportService.resolveTappedAt(NOW.minusDays(8), 90_000L, NOW));
    }
}
