package com.backend.gapfinder.models;

import java.time.LocalDateTime;

import com.backend.gapfinder.enums.MatchStatusEnum;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Data;

@Entity
@Table(name = "matches")
@Data

public class MatchModel extends BaseModel {

    // Free time slot of the user who initiated the match
    @ManyToOne
    @JoinColumn(name = "proposer_gap_id", nullable = false)
    private GapModel proposerGap;

    // Free time slot of the user who accepted the match
    @ManyToOne
    @JoinColumn(name = "acceptor_gap_id", nullable = false)
    private GapModel acceptorGap;

    // Start time of the agreed meeting
    @Column(name = "start_time", nullable = false)
    private LocalDateTime startTime;

    // End time of the agreed meeting
    @Column(name = "end_time", nullable = false)
    private LocalDateTime endTime;

    // Compatibility score calculated by the scoring algorithm
    @Column(nullable = false)
    private Double score;

    // Current state of the match (PENDING, ACCEPTED, REJECTED, COMPLETED)
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private MatchStatusEnum status;
}