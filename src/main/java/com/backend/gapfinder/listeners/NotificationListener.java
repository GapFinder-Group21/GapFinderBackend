package com.backend.gapfinder.listeners;

import com.backend.gapfinder.events.FriendshipEvent;
import com.backend.gapfinder.events.MatchEvent;
import com.backend.gapfinder.events.OpenTableEvent;
import com.backend.gapfinder.services.NotificationService;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

// Observer: reacts to domain events and creates the notification for the recipient
@Slf4j
@Component
public class NotificationListener {

    private final NotificationService notificationService;

    public NotificationListener(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @EventListener
    public void onFriendshipEvent(FriendshipEvent event) {
        log.info("Evento de amistad recibido: {} para el usuario {}", event.type(), event.recipientId());

        String message = switch (event.type()) {
            case FRIEND_REQUEST_SENT -> event.actorName() + " te envió una solicitud de amistad";
            case FRIEND_REQUEST_ACCEPTED -> event.actorName() + " aceptó tu solicitud de amistad";
            case FRIEND_REQUEST_REJECTED -> event.actorName() + " rechazó tu solicitud de amistad";
            default -> throw new IllegalArgumentException("Tipo no válido para un evento de amistad: " + event.type());
        };

        notificationService.create(event.recipientId(), event.type(), message, event.friendshipId());
    }

    @EventListener
    public void onMatchEvent(MatchEvent event) {
        log.info("Evento de match recibido: {} para el usuario {}", event.type(), event.recipientId());

        String message = switch (event.type()) {
            case MATCH_PROPOSED -> event.actorName() + " te propuso un match";
            case MATCH_ACCEPTED -> event.actorName() + " aceptó tu match";
            case MATCH_REJECTED -> event.actorName() + " rechazó tu match";
            default -> throw new IllegalArgumentException("Tipo no válido para un evento de match: " + event.type());
        };

        notificationService.create(event.recipientId(), event.type(), message, event.matchId());
    }

    @EventListener
    public void onOpenTableEvent(OpenTableEvent event) {
        log.info("Evento de open table recibido: {} para el usuario {}", event.type(), event.recipientId());

        String message = switch (event.type()) {
            case OPEN_TABLE_JOINED ->
                    event.actorName() + " se unió a tu open table \"" + event.openTableTitle() + "\"";
            default -> throw new IllegalArgumentException("Tipo no válido para un evento de open table: " + event.type());
        };

        notificationService.create(event.recipientId(), event.type(), message, event.openTableId());
    }
}
