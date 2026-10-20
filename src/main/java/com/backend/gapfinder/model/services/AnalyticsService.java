package com.backend.gapfinder.model.services;

import com.backend.gapfinder.view.dto.responses.BuildingGapPresenceResponseDTO;
import com.backend.gapfinder.view.dto.responses.ConnectionMethodResponseDTO;
import com.backend.gapfinder.view.dto.responses.GapCoverageResponseDTO;
import com.backend.gapfinder.view.dto.AppOpenDelayRangeStatsBasicDTO;
import com.backend.gapfinder.view.dto.AppOpenDelayStatsDTO;
import com.backend.gapfinder.view.dto.LocationUpdateRangeStatsBasicDTO;
import com.backend.gapfinder.view.dto.LocationUpdateRateStatsDTO;
import com.backend.gapfinder.view.dto.StudentLocationUpdateRateBasicDTO;
import com.backend.gapfinder.view.dto.OpenTableAbandonmentStatsBasicDTO;
import com.backend.gapfinder.view.dto.SharedInterestGroupStatsBasicDTO;
import com.backend.gapfinder.view.dto.UnmatchedFreeTimeStatsBasicDTO;
import com.backend.gapfinder.view.dto.UnusedActivityStatsDTO;
import com.backend.gapfinder.model.enums.MatchStatusEnum;
import com.backend.gapfinder.model.enums.OpenTableStatusEnum;
import com.backend.gapfinder.model.exceptions.NotFoundException;
import com.backend.gapfinder.model.repositories.ActivityRepository;
import com.backend.gapfinder.model.repositories.AppSessionRepository;
import com.backend.gapfinder.model.repositories.GapRepository;
import com.backend.gapfinder.model.repositories.MatchRepository;
import com.backend.gapfinder.model.repositories.OpenTableRepository;
import com.backend.gapfinder.model.repositories.UserLocationLogRepository;
import com.backend.gapfinder.model.repositories.projections.AppOpenDelayProjection;
import com.backend.gapfinder.model.repositories.projections.LocationUpdateSummaryProjection;
import com.backend.gapfinder.model.repositories.projections.SharedInterestGroupProjection;
import com.backend.gapfinder.model.repositories.projections.UnmatchedFreeTimeProjection;
import com.backend.gapfinder.model.repositories.projections.UnusedActivityProjection;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
public class AnalyticsService {

    private final OpenTableAbandonmentService abandonmentService;
    private final GapRepository gapRepository;
    private final UserLocationLogRepository locationLogRepository;
    private final MatchRepository matchRepository;
    private final OpenTableRepository openTableRepository;
    private final AppSessionRepository appSessionRepository;
    private final UserService userService;
    private final ActivityRepository activityRepository;

    public AnalyticsService(OpenTableAbandonmentService abandonmentService, GapRepository gapRepository,
                            UserLocationLogRepository locationLogRepository, MatchRepository matchRepository,
                            OpenTableRepository openTableRepository, AppSessionRepository appSessionRepository,
                            UserService userService, ActivityRepository activityRepository) {
        this.activityRepository = activityRepository;
        this.appSessionRepository = appSessionRepository;
        this.userService = userService;
        this.abandonmentService = abandonmentService;
        this.gapRepository = gapRepository;
        this.locationLogRepository = locationLogRepository;
        this.matchRepository = matchRepository;
        this.openTableRepository = openTableRepository;
    }

    // ==================== BQ 2 TYPE 1 GRUPAL ====================
    // How many location updates per hour does the backend receive from each student while they are inside a campus building?

    // Ranges of updates per student-hour in natural order
    private static final List<String> LOCATION_UPDATE_RANGES = List.of("1", "2-5", "6-15", "16-30", "31+");

    // Location updates per hour received from the students while inside a campus building: summary of all the
    // student-hours, their distribution per range and the rate of each student (highest first)
    // since: optional, whole history if null. limit: optional, all the students if null
    @Transactional(readOnly = true)
    public LocationUpdateRateStatsDTO getLocationUpdatesPerHour(LocalDateTime since, Integer limit) {
        LocalDateTime start = since != null ? since : HISTORY_START;

        log.info("Inicia proceso de consultar las actualizaciones de ubicación por hora desde {}", start);

        if (limit != null && limit <= 0) {
            throw new IllegalArgumentException("El límite debe ser mayor a 0");
        }

        LocationUpdateSummaryProjection summary = locationLogRepository.findLocationUpdateSummary(start);
        long studentHours = summary.getStudentHours() != null ? summary.getStudentHours() : 0L;
        if (studentHours == 0) {
            throw new NotFoundException("No hay actualizaciones de ubicación registradas desde la fecha indicada");
        }

        // Student-hours per range, with 0 for the ranges that do not appear in the query
        Map<String, Long> hoursByRange = new LinkedHashMap<>();
        LOCATION_UPDATE_RANGES.forEach(range -> hoursByRange.put(range, 0L));
        locationLogRepository.countStudentHoursByUpdatesRange(start)
                .forEach(row -> hoursByRange.put(row.getUpdatesRange(), row.getStudentHours()));

        List<LocationUpdateRangeStatsBasicDTO> distribution = hoursByRange.entrySet().stream()
                .map(entry -> new LocationUpdateRangeStatsBasicDTO(
                        entry.getKey(), entry.getValue(), (double) entry.getValue() / studentHours * 100))
                .toList();

        // Already sorted by the query from the highest to the lowest average
        List<StudentLocationUpdateRateBasicDTO> students = locationLogRepository
                .findLocationUpdateRateByStudent(start).stream()
                .limit(limit != null ? limit : Long.MAX_VALUE)
                .map(row -> new StudentLocationUpdateRateBasicDTO(
                        row.getUserId(),
                        row.getUserName(),
                        row.getAveragePerHour(),
                        row.getMaxPerHour(),
                        row.getHoursInside()))
                .toList();

        log.info("Termina proceso de consultar las actualizaciones de ubicación por hora desde {}", start);
        return new LocationUpdateRateStatsDTO(
                summary.getAveragePerHour(),
                summary.getMedianPerHour(),
                summary.getMaxPerHour(),
                studentHours,
                summary.getStudents(),
                distribution,
                students);
    }

    // ================== END BQ 2 TYPE 1 GRUPAL ==================

    // ==================== BQ 3 TYPE 2 GRUPAL ====================
    // At which step do students most often leave the Open Table creating process?

    // Retrieves the creation step with the highest abandonment rate since the given date
    @Transactional(readOnly = true)
    public OpenTableAbandonmentStatsBasicDTO getMostAbandonedStep(LocalDateTime since) {
        log.info("Inicia proceso de consultar el paso más abandonado desde {}", since);

        OpenTableAbandonmentStatsBasicDTO result = abandonmentService.getMostAbandonedStep(since)
                .orElseThrow(() -> new NotFoundException("No hay abandonos registrados desde la fecha indicada"));

        log.info("Termina proceso de consultar el paso más abandonado desde {}", since);
        return result;
    }

    // ================== END BQ 3 TYPE 2 GRUPAL ==================

    // ==================== BQ 5 ====================
    // Miguel Cabrera: Which gap durations have the lowest match coverage?

    // Retrieves match coverage metrics grouped by gap duration buckets sorted by lowest coverage
    @Transactional(readOnly = true)
    public List<GapCoverageResponseDTO> getMatchCoverageByGapDuration() {
        log.info("Inicia proceso de consultar la cobertura de matches por duración de gap");

        List<GapCoverageResponseDTO> result = gapRepository.findCoverageByDurationBucket().stream()
                .map(row -> {
                    double totalGapMinutes = ((Number) row[2]).doubleValue();
                    double totalMatchedMinutes = ((Number) row[3]).doubleValue();

                    GapCoverageResponseDTO dto = new GapCoverageResponseDTO();
                    dto.setDurationRange((String) row[0]);
                    dto.setGapCount(((Number) row[1]).longValue());
                    dto.setTotalGapMinutes(totalGapMinutes);
                    dto.setTotalMatchedMinutes(totalMatchedMinutes);
                    dto.setCoveragePercent(
                            calculateCoveragePercent(totalMatchedMinutes, totalGapMinutes)
                    );

                    return dto;
                })
                .sorted(Comparator.comparingDouble(GapCoverageResponseDTO::getCoveragePercent))
                .toList();

        log.info("Termina proceso de consultar la cobertura de matches por duración de gap");
        return result;
    }

    // Calculates the coverage percentage based on matched minutes and total gap minutes
    private double calculateCoveragePercent(double matchedMinutes, double gapMinutes) {
        if (gapMinutes <= 0) {
            return 0.0;
        }

        return matchedMinutes / gapMinutes * 100;
    }

    // ================== END BQ 5 ==================

    // ==================== BQ 7 ====================
    // Sofia Arias: Which connection method leads to more accepted meetups: open tables or matches?

    // Compares the percentage of completed matches against the percentage of completed open tables
    @Transactional(readOnly = true)
    public ConnectionMethodResponseDTO compareConnectionMethods() {
        log.info("Inicia proceso de comparar los métodos de conexión (matches vs open tables)");

        long completedMatches = matchRepository.countByStatus(MatchStatusEnum.COMPLETED);
        long totalMatches = matchRepository.count();
        long completedOpenTables = openTableRepository.countByStatus(OpenTableStatusEnum.COMPLETED);
        long totalOpenTables = openTableRepository.count();

        double matchPercent = calculateCompletionPercent(completedMatches, totalMatches);
        double openTablePercent = calculateCompletionPercent(completedOpenTables, totalOpenTables);

        ConnectionMethodResponseDTO dto = new ConnectionMethodResponseDTO();
        dto.setCompletedMatches(completedMatches);
        dto.setTotalMatches(totalMatches);
        dto.setMatchCompletionPercent(matchPercent);
        dto.setCompletedOpenTables(completedOpenTables);
        dto.setTotalOpenTables(totalOpenTables);
        dto.setOpenTableCompletionPercent(openTablePercent);
        dto.setWinner(matchPercent > openTablePercent ? "MATCHES"
                : openTablePercent > matchPercent ? "OPEN_TABLES" : "TIE");

        log.info("Termina proceso de comparar los métodos de conexión (matches vs open tables)");
        return dto;
    }

    // Calculates the completion percentage based on completed and total records
    private double calculateCompletionPercent(long completed, long total) {
        if (total <= 0) {
            return 0.0;
        }

        return (double) completed / total * 100;
    }

    // ================== END BQ 7 ==================

    // ==================== BQ 12 INDIVIDUAL ====================
    // Sofia Morato: Which careers and semesters have the highest rate of unmatched free time on campus?

    // Lower bound used when no start date is given (the whole history)
    private static final LocalDateTime HISTORY_START = LocalDateTime.of(1970, 1, 1, 0, 0);

    // Ranks the (career, semester) groups by their unmatched free time rate, highest first
    // from: optional, whole history if null. to: optional, now if null (future gaps can still be matched)
    // limit: optional, all groups if null
    @Transactional(readOnly = true)
    public List<UnmatchedFreeTimeStatsBasicDTO> getUnmatchedFreeTimeByCareerAndSemester(
            LocalDateTime from, LocalDateTime to, Integer limit) {

        LocalDateTime start = from != null ? from : HISTORY_START;
        LocalDateTime end = to != null ? to : LocalDateTime.now();

        log.info("Inicia proceso de consultar la tasa de tiempo libre sin match por carrera y semestre entre {} y {}",
                start, end);

        if (!start.isBefore(end)) {
            throw new IllegalArgumentException("La fecha 'from' debe ser antes de la fecha 'to'");
        }
        if (limit != null && limit <= 0) {
            throw new IllegalArgumentException("El límite debe ser mayor a 0");
        }

        List<UnmatchedFreeTimeStatsBasicDTO> stats = gapRepository
                .findFreeTimeByCareerAndSemester(start, end).stream()
                .map(this::toUnmatchedFreeTimeStats)
                .sorted(Comparator.comparingDouble(UnmatchedFreeTimeStatsBasicDTO::unmatchedRate)
                        .thenComparingDouble(UnmatchedFreeTimeStatsBasicDTO::unmatchedMinutes)
                        .reversed())
                .limit(limit != null ? limit : Long.MAX_VALUE)
                .toList();

        if (stats.isEmpty()) {
            throw new NotFoundException("No hay gaps registrados en el rango de fechas indicado");
        }

        log.info("Termina proceso de consultar la tasa de tiempo libre sin match por carrera y semestre entre {} y {}",
                start, end);
        return stats;
    }

    // Builds the statistics of a group calculating its unmatched minutes and rate
    private UnmatchedFreeTimeStatsBasicDTO toUnmatchedFreeTimeStats(UnmatchedFreeTimeProjection row) {
        double totalMinutes = row.getTotalGapMinutes() != null ? row.getTotalGapMinutes() : 0.0;
        double matchedMinutes = row.getMatchedMinutes() != null ? row.getMatchedMinutes() : 0.0;
        double unmatchedMinutes = Math.max(totalMinutes - matchedMinutes, 0.0);
        double rate = totalMinutes == 0 ? 0.0 : unmatchedMinutes / totalMinutes;

        return new UnmatchedFreeTimeStatsBasicDTO(
                row.getCareer(),
                row.getSemester(),
                row.getStudents(),
                row.getTotalGaps(),
                row.getUnmatchedGaps(),
                totalMinutes,
                unmatchedMinutes,
                rate);
    }

    // ================== END BQ 12 INDIVIDUAL ==================


    // ==================== BQ 11 ====================
    // Joel: Where do the most students have free time on campus?

    // Retrieves student presence and free time metrics aggregated by campus building
    @Transactional(readOnly = true)
    public List<BuildingGapPresenceResponseDTO> getBuildingsByGapPresence() {
        log.info("Inicia proceso de consultar la presencia de estudiantes en gaps por edificio");

        List<BuildingGapPresenceResponseDTO> result = locationLogRepository.findGapPresenceByBuilding().stream()
                .map(p -> new BuildingGapPresenceResponseDTO(
                        p.getBuildingId(),
                        p.getBuildingName(),
                        p.getStudentCount(),
                        p.getGapCount(),
                        p.getTotalGapMinutes()))
                .toList();

        log.info("Termina proceso de consultar la presencia de estudiantes en gaps por edificio");
        return result;
    }

    // ================== END BQ 11 ==================

    // ==================== BQ 13 INDIVIDUAL ====================
    // Daniela Martinez: Which interests are shared by the largest groups of students who are free at the same time?

    // Upper bound used when no end date is given (LocalDateTime.MAX is out of the PostgreSQL timestamp range)
    private static final LocalDateTime FUTURE_END = LocalDateTime.of(9999, 12, 31, 23, 59);

    // Ranks the interests by the largest group of students sharing them who are free at the same time, largest first
    // from: optional, whole history if null. to: optional, no upper bound if null
    // limit: optional, all interests if null
    @Transactional(readOnly = true)
    public List<SharedInterestGroupStatsBasicDTO> getInterestsByLargestFreeGroup(
            LocalDateTime from, LocalDateTime to, Integer limit) {

        LocalDateTime start = from != null ? from : HISTORY_START;
        LocalDateTime end = to != null ? to : FUTURE_END;

        log.info("Inicia proceso de consultar los intereses compartidos por los grupos libres más grandes entre {} y {}",
                start, end);

        if (!start.isBefore(end)) {
            throw new IllegalArgumentException("La fecha 'from' debe ser antes de la fecha 'to'");
        }
        if (limit != null && limit <= 0) {
            throw new IllegalArgumentException("El límite debe ser mayor a 0");
        }

        List<SharedInterestGroupStatsBasicDTO> stats = gapRepository
                .findLargestFreeGroupByInterest(start, end).stream()
                .map(this::toSharedInterestGroupStats)
                .sorted(Comparator.comparingLong(SharedInterestGroupStatsBasicDTO::largestGroupSize)
                        .thenComparingLong(SharedInterestGroupStatsBasicDTO::studentsWithInterest)
                        .reversed())
                .limit(limit != null ? limit : Long.MAX_VALUE)
                .toList();

        if (stats.isEmpty()) {
            throw new NotFoundException("No hay estudiantes con intereses y gaps en el rango de fechas indicado");
        }

        log.info("Termina proceso de consultar los intereses compartidos por los grupos libres más grandes entre {} y {}",
                start, end);
        return stats;
    }

    // Builds the statistics of an interest from the query row
    private SharedInterestGroupStatsBasicDTO toSharedInterestGroupStats(SharedInterestGroupProjection row) {
        return new SharedInterestGroupStatsBasicDTO(
                row.getInterestId(),
                row.getInterestName(),
                row.getLargestGroupSize() != null ? row.getLargestGroupSize() : 0L,
                row.getPeakTime(),
                row.getStudentsWithInterest() != null ? row.getStudentsWithInterest() : 0L);
    }

    // ================== END BQ 13 INDIVIDUAL ==================

    // ==================== BQ 4 INDIVIDUAL TYPE 2 ====================
    // Sofia Morato: How long after a gap starts do students open the app?

    // Ranges of minutes in natural order; the last one groups the gaps where the app was not opened
    private static final List<String> APP_OPEN_RANGES = List.of("0-5", "5-15", "15-30", "30+", "No abrió");

    // Minimum gaps of the user needed to trust their own average
    private static final long MIN_USER_GAPS = 3;

    // Bounds of the reminder offset in minutes (not instant, not too late to be useful)
    private static final int MIN_OFFSET_MINUTES = 2;
    private static final int MAX_OFFSET_MINUTES = 30;

    // Offset used when nobody has data yet
    private static final int DEFAULT_OFFSET_MINUTES = 5;

    // Distribution of the minutes after a gap starts at which the app is first opened, plus the average of all users
    // userId: optional, null for the dashboard. With a user it also returns the minutes after the gap start to send them the reminder
    // since: optional, whole history if null
    @Transactional(readOnly = true)
    public AppOpenDelayStatsDTO getAppOpenDelay(Long userId, LocalDateTime since) {
        LocalDateTime start = since != null ? since : HISTORY_START;

        log.info("Inicia proceso de consultar el retraso en abrir la app tras el inicio de un gap desde {}", start);

        if (userId != null) {
            userService.getById(userId);
        }

        // Gaps per range, with 0 for the ranges that do not appear in the query
        Map<String, Long> gapsByRange = new LinkedHashMap<>();
        APP_OPEN_RANGES.forEach(range -> gapsByRange.put(range, 0L));
        appSessionRepository.countGapsByOpenDelayRange(start)
                .forEach(row -> gapsByRange.put(row.getDelayRange(), row.getGaps()));

        long totalGaps = gapsByRange.values().stream().mapToLong(Long::longValue).sum();
        if (totalGaps == 0) {
            throw new NotFoundException("No hay gaps terminados registrados desde la fecha indicada");
        }

        List<AppOpenDelayRangeStatsBasicDTO> distribution = gapsByRange.entrySet().stream()
                .map(entry -> new AppOpenDelayRangeStatsBasicDTO(
                        entry.getKey(), entry.getValue(), (double) entry.getValue() / totalGaps * 100))
                .toList();

        // Most frequent range where the app was opened (the first one in natural order wins a tie)
        String mostFrequentRange = distribution.stream()
                .filter(stat -> !stat.range().equals("No abrió") && stat.gaps() > 0)
                .max(Comparator.comparingLong(AppOpenDelayRangeStatsBasicDTO::gaps))
                .map(AppOpenDelayRangeStatsBasicDTO::range)
                .orElse(null);

        AppOpenDelayProjection averages = appSessionRepository.findAppOpenDelayAverages(userId, start);
        long globalGaps = averages.getGlobalGapsCounted() != null ? averages.getGlobalGapsCounted() : 0L;
        Double globalAverage = globalGaps > 0 ? averages.getGlobalAverageMinutes() : null;

        // Without a user there is no reminder offset
        if (userId == null) {
            log.info("Termina proceso de consultar el retraso en abrir la app tras el inicio de un gap desde {}", start);
            return new AppOpenDelayStatsDTO(distribution, mostFrequentRange, globalAverage, globalGaps,
                    null, null, null, null);
        }

        long userGaps = averages.getUserGapsCounted() != null ? averages.getUserGapsCounted() : 0L;
        Double userAverage = userGaps > 0 ? averages.getUserAverageMinutes() : null;

        // The user's own average if they have enough gaps, else the average of all users, else a default
        double offset;
        String source;
        if (userGaps >= MIN_USER_GAPS && userAverage != null) {
            offset = userAverage;
            source = "USER";
        } else if (globalAverage != null) {
            offset = globalAverage;
            source = "GLOBAL";
        } else {
            offset = DEFAULT_OFFSET_MINUTES;
            source = "DEFAULT";
        }

        // Whole minutes, limited to the allowed range
        int offsetMinutes = (int) Math.max(MIN_OFFSET_MINUTES, Math.min(MAX_OFFSET_MINUTES, Math.round(offset)));

        log.info("Termina proceso de consultar el retraso en abrir la app tras el inicio de un gap desde {}", start);
        return new AppOpenDelayStatsDTO(distribution, mostFrequentRange, globalAverage, globalGaps,
                userAverage, userGaps, offsetMinutes, source);
    }

    // ================== END BQ 4 INDIVIDUAL TYPE 2 ==================

    // ==================== BQ 8 INDIVIDUAL TYPE 3 ====================
    // Sofia Morato: Which activities are never or rarely chosen when creating open tables?

    // An activity chosen in less than this percentage of the open tables of the period is RARELY chosen
    // 5 % is a simple, easy to explain cut: an activity that gets less than 1 of every 20 open tables is clearly ignored
    private static final double RARELY_CHOSEN_THRESHOLD_PERCENT = 5.0;

    // Days used as the default start date, the same as the dashboard
    private static final int DEFAULT_ACTIVITY_DAYS_BACK = 30;

    // Lists ALL the activities from the least to the most chosen, classified as NEVER, RARELY or OK
    // since: optional, last 30 days if null. limit: optional, all activities if null
    @Transactional(readOnly = true)
    public List<UnusedActivityStatsDTO> getUnusedActivities(LocalDateTime since, Integer limit) {
        LocalDateTime start = since != null ? since : LocalDateTime.now().minusDays(DEFAULT_ACTIVITY_DAYS_BACK);

        log.info("Inicia proceso de consultar las actividades nunca o raramente elegidas desde {}", start);

        if (limit != null && limit <= 0) {
            throw new IllegalArgumentException("El límite debe ser mayor a 0");
        }

        List<UnusedActivityProjection> rows = activityRepository.findActivityUsageSince(start);
        if (rows.isEmpty()) {
            throw new NotFoundException("No hay actividades registradas");
        }

        // The percentage is over the open tables that chose an activity, not over all the open tables
        long totalTables = rows.stream()
                .mapToLong(row -> row.getOpenTables() != null ? row.getOpenTables() : 0L)
                .sum();

        // The query already sorts them from the least to the most chosen
        List<UnusedActivityStatsDTO> stats = rows.stream()
                .map(row -> toUnusedActivityStats(row, totalTables))
                .limit(limit != null ? limit : Long.MAX_VALUE)
                .toList();

        log.info("Termina proceso de consultar las actividades nunca o raramente elegidas desde {}", start);
        return stats;
    }

    // Builds the statistics of an activity with its share of the open tables and its classification
    private UnusedActivityStatsDTO toUnusedActivityStats(UnusedActivityProjection row, long totalTables) {
        long tables = row.getOpenTables() != null ? row.getOpenTables() : 0L;
        double percentage = totalTables == 0 ? 0.0 : (double) tables / totalTables * 100;

        String classification;
        if (tables == 0) {
            classification = "NEVER";
        } else if (percentage < RARELY_CHOSEN_THRESHOLD_PERCENT) {
            classification = "RARELY";
        } else {
            classification = "OK";
        }

        return new UnusedActivityStatsDTO(
                row.getActivityId(),
                row.getActivityName(),
                row.getInterestName(),
                tables,
                percentage,
                classification);
    }

    // ================== END BQ 8 INDIVIDUAL TYPE 3 ==================
}
