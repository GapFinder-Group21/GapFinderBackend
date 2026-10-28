package com.backend.gapfinder.model.repositories.projections;

// Projection with the student demand vs open table supply per interest
public interface InterestDemandGapProjection {

    // Identifier of the interest
    Long getInterestId();

    // Name of the interest
    String getInterestName();

    // Number of students who have this interest
    Long getStudentCount();

    // Number of open tables created for activities of this interest
    Long getOpenTableCount();
}
