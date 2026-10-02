package com.backend.gapfinder.adapters;

// Adapter pattern - Client Interface: protocol the schedule import (client) uses to get a user's classes,
// expressed only with GapFinder's own model, so the client never depends on an external calendar library
public interface ScheduleSource {

    // Returns the user's class blocks already converted to GapFinder's model
    AdaptedSchedule fetchClassBlocks(Long userId);
}
