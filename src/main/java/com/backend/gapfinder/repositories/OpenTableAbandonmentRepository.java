package com.backend.gapfinder.repositories;

import com.backend.gapfinder.models.OpenTableAbandonmentModel;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

// Repository for abandoned open table creation flows
@Repository
public interface OpenTableAbandonmentRepository extends JpaRepository<OpenTableAbandonmentModel, Long> {

    // Each row is [step, number of abandonments at that step] since the given date
    @Query("select a.step, count(a) from OpenTableAbandonmentModel a "
         + "where a.abandonedAt >= :since group by a.step")
    List<Object[]> countGroupedByStep(@Param("since") LocalDateTime since);
}