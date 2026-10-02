package com.backend.gapfinder.services;

import com.backend.gapfinder.exceptions.NotFoundException;
import com.backend.gapfinder.models.BuildingModel;
import com.backend.gapfinder.repositories.BuildingRepository;

import lombok.extern.slf4j.Slf4j;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
public class BuildingService {

    private final BuildingRepository buildingRepository;

    // Dependency injection constructor
    public BuildingService(BuildingRepository buildingRepository) {
        this.buildingRepository = buildingRepository;
    }

    // Get a building by its id
    @Transactional
    public BuildingModel getById(Long id) {
        log.info("Inicia proceso de consultar el edificio con id = {}", id);
        return buildingRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("El edificio con id " + id + " no existe"));
    }

    // Get all buildings
    @Transactional
    public List<BuildingModel> getAll() {
        log.info("Inicia proceso de consultar todos los edificios");
        return buildingRepository.findAll();
    }

    // Create a new building
    @Transactional
    public BuildingModel create(BuildingModel building) {
        log.info("Inicia proceso de creación de un edificio con nombre = {}", building.getName());

        validateBuildingData(building);

        boolean yaExiste = buildingRepository.existsByName(building.getName());
        if (yaExiste) {
            throw new IllegalArgumentException("Ya existe un edificio con el nombre: " + building.getName());
        }

        building.setId(null);

        log.info("Termina proceso de creación de un edificio con nombre = {}", building.getName());
        return buildingRepository.save(building);
    }

    // Update the data from an existing building
    @Transactional
    public BuildingModel update(Long id, BuildingModel building) {
        log.info("Inicia proceso de actualización del edificio con id = {}", id);

        BuildingModel existente = getById(id);
        validateBuildingData(building);

        existente.setName(building.getName());

        log.info("Termina proceso de actualización del edificio con id = {}", id);
        return buildingRepository.save(existente);
    }

    // Delete an existing building
    @Transactional
    public void delete(Long id) {
        log.info("Inicia proceso de eliminación del edificio con id = {}", id);

        BuildingModel existente = getById(id);
        buildingRepository.delete(existente);

        log.info("Termina proceso de eliminación del edificio con id = {}", id);
    }

    // Validate that the building data is correct
    private void validateBuildingData(BuildingModel building) {
        if (building.getName() == null || building.getName().isBlank()) {
            throw new IllegalArgumentException("El nombre del edificio es obligatorio");
        }
    }
}