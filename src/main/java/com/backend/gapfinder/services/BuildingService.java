package com.backend.gapfinder.services;

import com.backend.gapfinder.exceptions.NotFoundException;
import com.backend.gapfinder.models.BuildingModel;
import com.backend.gapfinder.repositories.BuildingRepository;

import lombok.extern.slf4j.Slf4j;

import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Slf4j
@Service
public class BuildingService {

    private final BuildingRepository buildingRepository;
    private final GeometryFactory geometryFactory;

    // Dependency injection constructor
    public BuildingService(BuildingRepository buildingRepository, GeometryFactory geometryFactory) {
        this.buildingRepository = buildingRepository;
        this.geometryFactory = geometryFactory;
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

    // Determines which building the user is in, based on GPS coordinates
    @Transactional(readOnly = true)
    public Optional<BuildingModel> findBuildingContainingUser(double latitude, double longitude) {
        log.info("Inicia proceso de resolver edificio a partir de coordenadas ({}, {})", latitude, longitude);

        // JTS Coordinate order is (X, Y) -> (longitude, latitude)
        Point userLocation = geometryFactory.createPoint(new Coordinate(longitude, latitude));

        Optional<BuildingModel> building = buildingRepository.findBuildingAtUserLocation(userLocation);

        log.info("Termina proceso de resolver edificio a partir de coordenadas");
        return building;
    }

    // Validate that the building data is correct
    private void validateBuildingData(BuildingModel building) {
        if (building.getName() == null || building.getName().isBlank()) {
            throw new IllegalArgumentException("El nombre del edificio es obligatorio");
        }

        if (building.getLocation() == null) {
            throw new IllegalArgumentException("La ubicación del edificio es obligatoria");
        }

        if (building.getRadiusMeters() <= 0) {
            throw new IllegalArgumentException("El radio debe ser un número positivo");
        }
    }
}