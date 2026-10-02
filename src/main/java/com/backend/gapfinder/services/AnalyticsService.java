package com.backend.gapfinder.services;

import com.backend.gapfinder.dto.responses.GapCoverageResponseDTO;
import com.backend.gapfinder.dto.OpenTableAbandonmentStatsBasicDTO;
import com.backend.gapfinder.exceptions.NotFoundException;
import com.backend.gapfinder.repositories.GapRepository;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;

@Slf4j
@Service
public class AnalyticsService {

    private final OpenTableAbandonmentService abandonmentService;
    private final GapRepository gapRepository;

    public AnalyticsService(OpenTableAbandonmentService abandonmentService, GapRepository gapRepository) {
        this.abandonmentService = abandonmentService;
        this.gapRepository = gapRepository;
    }

    // ==================== BQ 3 TYPE 2 GRUPAL ====================
    // At which step do students most often leave the Open Table creating process?

    // Retrieves the creation step with the highest abandonment rate since the given date
    @Transactional(readOnly = true)
    public OpenTableAbandonmentStatsBasicDTO getMostAbandonedStep(LocalDateTime since) {
        log.info("Inicia proceso de consultar el paso más abandonado desde {}", since);

        OpenTableAbandonmentStatsBasicDTO result = abandonmentService.getMostAbandonedStep(since)
                .orElseThrow(() -> new NotFoundException("No hay abandonos registrados desde la fecha indicada"));

        log.info("Termina proceso de consultar el paso más abandonado desde {}", since);
        return result;
    }

    // ================== END BQ 3 TYPE 2 GRUPAL ==================

    // ==================== BQ 5 ====================
    // Miguel Cabrera: Which gap durations have the lowest match coverage?

    // Retrieves match coverage metrics grouped by gap duration buckets sorted by lowest coverage
    @Transactional(readOnly = true)
    public List<GapCoverageResponseDTO> getMatchCoverageByGapDuration() {
        log.info("Inicia proceso de consultar la cobertura de matches por duración de gap");

        List<GapCoverageResponseDTO> result = gapRepository.findCoverageByDurationBucket().stream()
                .map(row -> {
                    double totalGapMinutes = ((Number) row[2]).doubleValue();
                    double totalMatchedMinutes = ((Number) row[3]).doubleValue();

                    GapCoverageResponseDTO dto = new GapCoverageResponseDTO();
                    dto.setDurationRange((String) row[0]);
                    dto.setGapCount(((Number) row[1]).longValue());
                    dto.setTotalGapMinutes(totalGapMinutes);
                    dto.setTotalMatchedMinutes(totalMatchedMinutes);
                    dto.setCoveragePercent(
                            calculateCoveragePercent(totalMatchedMinutes, totalGapMinutes)
                    );

                    return dto;
                })
                .sorted(Comparator.comparingDouble(GapCoverageResponseDTO::getCoveragePercent))
                .toList();

        log.info("Termina proceso de consultar la cobertura de matches por duración de gap");
        return result;
    }

    // Calculates the coverage percentage based on matched minutes and total gap minutes
    private double calculateCoveragePercent(double matchedMinutes, double gapMinutes) {
        if (gapMinutes <= 0) {
            return 0.0;
        }

        return matchedMinutes / gapMinutes * 100;
    }

    // ================== END BQ 5 ==================
}