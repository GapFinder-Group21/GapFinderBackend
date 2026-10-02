package com.backend.gapfinder.dto.responses;

import lombok.Data;

@Data
public class GapCoverageResponseDTO {

    // Duration range of the gaps (e.g., <30, 30-60, 60-120)
    private String durationRange;

    // Number of gaps that fall in this range
    private Long gapCount;

    // Total free minutes of all gaps in this range
    private Double totalGapMinutes;

    // Total minutes of those gaps covered by matches
    private Double totalMatchedMinutes;

    // MatchedTime / GapDuration, as a percentage
    private Double coveragePercent;
}