package com.backend.gapfinder.dto.responses;

import lombok.Data;

@Data
public class ConnectionMethodResponseDTO {

    // Number of matches with status COMPLETED
    private Long completedMatches;

    // Total number of matches
    private Long totalMatches;

    // CompletedMatches / TotalMatches, as a percentage
    private Double matchCompletionPercent;

    // Number of open tables with status COMPLETED
    private Long completedOpenTables;

    // Total number of open tables
    private Long totalOpenTables;

    // CompletedOpenTables / TotalOpenTables, as a percentage
    private Double openTableCompletionPercent;

    // Connection method with the highest completion percentage (MATCHES, OPEN_TABLES or TIE)
    private String winner;
}