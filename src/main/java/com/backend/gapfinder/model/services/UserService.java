package com.backend.gapfinder.model.services;

import com.backend.gapfinder.model.exceptions.NotFoundException;
import com.backend.gapfinder.model.models.BuildingModel;
import com.backend.gapfinder.model.models.InterestModel;
import com.backend.gapfinder.model.models.UserModel;
import com.backend.gapfinder.model.repositories.UserRepository;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
public class UserService {

    private final UserRepository userRepository;
    private final InterestService interestService;
    private final BuildingService buildingService;

    public UserService(UserRepository userRepository, InterestService interestService,
                        BuildingService buildingService) {
        this.userRepository = userRepository;
        this.interestService = interestService;
        this.buildingService = buildingService;
    }

    // Get a user by its id
    @Transactional
    public UserModel getById(Long id) {
        log.info("Inicia proceso de consultar el usuario con id = {}", id);
        return userRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("El usuario con id " + id + " no existe"));
    }

    // Get all users
    @Transactional
    public List<UserModel> getAll() {
        log.info("Inicia proceso de consultar todos los usuarios");
        return userRepository.findAll();
    }

    // Create a new user
    @Transactional
    public UserModel create(UserModel user) {
        log.info("Inicia proceso de creación de un usuario con nombre = {}", user.getName());

        validateUserData(user);

        // An account needs a password; the public way to create one is /auth/register
        if (user.getPasswordHash() == null || user.getPasswordHash().isBlank()) {
            throw new IllegalArgumentException("Para crear una cuenta usa /auth/register");
        }

        if (user.getEmail() == null || user.getEmail().isBlank()) {
            throw new IllegalArgumentException("El correo del usuario es obligatorio");
        }

        // Emails are stored lowercase, as in the register flow
        String email = user.getEmail().toLowerCase().trim();
        if (userRepository.existsByEmail(email)) {
            throw new IllegalArgumentException("Ya existe una cuenta con ese correo");
        }

        user.setId(null);
        user.setEmail(email);
        user.setVerified(false);
        user.setCreatedAt(LocalDateTime.now());
        user.setInterests(resolveInterests(user.getInterests()));
        user.setCurrentBuilding(resolveCurrentBuilding(user.getCurrentBuilding()));

        log.info("Termina proceso de creación de un usuario con nombre = {}", user.getName());
        return userRepository.save(user);
    }

    // Update the data from an existing user
    @Transactional
    public UserModel update(Long id, UserModel user) {
        log.info("Inicia proceso de actualización del usuario con id = {}", id);

        UserModel existente = getById(id);
        validateUserData(user);

        existente.setName(user.getName());
        existente.setPhoneNumber(user.getPhoneNumber());
        existente.setCareer(user.getCareer());

        // Optional fields: only the ones sent in the request are changed
        if (user.getSemester() != null) {
            existente.setSemester(user.getSemester());
        }
        if (user.getPreferredEffort() != null) {
            existente.setPreferredEffort(user.getPreferredEffort());
        }
        if (user.getInterests() != null) {
            existente.setInterests(resolveInterests(user.getInterests()));
        }
        if (user.getCurrentBuilding() != null) {
            existente.setCurrentBuilding(resolveCurrentBuilding(user.getCurrentBuilding()));
        }

        log.info("Termina proceso de actualización del usuario con id = {}", id);
        return userRepository.save(existente);
    }

    // Delete an existing user
    @Transactional
    public void delete(Long id) {
        log.info("Inicia proceso de eliminación del usuario con id = {}", id);

        UserModel existente = getById(id);
        userRepository.delete(existente);

        log.info("Termina proceso de eliminación del usuario con id = {}", id);
    }

    // Validate that the user data is correct
    private void validateUserData(UserModel user) {
        if (user.getName() == null || user.getName().isBlank()) {
            throw new IllegalArgumentException("El nombre del usuario es obligatorio");
        }

        if (user.getCareer() == null || user.getCareer().isBlank()) {
            throw new IllegalArgumentException("La carrera del usuario es obligatoria");
        }
    }

    // Resolve the list of interests from the ids provided, if any
    private List<InterestModel> resolveInterests(List<InterestModel> interests) {
        if (interests == null || interests.isEmpty()) {
            return null;
        }

        return interests.stream()
                .map(interest -> interestService.getById(interest.getId()))
                .toList();
    }

    // Resolve the current building from the id provided, if any
    private BuildingModel resolveCurrentBuilding(BuildingModel currentBuilding) {
        if (currentBuilding == null || currentBuilding.getId() == null) {
            return null;
        }

        return buildingService.getById(currentBuilding.getId());
    }

    // Add an interest to an existing user
    @Transactional
    public UserModel addInterest(Long userId, Long interestId) {
        UserModel user = getById(userId);
        InterestModel interest = interestService.getById(interestId);

        if (user.getInterests() == null) {
            user.setInterests(new ArrayList<>());
        }

        boolean exists = user.getInterests().stream()
                .anyMatch(i -> i.getId().equals(interestId));
        if (!exists) {
            user.getInterests().add(interest);
        }

        return userRepository.save(user);
    }

    // Remove an interest from an existing user (idempotent: nothing happens if the user does not have it)
    @Transactional
    public void removeInterest(Long userId, Long interestId) {
        UserModel user = getById(userId);

        if (user.getInterests() == null) {
            return;
        }

        boolean removed = user.getInterests().removeIf(i -> interestId.equals(i.getId()));
        if (removed) {
            userRepository.save(user);
        }
    }

    // Search users by name
    @Transactional
    public List<UserModel> searchByName(String name) {
        log.info("Inicia proceso de búsqueda de usuarios con nombre = {}", name);

        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("El texto de búsqueda es obligatorio");
        }

        return userRepository.findByNameContainingIgnoreCase(name.trim());
    }
}