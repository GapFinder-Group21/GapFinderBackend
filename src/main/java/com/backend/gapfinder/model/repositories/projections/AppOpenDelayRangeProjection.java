package com.backend.gapfinder.model.repositories.projections;

// Projection with the number of gaps whose first app opening falls in a range of minutes
public interface AppOpenDelayRangeProjection {

    // Range of minutes after the gap start: '0-5', '5-15', '15-30', '30+' or 'No abrió' (the app was not opened)
    String getDelayRange();

    // Number of gaps in the range
    Long getGaps();
}
