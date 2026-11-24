package com.backend.gapfinder.model.services;

import com.backend.gapfinder.model.models.AppSessionModel;
import com.backend.gapfinder.model.repositories.AppSessionRepository;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;

// Handles the registration of the openings of the app TYPE 2
@Slf4j
@Service
public class AppSessionService {

    private final AppSessionRepository appSessionRepository;
    private final UserService userService;

    // Dependency injection constructor
    public AppSessionService(AppSessionRepository appSessionRepository, UserService userService) {
        this.appSessionRepository = appSessionRepository;
        this.userService = userService;
    }

    // An opening reported by the phone can be at most this old (a queued opening is sent when the signal returns)
    static final Duration MAX_AGE = Duration.ofDays(7);

    // Register an opening of the app for a user, now
    @Transactional
    public AppSessionModel register(Long userId) {
        return register(userId, null);
    }

    // Register an opening of the app for a user. openedAt is the time the phone saw (it can be earlier than now
    // when the opening was queued offline); an empty, future or too old value is replaced by the current time
    @Transactional
    public AppSessionModel register(Long userId, LocalDateTime openedAt) {
        log.info("Inicia proceso de registrar una apertura de la app para el usuario {}", userId);

        AppSessionModel session = new AppSessionModel();
        session.setUser(userService.getById(userId));
        session.setOpenedAt(resolveOpenedAt(openedAt, LocalDateTime.now()));
        AppSessionModel saved = appSessionRepository.save(session);

        log.info("Termina proceso de registrar una apertura de la app para el usuario {}", userId);
        return saved;
    }

    // The time that is saved: the one from the phone when it is believable, otherwise the current time
    static LocalDateTime resolveOpenedAt(LocalDateTime requested, LocalDateTime now) {
        if (requested == null || requested.isAfter(now) || requested.isBefore(now.minus(MAX_AGE))) {
            return now;
        }
        return requested;
    }
}
