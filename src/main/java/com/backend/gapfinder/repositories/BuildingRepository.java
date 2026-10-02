package com.backend.gapfinder.repositories;

import com.backend.gapfinder.models.BuildingModel;

import org.locationtech.jts.geom.Point;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface BuildingRepository extends JpaRepository<BuildingModel, Long> {

    // Checks if a campus building exists with the specified name
    boolean existsByName(String name);

    // Finds the closest building whose coverage radius contains the given point
    @Query(value = """
        SELECT b.*
        FROM buildings b
        WHERE ST_DWithin(CAST(b.location AS geography), CAST(:userLocation AS geography), b.radius_meters)
        ORDER BY ST_Distance(CAST(b.location AS geography), CAST(:userLocation AS geography))
        LIMIT 1
        """, nativeQuery = true)
    Optional<BuildingModel> findBuildingAtUserLocation(@Param("userLocation") Point userLocation);

}