package com.backend.gapfinder.repositories;

import com.backend.gapfinder.enums.DayOfWeekEnum;
import com.backend.gapfinder.models.ClassBlockModel;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalTime;
import java.util.List;

public interface ClassBlockRepository extends JpaRepository<ClassBlockModel, Long> {

    List<ClassBlockModel> findByUserId(Long userId);

    // Ids de los usuarios que tienen al menos una clase en su horario
    @Query("SELECT DISTINCT c.user.id FROM ClassBlockModel c")
    List<Long> findDistinctUserIds();

    boolean existsByUserIdAndSubjectAndDayOfWeekAndStartTimeAndEndTime(
            Long userId,
            String subject,
            DayOfWeekEnum dayOfWeek,
            LocalTime startTime,
            LocalTime endTime
    );
}