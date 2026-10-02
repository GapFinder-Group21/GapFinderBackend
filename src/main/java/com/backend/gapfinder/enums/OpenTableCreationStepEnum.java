package com.backend.gapfinder.enums;

// Steps of the open table creation wizard where the user can abandon the flow
public enum OpenTableCreationStepEnum {

    // User was selecting the activity for the open table
    ACTIVITY,

    // User was writing the title/description of the open table
    DESCRIPTION,

    // User was setting the max number of participants
    PARTICIPANTS,

    // User was selecting the building/location of the open table
    LOCATION
}