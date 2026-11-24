package com.backend.gapfinder.model.services;

import com.backend.gapfinder.model.enums.FriendshipStatusEnum;
import com.backend.gapfinder.model.exceptions.AlreadyDoneException;
import com.backend.gapfinder.model.models.ChatMessageModel;
import com.backend.gapfinder.model.models.UserModel;
import com.backend.gapfinder.model.repositories.ChatMessageRepository;
import com.backend.gapfinder.model.repositories.FriendshipRepository;

import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

// Simple chat between friends: send a message and read the new ones (the app polls, no websockets)
@Slf4j
@Service
public class ChatService {

    // Max messages returned in one call
    static final int PAGE_SIZE = 50;

    // Max length of the client id (a UUID has 36 characters)
    static final int MAX_CLIENT_ID_LENGTH = 64;

    private final ChatMessageRepository chatMessageRepository;
    private final FriendshipRepository friendshipRepository;
    private final UserService userService;

    public ChatService(ChatMessageRepository chatMessageRepository,
                       FriendshipRepository friendshipRepository,
                       UserService userService) {
        this.chatMessageRepository = chatMessageRepository;
        this.friendshipRepository = friendshipRepository;
        this.userService = userService;
    }

    // Messages between the user and a friend with id greater than afterId, oldest first (max 50)
    @Transactional(readOnly = true)
    public List<ChatMessageModel> getConversation(Long userId, Long friendId, Long afterId) {
        log.info("Inicia proceso de consultar el chat entre {} y {} después del mensaje {}", userId, friendId, afterId);

        long after = (afterId == null || afterId < 0) ? 0L : afterId;
        return chatMessageRepository.findConversationAfter(userId, friendId, after, PageRequest.of(0, PAGE_SIZE));
    }

    // Sends a message from senderId to receiverId
    @Transactional
    public ChatMessageModel send(Long senderId, Long receiverId, String content, String clientId) {
        log.info("Inicia proceso de enviar un mensaje de {} a {}", senderId, receiverId);

        validateContent(content);
        validateClientId(clientId);
        if (receiverId == null) {
            throw new IllegalArgumentException("Falta el destinatario del mensaje");
        }
        // Business rule → 422
        if (!areFriends(senderId, receiverId)) {
            throw new IllegalStateException("Solo puedes enviar mensajes a tus amigos");
        }
        // Retry of a message that was already stored → 409 (the app outbox counts it as done)
        if (chatMessageRepository.existsBySenderIdAndClientId(senderId, clientId)) {
            throw new AlreadyDoneException("Este mensaje ya fue enviado");
        }

        UserModel sender = userService.getById(senderId);
        UserModel receiver = userService.getById(receiverId);

        ChatMessageModel message = new ChatMessageModel();
        message.setSender(sender);
        message.setReceiver(receiver);
        message.setContent(content);
        message.setClientId(clientId);
        message.setCreatedAt(LocalDateTime.now());

        try {
            return chatMessageRepository.saveAndFlush(message);
        } catch (DataIntegrityViolationException ex) {
            // Two retries arrived at the same time: the unique (sender_id, client_id) kept only one
            throw new AlreadyDoneException("Este mensaje ya fue enviado");
        }
    }

    // Only ACCEPTED friendships allow chatting
    private boolean areFriends(Long userA, Long userB) {
        return friendshipRepository.findBetweenUsers(userA, userB).stream()
                .anyMatch(f -> f.getStatus() == FriendshipStatusEnum.ACCEPTED);
    }

    private static void validateContent(String content) {
        if (content == null || content.isBlank()) {
            throw new IllegalArgumentException("El mensaje no puede estar vacío");
        }
        if (content.length() > ChatMessageModel.MAX_CONTENT_LENGTH) {
            throw new IllegalArgumentException("El mensaje no puede tener más de "
                    + ChatMessageModel.MAX_CONTENT_LENGTH + " caracteres");
        }
    }

    private static void validateClientId(String clientId) {
        if (clientId == null || clientId.isBlank()) {
            throw new IllegalArgumentException("Falta el clientId del mensaje");
        }
        if (clientId.length() > MAX_CLIENT_ID_LENGTH) {
            throw new IllegalArgumentException("El clientId no puede tener más de "
                    + MAX_CLIENT_ID_LENGTH + " caracteres");
        }
    }
}
