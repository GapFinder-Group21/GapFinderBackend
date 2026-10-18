package com.backend.gapfinder.services;

import com.backend.gapfinder.models.BuildingModel;
import com.backend.gapfinder.models.UserLocationLogModel;
import com.backend.gapfinder.models.UserModel;
import com.backend.gapfinder.repositories.UserRepository;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

// Handles the context-aware feature: current building of the user and friends nearby
// Smart Feature
@Slf4j
@Service
public class NearbyFriendsService {

    // Location updates older than this are considered stale
    private static final long RECENT_MINUTES = 30;

    private final UserService userService;
    private final UserRepository userRepository;
    private final BuildingService buildingService;
    private final UserLocationLogService locationLogService;
    private final FriendshipService friendshipService;

    // Dependency injection constructor
    public NearbyFriendsService(UserService userService,
                                UserRepository userRepository,
                                BuildingService buildingService,
                                UserLocationLogService locationLogService,
                                FriendshipService friendshipService) {
        this.userService = userService;
        this.userRepository = userRepository;
        this.buildingService = buildingService;
        this.locationLogService = locationLogService;
        this.friendshipService = friendshipService;
    }

    // Resolves the building from GPS coordinates, updates the user's context and returns friends nearby
    @Transactional
    public List<UserModel> updateLocationByCoordinates(Long userId, double latitude, double longitude) {
        log.info("Inicia proceso de actualizar ubicación del usuario {} por coordenadas", userId);

        Optional<BuildingModel> building = buildingService.findBuildingContainingUser(latitude, longitude);

        if (building.isEmpty()) {
            // User is outside every building: clear the current context
            UserModel user = userService.getById(userId);
            user.setCurrentBuilding(null);
            user.setLocationUpdatedAt(LocalDateTime.now());
            userRepository.save(user);

            log.info("El usuario {} no está dentro de ningún edificio", userId);
            return List.of();
        }

        List<UserModel> nearby = updateLocationAndGetNearbyFriends(userId, building.get().getId());

        log.info("Termina proceso de actualizar ubicación del usuario {} por coordenadas", userId);
        return nearby;
    }

    // Updates the user's current building, logs the location and returns friends in the same building
    @Transactional
    public List<UserModel> updateLocationAndGetNearbyFriends(Long userId, Long buildingId) {
        log.info("Inicia proceso de actualizar ubicación del usuario {} al edificio {}", userId, buildingId);

        UserModel user = userService.getById(userId);
        BuildingModel building = buildingService.getById(buildingId);

        // Fast context state
        user.setCurrentBuilding(building);
        user.setLocationUpdatedAt(LocalDateTime.now());
        userRepository.save(user);

        // History for analytics: one log per location update, sent by the app while a gap is active
        UserLocationLogModel locationLog = new UserLocationLogModel();
        locationLog.setUser(user);
        locationLog.setBuilding(building);
        locationLogService.create(locationLog);

        List<UserModel> nearby = findNearbyFriends(userId, buildingId);

        log.info("Termina proceso de actualizar ubicación del usuario {}", userId);
        return nearby;
    }

    // Returns the accepted friends currently in the user's current building
    @Transactional(readOnly = true)
    public List<UserModel> getNearbyFriends(Long userId) {
        log.info("Inicia proceso de consultar los amigos cercanos del usuario {}", userId);

        UserModel user = userService.getById(userId);

        if (user.getCurrentBuilding() == null) {
            return List.of();
        }

        List<UserModel> nearby = findNearbyFriends(userId, user.getCurrentBuilding().getId());

        log.info("Termina proceso de consultar los amigos cercanos del usuario {}", userId);
        return nearby;
    }

    // Filters the user's accepted friends by building and recent location update
    private List<UserModel> findNearbyFriends(Long userId, Long buildingId) {
        LocalDateTime since = LocalDateTime.now().minusMinutes(RECENT_MINUTES);

        return friendshipService.getFriendsByUser(userId).stream()
                .filter(f -> f.getCurrentBuilding() != null
                        && f.getCurrentBuilding().getId().equals(buildingId)
                        && f.getLocationUpdatedAt() != null
                        && f.getLocationUpdatedAt().isAfter(since))
                .toList();
    }
}