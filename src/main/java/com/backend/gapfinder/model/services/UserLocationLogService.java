package com.backend.gapfinder.services;

import com.backend.gapfinder.exceptions.NotFoundException;
import com.backend.gapfinder.models.BuildingModel;
import com.backend.gapfinder.models.UserLocationLogModel;
import com.backend.gapfinder.models.UserModel;
import com.backend.gapfinder.repositories.UserLocationLogRepository;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
public class UserLocationLogService {

    private final UserLocationLogRepository userLocationLogRepository;
    private final UserService userService;
    private final BuildingService buildingService;

    public UserLocationLogService(UserLocationLogRepository userLocationLogRepository, UserService userService,
                                   BuildingService buildingService) {
        this.userLocationLogRepository = userLocationLogRepository;
        this.userService = userService;
        this.buildingService = buildingService;
    }

    // Get a location log by its id
    @Transactional
    public UserLocationLogModel getById(Long id) {
        log.info("Inicia proceso de consultar el registro de ubicación con id = {}", id);
        return userLocationLogRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("El registro de ubicación con id " + id + " no existe"));
    }

    // Get all location logs
    @Transactional
    public List<UserLocationLogModel> getAll() {
        log.info("Inicia proceso de consultar todos los registros de ubicación");
        return userLocationLogRepository.findAll();
    }

    // Create a new location log
    @Transactional
    public UserLocationLogModel create(UserLocationLogModel locationLog) {
        log.info("Inicia proceso de creación de un registro de ubicación");

        validateLocationLogData(locationLog);

        UserModel user = userService.getById(locationLog.getUser().getId());
        BuildingModel building = buildingService.getById(locationLog.getBuilding().getId());

        locationLog.setId(null);
        locationLog.setUser(user);
        locationLog.setBuilding(building);
        locationLog.setTimestamp(LocalDateTime.now());

        log.info("Termina proceso de creación de un registro de ubicación");
        return userLocationLogRepository.save(locationLog);
    }

    // Update the data from an existing location log
    @Transactional
    public UserLocationLogModel update(Long id, UserLocationLogModel locationLog) {
        log.info("Inicia proceso de actualización del registro de ubicación con id = {}", id);

        UserLocationLogModel existente = getById(id);
        validateLocationLogData(locationLog);

        UserModel user = userService.getById(locationLog.getUser().getId());
        BuildingModel building = buildingService.getById(locationLog.getBuilding().getId());

        existente.setUser(user);
        existente.setBuilding(building);

        log.info("Termina proceso de actualización del registro de ubicación con id = {}", id);
        return userLocationLogRepository.save(existente);
    }

    // Delete an existing location log
    @Transactional
    public void delete(Long id) {
        log.info("Inicia proceso de eliminación del registro de ubicación con id = {}", id);

        UserLocationLogModel existente = getById(id);
        userLocationLogRepository.delete(existente);

        log.info("Termina proceso de eliminación del registro de ubicación con id = {}", id);
    }

    // Validate that the location log data is correct
    private void validateLocationLogData(UserLocationLogModel locationLog) {
        if (locationLog.getUser() == null || locationLog.getUser().getId() == null) {
            throw new IllegalArgumentException("Debe indicar el usuario (user)");
        }

        if (locationLog.getBuilding() == null || locationLog.getBuilding().getId() == null) {
            throw new IllegalArgumentException("Debe indicar el edificio (building)");
        }
    }
}