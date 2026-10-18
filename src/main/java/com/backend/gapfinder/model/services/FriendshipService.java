package com.backend.gapfinder.model.services;

import com.backend.gapfinder.model.enums.FriendshipStatusEnum;
import com.backend.gapfinder.model.enums.NotificationTypeEnum;
import com.backend.gapfinder.model.events.FriendshipEvent;
import com.backend.gapfinder.model.exceptions.ForbiddenException;
import com.backend.gapfinder.model.exceptions.NotFoundException;
import com.backend.gapfinder.model.models.FriendshipModel;
import com.backend.gapfinder.model.models.UserModel;
import com.backend.gapfinder.model.repositories.FriendshipRepository;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
public class FriendshipService {

    private final FriendshipRepository friendshipRepository;
    private final UserService userService;
    private final ApplicationEventPublisher eventPublisher;

    public FriendshipService(FriendshipRepository friendshipRepository, UserService userService,
                             ApplicationEventPublisher eventPublisher) {
        this.friendshipRepository = friendshipRepository;
        this.userService = userService;
        this.eventPublisher = eventPublisher;
    }

    // Get a friendship by its id
    @Transactional
    public FriendshipModel getById(Long id) {
        log.info("Inicia proceso de consultar la amistad con id = {}", id);
        return friendshipRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("La amistad con id " + id + " no existe"));
    }

    // Get all friendships
    @Transactional
    public List<FriendshipModel> getAll() {
        log.info("Inicia proceso de consultar todas las amistades");
        return friendshipRepository.findAll();
    }

    // Create a new friendship request and notify the receiver
    // Every request starts PENDING; the receiver accepts or rejects it
    @Transactional
    public FriendshipModel create(FriendshipModel friendship) {
        log.info("Inicia proceso de creación de una amistad");

        validateFriendshipData(friendship);

        UserModel requester = userService.getById(friendship.getRequester().getId());
        UserModel receiver = userService.getById(friendship.getReceiver().getId());

        // Two users have at most one live request or friendship, in either direction
        List<FriendshipModel> existing = friendshipRepository.findBetweenUsers(requester.getId(), receiver.getId());
        boolean alreadyLinked = existing.stream()
                .anyMatch(f -> f.getStatus() != FriendshipStatusEnum.REJECTED);
        if (alreadyLinked) {
            throw new IllegalStateException("Ya existe una solicitud de amistad o una amistad entre estos usuarios");
        }

        // A rejected request does not block a new one: the old record is replaced
        if (!existing.isEmpty()) {
            friendshipRepository.deleteAll(existing);
            friendshipRepository.flush();
        }

        friendship.setId(null);
        friendship.setRequester(requester);
        friendship.setReceiver(receiver);
        friendship.setStatus(FriendshipStatusEnum.PENDING);
        friendship.setCreatedAt(LocalDateTime.now());

        FriendshipModel saved = friendshipRepository.save(friendship);

        eventPublisher.publishEvent(new FriendshipEvent(
                NotificationTypeEnum.FRIEND_REQUEST_SENT,
                receiver.getId(),
                requester.getName(),
                saved.getId()));

        log.info("Termina proceso de creación de una amistad");
        return saved;
    }

    // Update the data from an existing friendship
    // The status is not edited here: it changes through accept and reject
    @Transactional
    public FriendshipModel update(Long id, FriendshipModel friendship) {
        log.info("Inicia proceso de actualización de la amistad con id = {}", id);

        FriendshipModel existente = getById(id);
        validateFriendshipData(friendship);

        UserModel requester = userService.getById(friendship.getRequester().getId());
        UserModel receiver = userService.getById(friendship.getReceiver().getId());

        // The new pair of users cannot already have another request or friendship
        boolean pairTaken = friendshipRepository.findBetweenUsers(requester.getId(), receiver.getId()).stream()
                .anyMatch(f -> !f.getId().equals(id));
        if (pairTaken) {
            throw new IllegalStateException("Ya existe una solicitud de amistad o una amistad entre estos usuarios");
        }

        existente.setRequester(requester);
        existente.setReceiver(receiver);

        log.info("Termina proceso de actualización de la amistad con id = {}", id);
        return friendshipRepository.save(existente);
    }

    // Delete an existing friendship
    @Transactional
    public void delete(Long id) {
        log.info("Inicia proceso de eliminación de la amistad con id = {}", id);

        FriendshipModel existente = getById(id);
        friendshipRepository.delete(existente);

        log.info("Termina proceso de eliminación de la amistad con id = {}", id);
    }

    // Validate that the friendship data is correct
    private void validateFriendshipData(FriendshipModel friendship) {
        if (friendship.getRequester() == null || friendship.getRequester().getId() == null) {
            throw new IllegalArgumentException("Debe indicar el usuario que envía la solicitud (requester)");
        }

        if (friendship.getReceiver() == null || friendship.getReceiver().getId() == null) {
            throw new IllegalArgumentException("Debe indicar el usuario que recibe la solicitud (receiver)");
        }

        if (friendship.getRequester().getId().equals(friendship.getReceiver().getId())) {
            throw new IllegalArgumentException("Un usuario no puede enviarse solicitud a sí mismo");
        }
    }


    // Accept a pending friend request and notify the requester
    @Transactional
    public FriendshipModel acceptFriendship(Long friendshipId, Long userId) {
        log.info("Inicia proceso de aceptación de la amistad con id = {} por el usuario = {}", friendshipId, userId);

        FriendshipModel friendship = getById(friendshipId);
        validateReceiver(friendship, userId);
        validatePending(friendship);

        friendship.setStatus(FriendshipStatusEnum.ACCEPTED);
        FriendshipModel saved = friendshipRepository.save(friendship);

        eventPublisher.publishEvent(new FriendshipEvent(
                NotificationTypeEnum.FRIEND_REQUEST_ACCEPTED,
                saved.getRequester().getId(),
                saved.getReceiver().getName(),
                saved.getId()));

        log.info("Termina proceso de aceptación de la amistad con id = {}", friendshipId);
        return saved;
    }

    // Reject a pending friend request and notify the requester
    @Transactional
    public FriendshipModel rejectFriendship(Long friendshipId, Long userId) {
        log.info("Inicia proceso de rechazo de la amistad con id = {} por el usuario = {}", friendshipId, userId);

        FriendshipModel friendship = getById(friendshipId);
        validateReceiver(friendship, userId);
        validatePending(friendship);

        friendship.setStatus(FriendshipStatusEnum.REJECTED);
        FriendshipModel saved = friendshipRepository.save(friendship);

        eventPublisher.publishEvent(new FriendshipEvent(
                NotificationTypeEnum.FRIEND_REQUEST_REJECTED,
                saved.getRequester().getId(),
                saved.getReceiver().getName(),
                saved.getId()));

        log.info("Termina proceso de rechazo de la amistad con id = {}", friendshipId);
        return saved;
    }

    // Check that the user responding is the receiver of the friend request
    private void validateReceiver(FriendshipModel friendship, Long userId) {
        if (!friendship.getReceiver().getId().equals(userId)) {
            throw new ForbiddenException("Solo el usuario que recibe la solicitud puede responderla");
        }
    }

    // Check that the friend request is still pending before responding to it
    private void validatePending(FriendshipModel friendship) {
        if (friendship.getStatus() != FriendshipStatusEnum.PENDING) {
            throw new IllegalArgumentException("La solicitud de amistad ya fue respondida");
        }
    }

    // Get all accepted friends from a user (each friend appears once)
    @Transactional(readOnly = true)
    public List<UserModel> getFriendsByUser(Long userId) {
        log.info("Inicia proceso de consultar los amigos del usuario con id = {}", userId);

        userService.getById(userId);

        Map<Long, UserModel> friendsById = new LinkedHashMap<>();
        friendshipRepository
                .findByUserAndStatus(userId, FriendshipStatusEnum.ACCEPTED)
                .stream()
                .map(f -> f.getRequester().getId().equals(userId) ? f.getReceiver() : f.getRequester())
                .forEach(friend -> friendsById.putIfAbsent(friend.getId(), friend));

        List<UserModel> friends = List.copyOf(friendsById.values());

        log.info("Termina proceso de consultar los amigos del usuario con id = {}", userId);
        return friends;
    }
}
