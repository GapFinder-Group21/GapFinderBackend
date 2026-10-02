package com.backend.gapfinder.repositories;

import com.backend.gapfinder.models.UserLocationLogModel;

import org.springframework.data.jpa.repository.JpaRepository;

public interface UserLocationLogRepository extends JpaRepository<UserLocationLogModel, Long> {

}