package com.backend.gapfinder.controller.controllers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.backend.gapfinder.model.exceptions.ForbiddenException;
import com.backend.gapfinder.model.models.ChatMessageModel;
import com.backend.gapfinder.model.models.UserModel;
import com.backend.gapfinder.model.services.ChatService;
import com.backend.gapfinder.view.dto.ChatMessageDTO;
import com.backend.gapfinder.view.dto.requests.ChatMessageRequest;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;

class ChatControllerTest {

    private ChatService service;
    private ChatController controller;
    private UserModel me;
    private UserModel stranger;

    @BeforeEach
    void setUp() {
        service = mock(ChatService.class);
        controller = new ChatController(service);
        me = user(1L);
        stranger = user(3L);
    }

    private static UserModel user(Long id) {
        UserModel user = new UserModel();
        user.setId(id);
        return user;
    }

    private static ChatMessageModel message(Long id, UserModel sender, UserModel receiver) {
        ChatMessageModel message = new ChatMessageModel();
        message.setId(id);
        message.setSender(sender);
        message.setReceiver(receiver);
        message.setContent("hola");
        message.setClientId("uuid-" + id);
        message.setCreatedAt(LocalDateTime.now());
        return message;
    }

    @Test
    void aUserReadsTheirChatWithAFriend() {
        when(service.getConversation(1L, 2L, 0L)).thenReturn(List.of(message(5L, me, user(2L))));

        List<ChatMessageDTO> result = controller.getConversation(1L, 2L, 0L, me);

        assertEquals(1, result.size());
        assertEquals(5L, result.get(0).getId());
        assertEquals("uuid-5", result.get(0).getClientId());
        assertEquals(1L, result.get(0).getSenderId());
        assertEquals(2L, result.get(0).getReceiverId());
    }

    @Test
    void aUserCannotReadTheChatOfAnotherUser() {
        assertThrows(ForbiddenException.class, () -> controller.getConversation(1L, 2L, 0L, stranger));
        verify(service, never()).getConversation(anyLong(), anyLong(), anyLong());
    }

    @Test
    void theSenderIsAlwaysTheAuthenticatedUser() {
        ChatMessageRequest request = new ChatMessageRequest();
        request.setReceiverId(2L);
        request.setContent("hola");
        request.setClientId("uuid-9");
        when(service.send(1L, 2L, "hola", "uuid-9")).thenReturn(message(9L, me, user(2L)));

        ChatMessageDTO sent = controller.sendMessage(request, me);

        assertEquals(9L, sent.getId());
        verify(service).send(1L, 2L, "hola", "uuid-9");
    }

    @Test
    void sendingWithoutAnAuthenticatedUserIsForbidden() {
        assertThrows(ForbiddenException.class, () -> controller.sendMessage(new ChatMessageRequest(), null));
        verify(service, never()).send(any(), any(), any(), any());
    }
}
