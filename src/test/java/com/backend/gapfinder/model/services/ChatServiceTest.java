package com.backend.gapfinder.model.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.backend.gapfinder.model.enums.FriendshipStatusEnum;
import com.backend.gapfinder.model.exceptions.AlreadyDoneException;
import com.backend.gapfinder.model.models.ChatMessageModel;
import com.backend.gapfinder.model.models.FriendshipModel;
import com.backend.gapfinder.model.models.UserModel;
import com.backend.gapfinder.model.repositories.ChatMessageRepository;
import com.backend.gapfinder.model.repositories.FriendshipRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Pageable;

import java.util.List;

class ChatServiceTest {

    private ChatMessageRepository messages;
    private FriendshipRepository friendships;
    private UserService userService;
    private ChatService service;

    @BeforeEach
    void setUp() {
        messages = mock(ChatMessageRepository.class);
        friendships = mock(FriendshipRepository.class);
        userService = mock(UserService.class);
        service = new ChatService(messages, friendships, userService);

        when(userService.getById(1L)).thenReturn(user(1L));
        when(userService.getById(2L)).thenReturn(user(2L));
        when(messages.saveAndFlush(any(ChatMessageModel.class))).thenAnswer(inv -> {
            ChatMessageModel saved = inv.getArgument(0);
            saved.setId(100L);
            return saved;
        });
    }

    private static UserModel user(Long id) {
        UserModel user = new UserModel();
        user.setId(id);
        return user;
    }

    private void friendshipWithStatus(FriendshipStatusEnum status) {
        FriendshipModel friendship = new FriendshipModel();
        friendship.setRequester(user(1L));
        friendship.setReceiver(user(2L));
        friendship.setStatus(status);
        when(friendships.findBetweenUsers(1L, 2L)).thenReturn(List.of(friendship));
    }

    @Test
    void aFriendCanSendAMessage() {
        friendshipWithStatus(FriendshipStatusEnum.ACCEPTED);

        ChatMessageModel sent = service.send(1L, 2L, "hola", "uuid-1");

        assertEquals(100L, sent.getId());
        assertEquals(1L, sent.getSender().getId());
        assertEquals(2L, sent.getReceiver().getId());
        assertEquals("hola", sent.getContent());
        assertEquals("uuid-1", sent.getClientId());
    }

    @Test
    void sendingToSomeoneWhoIsNotAFriendIsARefusal() {
        when(friendships.findBetweenUsers(1L, 2L)).thenReturn(List.of());

        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> service.send(1L, 2L, "hola", "uuid-1"));

        // Plain IllegalStateException answers 422, not 409
        assertEquals(IllegalStateException.class, ex.getClass());
        verify(messages, never()).saveAndFlush(any());
    }

    @Test
    void aPendingFriendRequestDoesNotAllowChatting() {
        friendshipWithStatus(FriendshipStatusEnum.PENDING);

        assertThrows(IllegalStateException.class, () -> service.send(1L, 2L, "hola", "uuid-1"));
    }

    @Test
    void resendingTheSameClientIdIsAlreadyDone() {
        friendshipWithStatus(FriendshipStatusEnum.ACCEPTED);
        when(messages.existsBySenderIdAndClientId(1L, "uuid-1")).thenReturn(true);

        assertThrows(AlreadyDoneException.class, () -> service.send(1L, 2L, "hola", "uuid-1"));
        verify(messages, never()).saveAndFlush(any());
    }

    @Test
    void twoRetriesAtTheSameTimeAreAlreadyDone() {
        friendshipWithStatus(FriendshipStatusEnum.ACCEPTED);
        when(messages.saveAndFlush(any(ChatMessageModel.class)))
                .thenThrow(new DataIntegrityViolationException("unique sender_id, client_id"));

        assertThrows(AlreadyDoneException.class, () -> service.send(1L, 2L, "hola", "uuid-1"));
    }

    @Test
    void anEmptyMessageIsABadRequest() {
        friendshipWithStatus(FriendshipStatusEnum.ACCEPTED);

        assertThrows(IllegalArgumentException.class, () -> service.send(1L, 2L, "   ", "uuid-1"));
        assertThrows(IllegalArgumentException.class, () -> service.send(1L, 2L, null, "uuid-1"));
    }

    @Test
    void aMessageLongerThan500IsABadRequest() {
        friendshipWithStatus(FriendshipStatusEnum.ACCEPTED);

        assertThrows(IllegalArgumentException.class,
                () -> service.send(1L, 2L, "a".repeat(501), "uuid-1"));
    }

    @Test
    void aMessageWithoutClientIdIsABadRequest() {
        friendshipWithStatus(FriendshipStatusEnum.ACCEPTED);

        assertThrows(IllegalArgumentException.class, () -> service.send(1L, 2L, "hola", " "));
    }

    @Test
    void theConversationOnlyAsksForMessagesAfterTheGivenId() {
        ChatMessageModel newer = new ChatMessageModel();
        newer.setId(8L);
        when(messages.findConversationAfter(eq(1L), eq(2L), eq(7L), any(Pageable.class)))
                .thenReturn(List.of(newer));

        List<ChatMessageModel> result = service.getConversation(1L, 2L, 7L);

        assertEquals(List.of(newer), result);
        verify(messages).findConversationAfter(eq(1L), eq(2L), eq(7L), any(Pageable.class));
    }

    @Test
    void withoutAfterIdTheWholeConversationIsAskedWithAPageOf50() {
        service.getConversation(1L, 2L, null);

        verify(messages).findConversationAfter(eq(1L), eq(2L), eq(0L),
                eq(org.springframework.data.domain.PageRequest.of(0, ChatService.PAGE_SIZE)));
    }
}
