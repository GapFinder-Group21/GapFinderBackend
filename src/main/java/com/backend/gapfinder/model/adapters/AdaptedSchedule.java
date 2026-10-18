package com.backend.gapfinder.model.adapters;

import com.backend.gapfinder.model.models.ClassBlockModel;

import java.util.List;

// Schedule returned by a ScheduleSource, already in the format the client understands
public record AdaptedSchedule(

    // Class blocks converted from the external source, in the same order the source returned them
    List<ClassBlockModel> classBlocks,

    // Events of the source that could not be converted to a class block (e.g. events on Sunday)
    int skippedEvents
) {}
