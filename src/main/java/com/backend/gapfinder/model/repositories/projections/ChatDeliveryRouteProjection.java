package com.backend.gapfinder.model.repositories.projections;

// Projection with the delivery times of the chat messages of one route (BQ 17)
public interface ChatDeliveryRouteProjection {

    // 'DIRECT' or 'QUEUED'
    String getRoute();

    // Reported messages of the route
    Long getMessages();

    // Median milliseconds from the tap on Send until the server saved the message
    Double getMedianMs();

    // 90th percentile of the same time: 9 of every 10 messages took this long or less
    Double getP90Ms();

    // Only QUEUED: median milliseconds from the moment the connection came back (null for DIRECT)
    Double getMedianAfterReconnectMs();
}
