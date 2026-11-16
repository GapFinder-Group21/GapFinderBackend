package com.backend.gapfinder.model.repositories.projections;

// Projection with the finished open tables of a photo group and how many of them nobody joined
public interface EmptyTablesByPhotoProjection {

    // Photo group: 'WITH_PHOTO' or 'WITHOUT_PHOTO'
    String getPhotoGroup();

    // Finished open tables in the group
    Long getTotal();

    // Finished open tables in the group without any participant
    Long getEmpty();
}
