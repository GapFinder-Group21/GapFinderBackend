package com.backend.gapfinder.repositories;

import com.backend.gapfinder.enums.OpenTableStatusEnum;
import com.backend.gapfinder.models.OpenTableModel;

import java.time.LocalDateTime;
import java.util.List;


import org.springframework.data.jpa.repository.JpaRepository;


public interface OpenTableRepository extends JpaRepository<OpenTableModel, Long> {


    // Find the open tables with any of the given statuses whose end time is before the given time
    List<OpenTableModel> findByStatusInAndEndTimeBefore(List<OpenTableStatusEnum> statuses, LocalDateTime time);
}
