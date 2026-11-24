package com.backend.gapfinder.model.repositories.projections;

// Projection with how many chat messages of each route took a range of time to reach the server (BQ 17)
public interface ChatDeliveryRangeProjection {

    // '<1s', '1-3s', '3-10s', '10-60s', '1-10min' or '10min+'
    String getDelayRange();

    // Messages sent at once in the range
    Long getDirect();

    // Messages sent by the offline queue in the range
    Long getQueued();
}
