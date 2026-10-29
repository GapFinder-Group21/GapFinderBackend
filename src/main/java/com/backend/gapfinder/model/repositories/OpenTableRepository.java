package com.backend.gapfinder.model.repositories;

import com.backend.gapfinder.model.enums.OpenTableStatusEnum;
import com.backend.gapfinder.model.models.OpenTableModel;
import com.backend.gapfinder.model.repositories.projections.PreferredActivityRankingProjection;
import com.backend.gapfinder.model.repositories.projections.PreferredActivityTotalsProjection;
import com.backend.gapfinder.model.repositories.projections.StudentActivityUsageProjection;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import jakarta.persistence.LockModeType;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface OpenTableRepository extends JpaRepository<OpenTableModel, Long> {

    // Finds OPEN tables in a building that overlap with a gap of the user that has not ended yet
    // Tables created by the user or already joined by the user are not recommended
    // SMART FEATURE
    @Query("""
        SELECT DISTINCT o
        FROM OpenTableModel o, GapModel g
        WHERE o.building.id = :buildingId
          AND o.status = :status
          AND o.endTime > :now
          AND o.creator.id <> :userId
          AND NOT EXISTS (SELECT p FROM OpenTableParticipantModel p
                          WHERE p.openTable = o AND p.user.id = :userId)
          AND g.user.id = :userId
          AND g.endTime > :now
          AND o.startTime < g.endTime
          AND o.endTime > g.startTime
        ORDER BY o.startTime
        """)
    List<OpenTableModel> findMatchingOpenTables(@Param("userId") Long userId,
                                                @Param("buildingId") Long buildingId,
                                                @Param("status") OpenTableStatusEnum status,
                                                @Param("now") LocalDateTime now);



    // Gets an open table and locks its row until the transaction ends,
    // so two users joining at the same time cannot both take the last spot
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT o FROM OpenTableModel o WHERE o.id = :id")
    Optional<OpenTableModel> findByIdForUpdate(@Param("id") Long id);

    // Find the open tables with any of the given statuses whose end time is before the given time
    List<OpenTableModel> findByStatusInAndEndTimeBefore(List<OpenTableStatusEnum> statuses, LocalDateTime time);

    // Counts the total number of open tables created on or after the specified timestamp.
    long countByCreatedAtGreaterThanEqual(LocalDateTime since);

    // Count the open tables with a given status (BQ 7)
    long countByStatus(OpenTableStatusEnum status);

    // BQ 6 TYPE 2
    // Times a student chose each activity when creating open tables since :since, most chosen first
    // Tie: the activity used most recently comes first. Open tables without activity are ignored
    @Query(value = """
        SELECT a.id AS "activityId",
            a.name AS "activityName",
            CAST(COUNT(*) AS bigint) AS "timesChosen"
        FROM open_tables o
        JOIN activities a ON a.id = o.activity_id
        WHERE o.creator_id = :userId
          AND o.created_at >= :since
        GROUP BY a.id, a.name
        ORDER BY COUNT(*) DESC, MAX(o.created_at) DESC, a.id
        """, nativeQuery = true)
    List<StudentActivityUsageProjection> findActivityUsageByStudent(@Param("userId") Long userId,
                                                                    @Param("since") LocalDateTime since);

    // BQ 6 TYPE 2
    // Open tables created with each activity since :since and the different students who created them, most preferred first
    // Tie: the activity used most recently comes first. Open tables without activity are ignored
    @Query(value = """
        SELECT a.id AS "activityId",
            a.name AS "activityName",
            CAST(COUNT(*) AS bigint) AS "tables",
            CAST(COUNT(DISTINCT o.creator_id) AS bigint) AS "students"
        FROM open_tables o
        JOIN activities a ON a.id = o.activity_id
        WHERE o.created_at >= :since
        GROUP BY a.id, a.name
        ORDER BY COUNT(*) DESC, MAX(o.created_at) DESC, a.id
        """, nativeQuery = true)
    List<PreferredActivityRankingProjection> countOpenTablesByActivity(@Param("since") LocalDateTime since);

    // BQ 6 TYPE 2
    // Totals since :since: open tables with activity, students, and the students who always repeat the same activity
    // A student can only repeat an activity with two or more open tables, so the percentage uses those students
    // Without open tables it returns one row with zeros
    @Query(value = """
        SELECT CAST(COALESCE(SUM(per_student.tables_created), 0) AS bigint) AS "tables",
            CAST(COUNT(*) AS bigint) AS "students",
            CAST(COUNT(*) FILTER (WHERE per_student.tables_created >= 2) AS bigint) AS "studentsWithSeveralTables",
            CAST(COUNT(*) FILTER (WHERE per_student.tables_created >= 2 AND per_student.activities = 1) AS bigint)
                AS "studentsRepeatingSameActivity"
        FROM (
            SELECT o.creator_id,
                COUNT(*) AS tables_created,
                COUNT(DISTINCT o.activity_id) AS activities
            FROM open_tables o
            WHERE o.activity_id IS NOT NULL
              AND o.created_at >= :since
            GROUP BY o.creator_id
        ) per_student
        """, nativeQuery = true)
    PreferredActivityTotalsProjection findPreferredActivityTotals(@Param("since") LocalDateTime since);

}
