package com.backend.gapfinder.services;

import com.backend.gapfinder.builders.OpenTableRecommendationResponseBuilder;
import com.backend.gapfinder.dto.OpenTableCompleteDTO;
import com.backend.gapfinder.dto.responses.OpenTableRecommendationResponseDTO;
import com.backend.gapfinder.enums.OpenTableStatusEnum;
import com.backend.gapfinder.repositories.OpenTableRepository;
import com.backend.gapfinder.repositories.UserLocationLogRepository;
import com.backend.gapfinder.repositories.projections.FavoriteBuildingProjection;

import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

// Handles recommendation logic for open tables based on user locations and gap schedules
// Smart Feature
@Slf4j
@Service
public class RecommendationService {

    private final UserLocationLogRepository locationLogRepository;
    private final OpenTableRepository openTableRepository;
    private final ModelMapper modelMapper;

    // Dependency injection constructor
    public RecommendationService(UserLocationLogRepository locationLogRepository,
                                 OpenTableRepository openTableRepository,
                                 ModelMapper modelMapper) {
        this.locationLogRepository = locationLogRepository;
        this.openTableRepository = openTableRepository;
        this.modelMapper = modelMapper;
    }

    // Finds the user's favorite building and recommends OPEN tables matching the user's active gaps
    @Transactional(readOnly = true)
    public OpenTableRecommendationResponseDTO recommendOpenTables(Long userId) {
        log.info("Inicia proceso de recomendar open tables para el usuario {}", userId);

        // Retrieve the building where the user spends the most time during their gaps
        Optional<FavoriteBuildingProjection> favorite = locationLogRepository.findFavoriteBuilding(userId);
        if (favorite.isEmpty()) {
            log.info("El usuario {} no tiene logs de ubicación dentro de sus gaps", userId);
            return new OpenTableRecommendationResponseBuilder().build();
        }

        FavoriteBuildingProjection f = favorite.get();

        // Query active matching open tables and map entities to response DTOs
        List<OpenTableCompleteDTO> tables = openTableRepository
                .findMatchingOpenTables(userId, f.getBuildingId(), OpenTableStatusEnum.OPEN, LocalDateTime.now())
                .stream()
                .map(o -> modelMapper.map(o, OpenTableCompleteDTO.class))
                .toList();

        log.info("Termina proceso de recomendar open tables para el usuario {}", userId);
        return new OpenTableRecommendationResponseBuilder()
                .withFavoriteBuilding(f.getBuildingId(), f.getBuildingName())
                .withTotalMinutes(f.getTotalMinutes())
                .withOpenTables(tables)
                .build();
    }
}