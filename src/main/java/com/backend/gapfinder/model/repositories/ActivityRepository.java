package com.backend.gapfinder.repositories;

import com.backend.gapfinder.models.ActivityModel;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ActivityRepository extends JpaRepository<ActivityModel, Long> {
}