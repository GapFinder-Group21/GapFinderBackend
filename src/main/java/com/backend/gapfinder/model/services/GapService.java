package com.backend.gapfinder.model.services;

import com.backend.gapfinder.model.exceptions.NotFoundException;
import com.backend.gapfinder.model.models.GapModel;
import com.backend.gapfinder.model.models.UserModel;
import com.backend.gapfinder.model.repositories.GapRepository;
import com.backend.gapfinder.model.repositories.MatchRepository;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
public class GapService {

    private final GapRepository gapRepository;
    private final MatchRepository matchRepository;
    private final UserService userService;

    public GapService(GapRepository gapRepository, MatchRepository matchRepository, UserService userService) {
        this.gapRepository = gapRepository;
        this.matchRepository = matchRepository;
        this.userService = userService;
    }

    // Get a gap by its id
    @Transactional
    public GapModel getById(Long id) {
        log.info("Inicia proceso de consultar el gap con id = {}", id);
        return gapRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("El gap con id " + id + " no existe"));
    }

    // Get all gaps
    @Transactional
    public List<GapModel> getAll() {
        log.info("Inicia proceso de consultar todos los gaps");
        return gapRepository.findAll();
    }

    // Create a new gap
    @Transactional
    public GapModel create(GapModel gap) {
        log.info("Inicia proceso de creación de un gap");

        validateGapData(gap);

        UserModel user = userService.getById(gap.getUser().getId());

        // A user cannot be free twice at the same time
        validateNoOverlap(user.getId(), gap, 0L);

        gap.setId(null);
        gap.setUser(user);

        log.info("Termina proceso de creación de un gap");
        return gapRepository.save(gap);
    }

    // Update the data from an existing gap
    @Transactional
    public GapModel update(Long id, GapModel gap) {
        log.info("Inicia proceso de actualización del gap con id = {}", id);

        GapModel existente = getById(id);
        validateGapData(gap);

        // A gap used by a match keeps its times, so the meeting stays inside the gap
        if (matchRepository.existsByProposerGapIdOrAcceptorGapId(id, id)) {
            throw new IllegalStateException("El gap ya tiene matches, no es posible modificarlo");
        }

        // The owner of the gap does not change; the new times cannot overlap their other gaps
        validateNoOverlap(existente.getUser().getId(), gap, id);

        existente.setStartTime(gap.getStartTime());
        existente.setEndTime(gap.getEndTime());

        log.info("Termina proceso de actualización del gap con id = {}", id);
        return gapRepository.save(existente);
    }

    // Delete an existing gap
    @Transactional
    public void delete(Long id) {
        log.info("Inicia proceso de eliminación del gap con id = {}", id);

        GapModel existente = getById(id);

        // The matches of a gap point to it, so a gap with matches is kept as history
        if (matchRepository.existsByProposerGapIdOrAcceptorGapId(id, id)) {
            throw new IllegalStateException("El gap ya tiene matches, no es posible eliminarlo");
        }

        gapRepository.delete(existente);

        log.info("Termina proceso de eliminación del gap con id = {}", id);
    }

    // Validate that the gap data is correct
    private void validateGapData(GapModel gap) {
        if (gap.getUser() == null || gap.getUser().getId() == null) {
            throw new IllegalArgumentException("Debe indicar el usuario dueño del gap");
        }

        if (gap.getStartTime() == null || gap.getEndTime() == null) {
            throw new IllegalArgumentException("La hora de inicio y fin son obligatorias");
        }

        if (!gap.getStartTime().isBefore(gap.getEndTime())) {
            throw new IllegalArgumentException("La hora de inicio debe ser antes de la hora de fin");
        }
    }

    // The gap cannot overlap another gap of the same user (excludedId is the gap being edited, 0 if new)
    private void validateNoOverlap(Long userId, GapModel gap, Long excludedId) {
        if (gapRepository.existsOverlappingGapForUser(userId, gap.getStartTime(), gap.getEndTime(), excludedId)) {
            throw new IllegalStateException("El usuario ya tiene un gap en ese horario");
        }
    }
}