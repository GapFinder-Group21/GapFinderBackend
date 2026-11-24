package com.backend.gapfinder.model.repositories;

import com.backend.gapfinder.model.models.ChatDeliveryReportModel;
import com.backend.gapfinder.model.repositories.projections.ChatDeliveryRangeProjection;
import com.backend.gapfinder.model.repositories.projections.ChatDeliveryRouteProjection;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

// Repository of the chat delivery reports sent by the app (BQ 17 INDIVIDUAL TYPE 1)
@Repository
public interface ChatDeliveryReportRepository extends JpaRepository<ChatDeliveryReportModel, Long> {

    // Whether this user already reported this message
    boolean existsByUserIdAndClientId(Long userId, String clientId);

    // BQ 17 INDIVIDUAL TYPE 1
    // :since filters by when Send was tapped (tapped_at), so a message queued for hours counts in the period
    // when it was written

    // Messages, median and 90th percentile of the delivery time per route, and for the queued ones the median time
    // after the connection came back (percentile_cont skips the nulls, so it is null for DIRECT)
    // Routes without reports do not appear in the result (the service adds them with 0)
    @Query(value = """
        SELECT r.route AS "route",
            CAST(COUNT(*) AS bigint) AS "messages",
            CAST(percentile_cont(0.5) WITHIN GROUP (ORDER BY r.elapsed_ms) AS double precision) AS "medianMs",
            CAST(percentile_cont(0.9) WITHIN GROUP (ORDER BY r.elapsed_ms) AS double precision) AS "p90Ms",
            CAST(percentile_cont(0.5) WITHIN GROUP (ORDER BY r.after_reconnect_ms) AS double precision)
                AS "medianAfterReconnectMs"
        FROM chat_delivery_reports r
        WHERE r.tapped_at >= :since
        GROUP BY r.route
        """, nativeQuery = true)
    List<ChatDeliveryRouteProjection> summarizeByRoute(@Param("since") LocalDateTime since);

    // Messages of each route per range of delivery time. A range includes its lower limit:
    // a message of exactly 3 s is in '3-10s'
    // Ranges without messages do not appear in the result (the service adds them with 0)
    @Query(value = """
        SELECT CASE
                WHEN r.elapsed_ms < 1000 THEN '<1s'
                WHEN r.elapsed_ms < 3000 THEN '1-3s'
                WHEN r.elapsed_ms < 10000 THEN '3-10s'
                WHEN r.elapsed_ms < 60000 THEN '10-60s'
                WHEN r.elapsed_ms < 600000 THEN '1-10min'
                ELSE '10min+'
            END AS "delayRange",
            CAST(SUM(CASE WHEN r.route = 'DIRECT' THEN 1 ELSE 0 END) AS bigint) AS "direct",
            CAST(SUM(CASE WHEN r.route = 'QUEUED' THEN 1 ELSE 0 END) AS bigint) AS "queued"
        FROM chat_delivery_reports r
        WHERE r.tapped_at >= :since
        GROUP BY 1
        """, nativeQuery = true)
    List<ChatDeliveryRangeProjection> countByDelayRange(@Param("since") LocalDateTime since);
}
