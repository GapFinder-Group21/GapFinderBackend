package com.backend.gapfinder.services;

import com.backend.gapfinder.dto.OpenTableAbandonmentStatsBasicDTO;
import com.backend.gapfinder.enums.OpenTableCreationStepEnum;
import com.backend.gapfinder.models.OpenTableAbandonmentModel;
import com.backend.gapfinder.repositories.OpenTableAbandonmentRepository;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

// Handles the registration and analytics of abandoned open table creation flows GRUPAL BQ - TYPE 2
@Slf4j
@Service
public class OpenTableAbandonmentService {

    private final OpenTableAbandonmentRepository abandonmentRepository;
    private final UserService userService;
    private final ActivityService activityService;
    private final BuildingService buildingService;
    private final OpenTableService openTableService;

    // Dependency injection constructor
    public OpenTableAbandonmentService(OpenTableAbandonmentRepository abandonmentRepository,
                                       UserService userService,
                                       ActivityService activityService,
                                       BuildingService buildingService,
                                       OpenTableService openTableService) {
        this.abandonmentRepository = abandonmentRepository;
        this.userService = userService;
        this.activityService = activityService;
        this.buildingService = buildingService;
        this.openTableService = openTableService;
    }

    // Register an abandoned creation flow for a user
    @Transactional
    public OpenTableAbandonmentModel create(Long userId, OpenTableAbandonmentModel abandonment) {
        log.info("Inicia proceso de registrar un abandono de open table para el usuario {}", userId);

        validateAbandonmentData(abandonment);
        abandonment.setUser(userService.getById(userId));

        log.info("Termina proceso de registrar un abandono de open table para el usuario {}", userId);
        return abandonmentRepository.save(abandonment);
    }

    // Validate that the abandonment data is consistent with the step where it happened
    private void validateAbandonmentData(OpenTableAbandonmentModel abandonment) {
        if (abandonment.getStep() == null) {
            throw new IllegalArgumentException("El paso del abandono es obligatorio");
        }

        // After the ACTIVITY step, the activity must already be selected
        if (abandonment.getStep() != OpenTableCreationStepEnum.ACTIVITY && abandonment.getActivityId() == null) {
            throw new IllegalArgumentException(
                    "Si el abandono fue después del paso ACTIVITY, la actividad ya debía estar seleccionada");
        }

        // Duration is tied to a selected activity and must be positive
        if (abandonment.getDurationMinutes() != null) {
            if (abandonment.getActivityId() == null) {
                throw new IllegalArgumentException("La duración va ligada a una actividad seleccionada");
            }
            if (abandonment.getDurationMinutes() <= 0) {
                throw new IllegalArgumentException("La duración debe ser mayor a 0");
            }
        }

        // Building can only be selected in the LOCATION step
        if (abandonment.getBuildingId() != null && abandonment.getStep() != OpenTableCreationStepEnum.LOCATION) {
            throw new IllegalArgumentException("Solo se puede tener edificio si el abandono fue en el paso LOCATION");
        }

        if (abandonment.getMaxParticipants() != null) {
            if (abandonment.getStep() != OpenTableCreationStepEnum.PARTICIPANTS) {
                throw new IllegalArgumentException(
                        "Solo se puede tener máximo de participantes si el abandono fue en el paso PARTICIPANTS");
            }
            if (abandonment.getMaxParticipants() <= 0) {
                throw new IllegalArgumentException("El máximo de participantes debe ser mayor a 0");
            }
        }

        // Verify that the referenced activity and building exist
        if (abandonment.getActivityId() != null) {
            activityService.getById(abandonment.getActivityId());
        }
        if (abandonment.getBuildingId() != null) {
            buildingService.getById(abandonment.getBuildingId());
        }
    }

    // Count the abandonments per step since the given date (steps without abandonments return 0)
    @Transactional(readOnly = true)
    public Map<OpenTableCreationStepEnum, Long> countByStep(LocalDateTime since) {
        Map<OpenTableCreationStepEnum, Long> counts = new EnumMap<>(OpenTableCreationStepEnum.class);
        for (OpenTableCreationStepEnum step : OpenTableCreationStepEnum.values()) {
            counts.put(step, 0L);
        }
        for (Object[] row : abandonmentRepository.countGroupedByStep(since)) {
            counts.put((OpenTableCreationStepEnum) row[0], (Long) row[1]);
        }
        return counts;
    }

    // Calculate abandonments, users who reached each step and abandonment rate per step since the given date
    @Transactional(readOnly = true)
    public List<OpenTableAbandonmentStatsBasicDTO> calculateAbandonmentRateByStep(LocalDateTime since) {
        log.info("Inicia proceso de calcular la tasa de abandono por paso");

        Map<OpenTableCreationStepEnum, Long> counts = countByStep(since);
        long created = openTableService.countCreatedSince(since);
        OpenTableCreationStepEnum[] steps = OpenTableCreationStepEnum.values();

        List<OpenTableAbandonmentStatsBasicDTO> stats = new ArrayList<>();
        for (OpenTableCreationStepEnum step : steps) {
            long abandonments = counts.get(step);

            // Reached step k: the open tables created + the abandonments at step k or later steps
            long reached = created;
            for (OpenTableCreationStepEnum other : steps) {
                if (other.ordinal() >= step.ordinal()) {
                    reached += counts.get(other);
                }
            }

            double rate = reached == 0 ? 0.0 : (double) abandonments / reached;
            stats.add(new OpenTableAbandonmentStatsBasicDTO(step, abandonments, reached, rate));
        }

        log.info("Termina proceso de calcular la tasa de abandono por paso");
        return stats;
    }

    // Get the step with the highest abandonment rate (ties broken by number of abandonments)
    @Transactional(readOnly = true)
    public Optional<OpenTableAbandonmentStatsBasicDTO> getMostAbandonedStep(LocalDateTime since) {
        return calculateAbandonmentRateByStep(since).stream()
                .filter(stat -> stat.abandonments() > 0)
                .max(Comparator.comparingDouble(OpenTableAbandonmentStatsBasicDTO::abandonmentRate)
                        .thenComparingLong(OpenTableAbandonmentStatsBasicDTO::abandonments));
    }
}