package com.backend.gapfinder.services;

import com.backend.gapfinder.enums.FriendshipStatusEnum;
import com.backend.gapfinder.exceptions.NotFoundException;
import com.backend.gapfinder.models.FriendshipModel;
import com.backend.gapfinder.models.UserModel;
import com.backend.gapfinder.repositories.FriendshipRepository;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
public class FriendshipService {

    private final FriendshipRepository friendshipRepository;
    private final UserService userService;

    public FriendshipService(FriendshipRepository friendshipRepository, UserService userService) {
        this.friendshipRepository = friendshipRepository;
        this.userService = userService;
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

    // Create a new friendship
    @Transactional
    public FriendshipModel create(FriendshipModel friendship) {
        log.info("Inicia proceso de creación de una amistad");

        validateFriendshipData(friendship);

        UserModel requester = userService.getById(friendship.getRequester().getId());
        UserModel receiver = userService.getById(friendship.getReceiver().getId());

        friendship.setId(null);
        friendship.setRequester(requester);
        friendship.setReceiver(receiver);
        if (friendship.getStatus() == null) {
            friendship.setStatus(FriendshipStatusEnum.PENDING);
        }
        friendship.setCreatedAt(LocalDateTime.now());

        FriendshipModel saved = friendshipRepository.save(friendship);

        log.info("Termina proceso de creación de una amistad");
        return saved;
    }

    // Update the data from an existing friendship
    @Transactional
    public FriendshipModel update(Long id, FriendshipModel friendship) {
        log.info("Inicia proceso de actualización de la amistad con id = {}", id);

        FriendshipModel existente = getById(id);
        validateFriendshipData(friendship);

        UserModel requester = userService.getById(friendship.getRequester().getId());
        UserModel receiver = userService.getById(friendship.getReceiver().getId());

        existente.setRequester(requester);
        existente.setReceiver(receiver);
        existente.setStatus(friendship.getStatus());

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


    // Accept a pending friend request
    @Transactional
    public FriendshipModel acceptFriendship(Long friendshipId, Long userId) {
        log.info("Inicia proceso de aceptación de la amistad con id = {} por el usuario = {}", friendshipId, userId);

        FriendshipModel friendship = getById(friendshipId);
        validateReceiver(friendship, userId);
        validatePending(friendship);

        friendship.setStatus(FriendshipStatusEnum.ACCEPTED);
        FriendshipModel saved = friendshipRepository.save(friendship);

        log.info("Termina proceso de aceptación de la amistad con id = {}", friendshipId);
        return saved;
    }

    // Reject a pending friend request
    @Transactional
    public FriendshipModel rejectFriendship(Long friendshipId, Long userId) {
        log.info("Inicia proceso de rechazo de la amistad con id = {} por el usuario = {}", friendshipId, userId);

        FriendshipModel friendship = getById(friendshipId);
        validateReceiver(friendship, userId);
        validatePending(friendship);

        friendship.setStatus(FriendshipStatusEnum.REJECTED);
        FriendshipModel saved = friendshipRepository.save(friendship);

        log.info("Termina proceso de rechazo de la amistad con id = {}", friendshipId);
        return saved;
    }

    // Check that the user responding is the receiver of the friend request
    private void validateReceiver(FriendshipModel friendship, Long userId) {
        if (!friendship.getReceiver().getId().equals(userId)) {
            throw new IllegalArgumentException("Solo el usuario que recibe la solicitud puede responderla");
        }
    }

    // Check that the friend request is still pending before responding to it
    private void validatePending(FriendshipModel friendship) {
        if (friendship.getStatus() != FriendshipStatusEnum.PENDING) {
            throw new IllegalArgumentException("La solicitud de amistad ya fue respondida");
        }
    }

    // Get all accepted friends from a user
    @Transactional(readOnly = true)
    public List<UserModel> getFriendsByUser(Long userId) {
        log.info("Inicia proceso de consultar los amigos del usuario con id = {}", userId);

        userService.getById(userId);

        List<UserModel> friends = friendshipRepository
                .findByUserAndStatus(userId, FriendshipStatusEnum.ACCEPTED)
                .stream()
                .map(f -> f.getRequester().getId().equals(userId) ? f.getReceiver() : f.getRequester())
                .toList();

        log.info("Termina proceso de consultar los amigos del usuario con id = {}", userId);
        return friends;
    }
}