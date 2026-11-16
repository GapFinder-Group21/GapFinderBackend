package com.backend.gapfinder.controller.controllers;

import com.backend.gapfinder.model.exceptions.ForbiddenException;
import com.backend.gapfinder.model.models.UserModel;
import com.backend.gapfinder.model.services.ChatService;
import com.backend.gapfinder.security.AccessGuard;
import com.backend.gapfinder.view.dto.ChatMessageDTO;
import com.backend.gapfinder.view.dto.requests.ChatMessageRequest;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/chats")
public class ChatController {

    private final ChatService chatService;

    public ChatController(ChatService chatService) {
        this.chatService = chatService;
    }

    // Messages between the user and a friend newer than afterId (oldest first, max 50)
    // GET /chats/user/{userId}/with/{friendId}?afterId=0
    @GetMapping("/user/{userId}/with/{friendId}")
    public List<ChatMessageDTO> getConversation(@PathVariable Long userId,
                                                @PathVariable Long friendId,
                                                @RequestParam(defaultValue = "0") Long afterId,
                                                @AuthenticationPrincipal UserModel me) {
        // A user can only read their own chats
        AccessGuard.requireSameUser(me, userId);
        return chatService.getConversation(userId, friendId, afterId).stream()
                .map(ChatMessageDTO::from)
                .toList();
    }

    // Sends a message to a friend; the sender is always the authenticated user
    // POST /chats/messages body {receiverId, content, clientId}
    @PostMapping("/messages")
    @ResponseStatus(HttpStatus.CREATED)
    public ChatMessageDTO sendMessage(@RequestBody ChatMessageRequest request,
                                      @AuthenticationPrincipal UserModel me) {
        if (me == null) {
            throw new ForbiddenException("No tienes permiso para enviar mensajes");
        }
        return ChatMessageDTO.from(chatService.send(
                me.getId(), request.getReceiverId(), request.getContent(), request.getClientId()));
    }
}
