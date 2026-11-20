package com.backend.gapfinder.model.repositories;

import com.backend.gapfinder.model.models.StudyPeriodModel;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface StudyPeriodRepository extends JpaRepository<StudyPeriodModel, Long> {

    // Periods of a user that have not ended yet (active now or in the future), the earliest first
    List<StudyPeriodModel> findByUserIdAndEndTimeAfterOrderByStartTimeAsc(Long userId, LocalDateTime now);

    // Whether this user already sent a period with this client id
    boolean existsByUserIdAndClientId(Long userId, String clientId);

    // Ids (from the given ones) of the users that are studying at the given moment: one query for all of them
    @Query("SELECT DISTINCT sp.user.id FROM StudyPeriodModel sp " +
           "WHERE sp.user.id IN :userIds AND sp.startTime <= :now AND sp.endTime > :now")
    List<Long> findUserIdsStudyingAt(@Param("userIds") Collection<Long> userIds,
                                     @Param("now") LocalDateTime now);
}
