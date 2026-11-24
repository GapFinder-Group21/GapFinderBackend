package com.backend.gapfinder.model.models;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Data;
import lombok.EqualsAndHashCode;

// Rating that a student gives to a finished match (stars 1-5 and an optional comment)
@Entity
@Table(
    name = "match_ratings",
    // A student can rate each match only once
    uniqueConstraints = @UniqueConstraint(columnNames = {"match_id", "rater_id"})
)
@Data
@EqualsAndHashCode(callSuper = true)
public class MatchRatingModel extends BaseModel {

    public static final int MIN_STARS = 1;
    public static final int MAX_STARS = 5;
    public static final int MAX_COMMENT_LENGTH = 300;

    // Match being rated
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "match_id", nullable = false)
    private MatchModel match;

    // Student who gives the rating
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "rater_id", nullable = false)
    private UserModel rater;

    // The other student of the match, who receives the rating
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "rated_id", nullable = false)
    private UserModel rated;

    // From 1 to 5
    @Column(nullable = false)
    private Integer stars;

    // Optional comment
    @Column(length = MAX_COMMENT_LENGTH)
    private String comment;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}
