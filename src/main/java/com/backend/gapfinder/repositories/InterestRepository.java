package com.backend.gapfinder.repositories;

import com.backend.gapfinder.models.InterestModel;
import org.springframework.data.jpa.repository.JpaRepository;

public interface InterestRepository extends JpaRepository<InterestModel, Long> {

    
    boolean existsByName(String name);
}