package com.backend.gapfinder.config;

import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.PrecisionModel;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

// Configuration for spatial (JTS) beans
@Configuration
public class GeometryConfig {

    // Factory used to create Point geometries, SRID 4326 must match the building location column
    @Bean
    public GeometryFactory geometryFactory() {
        return new GeometryFactory(new PrecisionModel(), 4326);
    }
}