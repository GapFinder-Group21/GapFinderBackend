package com.backend.gapfinder.services;

import com.backend.gapfinder.exceptions.NotFoundException;
import com.backend.gapfinder.models.ActivityModel;
import com.backend.gapfinder.models.InterestModel;
import com.backend.gapfinder.repositories.ActivityRepository;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
public class ActivityService {

    private final ActivityRepository activityRepository;
    private final InterestService interestService;

    public ActivityService(ActivityRepository activityRepository, InterestService interestService) {
        this.activityRepository = activityRepository;
        this.interestService = interestService;
    }

    // Get an activity by its id
    @Transactional
    public ActivityModel getById(Long id) {
        log.info("Inicia proceso de consultar la actividad con id = {}", id);
        return activityRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("La actividad con id " + id + " no existe"));
    }

    // Get all activities
    @Transactional
    public List<ActivityModel> getAll() {
        log.info("Inicia proceso de consultar todas las actividades");
        return activityRepository.findAll();
    }

    // Create a new activity
    @Transactional
    public ActivityModel create(ActivityModel activity) {
        log.info("Inicia proceso de creación de una actividad con nombre = {}", activity.getName());

        validateActivityData(activity);

        InterestModel interest = interestService.getById(activity.getInterest().getId());

        activity.setId(null);
        activity.setInterest(interest);

        log.info("Termina proceso de creación de una actividad con nombre = {}", activity.getName());
        return activityRepository.save(activity);
    }

    // Update the data from an existing activity
    @Transactional
    public ActivityModel update(Long id, ActivityModel activity) {
        log.info("Inicia proceso de actualización de la actividad con id = {}", id);

        ActivityModel existente = getById(id);
        validateActivityData(activity);

        InterestModel interest = interestService.getById(activity.getInterest().getId());

        existente.setName(activity.getName());
        existente.setInterest(interest);
        existente.setDurationMinutes(activity.getDurationMinutes());
        existente.setEffortType(activity.getEffortType());

        log.info("Termina proceso de actualización de la actividad con id = {}", id);
        return activityRepository.save(existente);
    }

    // Delete an existing activity
    @Transactional
    public void delete(Long id) {
        log.info("Inicia proceso de eliminación de la actividad con id = {}", id);

        ActivityModel existente = getById(id);
        activityRepository.delete(existente);

        log.info("Termina proceso de eliminación de la actividad con id = {}", id);
    }

    // Validate that the activity data is correct
    private void validateActivityData(ActivityModel activity) {
        if (activity.getName() == null || activity.getName().isBlank()) {
            throw new IllegalArgumentException("El nombre de la actividad es obligatorio");
        }

        if (activity.getInterest() == null || activity.getInterest().getId() == null) {
            throw new IllegalArgumentException("Debe indicar el interés asociado");
        }

        if (activity.getDurationMinutes() == null || activity.getDurationMinutes() <= 0) {
            throw new IllegalArgumentException("La duración debe ser un número positivo");
        }

        if (activity.getEffortType() == null) {
            throw new IllegalArgumentException("Debe indicar el tipo de esfuerzo (effortType)");
        }
    }
}