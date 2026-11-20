package com.backend.gapfinder.model.services;

import com.backend.gapfinder.model.exceptions.AlreadyDoneException;
import com.backend.gapfinder.model.exceptions.ForbiddenException;
import com.backend.gapfinder.model.models.StudyPeriodModel;
import com.backend.gapfinder.model.models.UserModel;
import com.backend.gapfinder.model.repositories.StudyPeriodRepository;

import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

// Study mode: the user hides from the matches and from the nearby friends while a study period lasts.
// The filters themselves live in GapRepository (candidates, match requests) and NearbyFriendsService
@Slf4j
@Service
public class StudyModeService {

    private final StudyPeriodRepository studyPeriodRepository;
    private final UserService userService;

    public StudyModeService(StudyPeriodRepository studyPeriodRepository, UserService userService) {
        this.studyPeriodRepository = studyPeriodRepository;
        this.userService = userService;
    }

    // Study periods of the user that are active now or start later, the earliest first
    @Transactional(readOnly = true)
    public List<StudyPeriodModel> getActivePeriods(Long userId) {
        log.info("Inicia proceso de consultar los periodos de estudio del usuario {}", userId);

        List<StudyPeriodModel> periods = studyPeriodRepository
                .findByUserIdAndEndTimeAfterOrderByStartTimeAsc(userId, LocalDateTime.now());

        log.info("Termina proceso de consultar los periodos de estudio del usuario {}: {} periodos", userId, periods.size());
        return periods;
    }

    // Saves a study period of the user
    @Transactional
    public StudyPeriodModel create(Long userId, LocalDateTime startTime, LocalDateTime endTime,
                                   Long gapId, String clientId) {
        log.info("Inicia proceso de crear un periodo de estudio del usuario {}", userId);

        validatePeriod(startTime, endTime);
        validateClientId(clientId);

        // Retry of a period that was already stored → 409 (the app outbox counts it as done)
        if (studyPeriodRepository.existsByUserIdAndClientId(userId, clientId)) {
            throw new AlreadyDoneException("Este periodo de estudio ya fue registrado");
        }

        UserModel user = userService.getById(userId);

        StudyPeriodModel period = new StudyPeriodModel();
        period.setUser(user);
        period.setStartTime(startTime);
        period.setEndTime(endTime);
        period.setGapId(gapId);
        period.setClientId(clientId);
        period.setCreatedAt(LocalDateTime.now());

        try {
            StudyPeriodModel saved = studyPeriodRepository.saveAndFlush(period);
            log.info("Termina proceso de crear el periodo de estudio con id = {}", saved.getId());
            return saved;
        } catch (DataIntegrityViolationException ex) {
            // Two retries arrived at the same time: the unique (user_id, client_id) kept only one
            throw new AlreadyDoneException("Este periodo de estudio ya fue registrado");
        }
    }

    // Removes a study period. Idempotent: a period that does not exist counts as already removed.
    // A period of another user cannot be removed
    @Transactional
    public void delete(Long userId, Long periodId) {
        log.info("Inicia proceso de eliminar el periodo de estudio {} del usuario {}", periodId, userId);

        Optional<StudyPeriodModel> found = studyPeriodRepository.findById(periodId);
        if (found.isEmpty()) {
            log.info("El periodo de estudio {} ya no existe, no hay nada que eliminar", periodId);
            return;
        }

        if (!found.get().getUser().getId().equals(userId)) {
            throw new ForbiddenException("No tienes permiso para eliminar este periodo de estudio");
        }

        studyPeriodRepository.delete(found.get());
        log.info("Termina proceso de eliminar el periodo de estudio {}", periodId);
    }

    // The period has to end after it starts and last at most 12 hours
    private static void validatePeriod(LocalDateTime startTime, LocalDateTime endTime) {
        if (startTime == null || endTime == null) {
            throw new IllegalArgumentException("Faltan la hora de inicio y la de fin del periodo de estudio");
        }
        if (!endTime.isAfter(startTime)) {
            throw new IllegalArgumentException("La hora de fin debe ser posterior a la hora de inicio");
        }
        if (Duration.between(startTime, endTime).compareTo(Duration.ofHours(StudyPeriodModel.MAX_HOURS)) > 0) {
            throw new IllegalArgumentException("El periodo de estudio no puede durar más de "
                    + StudyPeriodModel.MAX_HOURS + " horas");
        }
    }

    private static void validateClientId(String clientId) {
        if (clientId == null || clientId.isBlank()) {
            throw new IllegalArgumentException("Falta el clientId del periodo de estudio");
        }
        if (clientId.length() > StudyPeriodModel.MAX_CLIENT_ID_LENGTH) {
            throw new IllegalArgumentException("El clientId no puede tener más de "
                    + StudyPeriodModel.MAX_CLIENT_ID_LENGTH + " caracteres");
        }
    }
}
