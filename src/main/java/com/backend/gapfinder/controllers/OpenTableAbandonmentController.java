package com.backend.gapfinder.controllers;

import com.backend.gapfinder.dto.OpenTableAbandonmentBasicDTO;
import com.backend.gapfinder.dto.OpenTableAbandonmentStatsBasicDTO;
import com.backend.gapfinder.enums.OpenTableCreationStepEnum;
import com.backend.gapfinder.exceptions.NotFoundException;
import com.backend.gapfinder.models.OpenTableAbandonmentModel;
import com.backend.gapfinder.services.OpenTableAbandonmentService;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import jakarta.validation.Valid;

import org.modelmapper.ModelMapper;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/open-table-abandonments")
public class OpenTableAbandonmentController {

    private final OpenTableAbandonmentService abandonmentService;
    private final ModelMapper modelMapper;

    public OpenTableAbandonmentController(OpenTableAbandonmentService abandonmentService, ModelMapper modelMapper) {
        this.abandonmentService = abandonmentService;
        this.modelMapper = modelMapper;
    }

    // Register an abandoned open table creation flow for a user
    // POST /open-table-abandonments/user/{userId}
    @PostMapping("/user/{userId}")
    @ResponseStatus(HttpStatus.CREATED)
    public OpenTableAbandonmentBasicDTO createAbandonment(
            @PathVariable Long userId,
            @Valid @RequestBody OpenTableAbandonmentBasicDTO dto) {

        OpenTableAbandonmentModel abandonment = new OpenTableAbandonmentModel();
        abandonment.setStep(dto.step());
        abandonment.setActivityId(dto.activityId());
        abandonment.setDurationMinutes(dto.durationMinutes());
        abandonment.setMaxParticipants(dto.maxParticipants());
        abandonment.setBuildingId(dto.buildingId());

        OpenTableAbandonmentModel created = abandonmentService.create(userId, abandonment);

        return new OpenTableAbandonmentBasicDTO(
                created.getStep(),
                created.getActivityId(),
                created.getDurationMinutes(),
                created.getMaxParticipants(),
                created.getBuildingId());
    }

    // Count the abandonments per creation step since the given date
    // GET /open-table-abandonments/count-by-step?since=2026-09-01T00:00:00
    @GetMapping("/count-by-step")
    public Map<OpenTableCreationStepEnum, Long> countByStep(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime since) {
        return abandonmentService.countByStep(since);
    }

    // Get abandonments, users reached and abandonment rate per step since the given date
    // GET /open-table-abandonments/stats?since=2026-09-01T00:00:00
    @GetMapping("/stats")
    public List<OpenTableAbandonmentStatsBasicDTO> getAbandonmentStats(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime since) {
        return abandonmentService.calculateAbandonmentRateByStep(since);
    }

    // Get the step with the highest abandonment rate since the given date
    // GET /open-table-abandonments/stats/most-abandoned?since=2026-09-01T00:00:00
    @GetMapping("/stats/most-abandoned")
    public OpenTableAbandonmentStatsBasicDTO getMostAbandonedStep(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime since) {
        return abandonmentService.getMostAbandonedStep(since)
                .orElseThrow(() -> new NotFoundException("No hay abandonos registrados desde la fecha indicada"));
    }
}