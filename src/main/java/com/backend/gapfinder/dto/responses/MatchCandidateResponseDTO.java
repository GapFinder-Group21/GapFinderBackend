package com.backend.gapfinder.dto.responses;

import com.backend.gapfinder.dto.GapCompleteDTO;

import lombok.Data;

@Data
public class MatchCandidateResponseDTO {

    // Target gap owned by the requesting student
    private GapCompleteDTO proposerGap;

    // Candidate gap belonging to the potential match
    private GapCompleteDTO acceptorGap;

    // Total compatibility score calculated by active strategies
    private Double score;
}