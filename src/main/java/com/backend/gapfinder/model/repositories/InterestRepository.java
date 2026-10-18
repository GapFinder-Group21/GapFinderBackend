package com.backend.gapfinder.model.repositories;

import com.backend.gapfinder.model.models.InterestModel;
import org.springframework.data.jpa.repository.JpaRepository;

public interface InterestRepository extends JpaRepository<InterestModel, Long> {

    
    boolean existsByName(String name);
}