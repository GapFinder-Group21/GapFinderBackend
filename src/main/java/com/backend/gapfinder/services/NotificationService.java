package com.backend.gapfinder.services;

import com.backend.gapfinder.enums.NotificationTypeEnum;
import com.backend.gapfinder.exceptions.NotFoundException;
import com.backend.gapfinder.models.NotificationModel;
import com.backend.gapfinder.models.UserModel;
import com.backend.gapfinder.repositories.NotificationRepository;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final UserService userService;

    public NotificationService(NotificationRepository notificationRepository, UserService userService) {
        this.notificationRepository = notificationRepository;
        this.userService = userService;
    }

    // Get a notification by its id
    @Transactional(readOnly = true)
    public NotificationModel getById(Long id) {
        log.info("Inicia proceso de consultar la notificación con id = {}", id);
        return notificationRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("La notificación con id " + id + " no existe"));
    }

    // Create a notification for a user (called by the observers)
    @Transactional
    public NotificationModel create(Long recipientId, NotificationTypeEnum type, String message,
                                    Long relatedEntityId) {
        log.info("Inicia proceso de creación de una notificación de tipo {} para el usuario {}",
                type, recipientId);

        if (type == null) {
            throw new IllegalArgumentException("Debe indicar el tipo de la notificación");
        }

        if (message == null || message.isBlank()) {
            throw new IllegalArgumentException("El mensaje de la notificación es obligatorio");
        }

        UserModel recipient = userService.getById(recipientId);

        NotificationModel notification = new NotificationModel();
        notification.setRecipient(recipient);
        notification.setType(type);
        notification.setMessage(message);
        notification.setRelatedEntityId(relatedEntityId);
        notification.setRead(false);
        notification.setCreatedAt(LocalDateTime.now());

        NotificationModel saved = notificationRepository.save(notification);

        log.info("Termina proceso de creación de la notificación con id = {}", saved.getId());
        return saved;
    }

    // Get all notifications of a user, newest first
    @Transactional(readOnly = true)
    public List<NotificationModel> getByUser(Long userId) {
        log.info("Inicia proceso de consultar las notificaciones del usuario con id = {}", userId);

        userService.getById(userId);
        return notificationRepository.findByRecipientIdOrderByCreatedAtDesc(userId);
    }

    // Get only the unread notifications of a user, newest first (used by the front polling)
    @Transactional(readOnly = true)
    public List<NotificationModel> getUnreadByUser(Long userId) {
        log.info("Inicia proceso de consultar las notificaciones no leídas del usuario con id = {}", userId);

        userService.getById(userId);
        return notificationRepository.findByRecipientIdAndReadFalseOrderByCreatedAtDesc(userId);
    }

    // Mark one notification as read, only if it belongs to the user
    @Transactional
    public NotificationModel markAsRead(Long notificationId, Long userId) {
        log.info("Inicia proceso de marcar como leída la notificación {} del usuario {}", notificationId, userId);

        NotificationModel notification = getById(notificationId);

        if (!notification.getRecipient().getId().equals(userId)) {
            throw new IllegalArgumentException("La notificación no pertenece a este usuario");
        }

        notification.setRead(true);
        NotificationModel saved = notificationRepository.save(notification);

        log.info("Termina proceso de marcar como leída la notificación {}", notificationId);
        return saved;
    }

    // Mark all unread notifications of a user as read
    @Transactional
    public void markAllAsRead(Long userId) {
        log.info("Inicia proceso de marcar todas las notificaciones del usuario {} como leídas", userId);

        userService.getById(userId);

        List<NotificationModel> unread = notificationRepository
                .findByRecipientIdAndReadFalseOrderByCreatedAtDesc(userId);
        unread.forEach(n -> n.setRead(true));
        notificationRepository.saveAll(unread);

        log.info("Termina proceso de marcar todas las notificaciones del usuario {} como leídas", userId);
    }
}
