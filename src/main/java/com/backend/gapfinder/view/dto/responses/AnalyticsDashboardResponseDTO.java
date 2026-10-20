package com.backend.gapfinder.view.dto.responses;

import com.backend.gapfinder.view.dto.AppOpenDelayStatsDTO;
import com.backend.gapfinder.view.dto.OpenTableAbandonmentStatsBasicDTO;
import com.backend.gapfinder.view.dto.PreferredActivityStatsDTO;
import com.backend.gapfinder.view.dto.SharedInterestGroupStatsBasicDTO;
import com.backend.gapfinder.view.dto.UnmatchedFreeTimeStatsBasicDTO;
import com.backend.gapfinder.view.dto.UnusedActivityStatsDTO;

import lombok.Data;

import java.util.List;

// Response with the answers of all the business questions, used by the analytics dashboard
@Data
public class AnalyticsDashboardResponseDTO {

    // BQ 3: creation step with the highest abandonment rate (null if there are no abandonments)
    private OpenTableAbandonmentStatsBasicDTO mostAbandonedStep;

    // BQ 4: minutes after a gap starts at which the app is first opened, per range (null if there are no finished gaps)
    private AppOpenDelayStatsDTO appOpenDelay;

    // BQ 6: activities most chosen when creating open tables and the share of students who always repeat the same one
    // (null if there are no open tables with activity in the period)
    private PreferredActivityStatsDTO preferredActivity;

    // BQ 5: match coverage per gap duration, lowest coverage first
    private List<GapCoverageResponseDTO> gapCoverageByDuration;

    // BQ 7: completion percentage of matches vs open tables
    private ConnectionMethodResponseDTO connectionMethods;

    // BQ 11: student presence and free time per building
    private List<BuildingGapPresenceResponseDTO> buildingsByGapPresence;

    // BQ 12: (career, semester) groups by unmatched free time rate, highest first (empty if there are no gaps)
    private List<UnmatchedFreeTimeStatsBasicDTO> unmatchedFreeTimeByCareerSemester;

    // BQ 13: interests by the largest group of students free at the same time, largest first (empty if there is no data)
    private List<SharedInterestGroupStatsBasicDTO> interestsByLargestFreeGroup;

    // BQ 8: all the activities from the least to the most chosen in open tables (empty if there are no activities)
    private List<UnusedActivityStatsDTO> unusedActivities;
}