package com.backend.gapfinder.model.repositories;

import com.backend.gapfinder.model.models.ActivityModel;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ActivityRepository extends JpaRepository<ActivityModel, Long> {
}