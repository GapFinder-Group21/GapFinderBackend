package com.backend.gapfinder.model.services;

import com.backend.gapfinder.model.models.AppSessionModel;
import com.backend.gapfinder.model.repositories.AppSessionRepository;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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

    // Register an opening of the app for a user (the timestamp is set to now on persist)
    @Transactional
    public AppSessionModel register(Long userId) {
        log.info("Inicia proceso de registrar una apertura de la app para el usuario {}", userId);

        AppSessionModel session = new AppSessionModel();
        session.setUser(userService.getById(userId));
        AppSessionModel saved = appSessionRepository.save(session);

        log.info("Termina proceso de registrar una apertura de la app para el usuario {}", userId);
        return saved;
    }
}
