package com.backend.gapfinder.model.repositories;

import com.backend.gapfinder.model.enums.DayOfWeekEnum;
import com.backend.gapfinder.model.models.ClassBlockModel;

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

    // Same check as above, leaving out the class being edited
    boolean existsByUserIdAndSubjectAndDayOfWeekAndStartTimeAndEndTimeAndIdNot(
            Long userId,
            String subject,
            DayOfWeekEnum dayOfWeek,
            LocalTime startTime,
            LocalTime endTime,
            Long id
    );
}