package com.backend.gapfinder.repositories;

import com.backend.gapfinder.models.BuildingModel;

import org.springframework.data.jpa.repository.JpaRepository;


public interface BuildingRepository extends JpaRepository<BuildingModel, Long> {

    // Checks if a campus building exists with the specified name
    boolean existsByName(String name);

}