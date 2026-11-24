package com.backend.gapfinder.view.dto;

// DTO with how often the finished open tables of a photo group ended with no participants
public record EmptyTablesByPhotoDTO(

    // Photo group: 'WITH_PHOTO' or 'WITHOUT_PHOTO'
    String group,

    // Finished open tables in the group (the ones that have not ended yet are not counted)
    long total,

    // Finished open tables in the group that nobody joined
    long empty,

    // Percentage of the finished open tables of the group that ended empty, between 0 and 100 (0 if the group has none)
    double emptyPercentage,

    // True for the group with the highest empty percentage (only one is marked, none on a tie)
    boolean highestEmpty
) {}
