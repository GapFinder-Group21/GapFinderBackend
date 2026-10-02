package com.backend.gapfinder.services;

import com.backend.gapfinder.exceptions.NotFoundException;
import com.backend.gapfinder.models.BuildingModel;
import com.backend.gapfinder.models.InterestModel;
import com.backend.gapfinder.models.UserModel;
import com.backend.gapfinder.repositories.UserRepository;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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

        user.setId(null);
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
        existente.setInterests(resolveInterests(user.getInterests()));
        existente.setCurrentBuilding(resolveCurrentBuilding(user.getCurrentBuilding()));

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