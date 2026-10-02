package com.backend.gapfinder.services;
import com.backend.gapfinder.dto.GapCompleteDTO;
import com.backend.gapfinder.dto.responses.MatchCandidateResponseDTO;
import com.backend.gapfinder.enums.MatchStatusEnum;
import com.backend.gapfinder.exceptions.NotFoundException;
import com.backend.gapfinder.models.GapModel;
import com.backend.gapfinder.models.MatchModel;
import com.backend.gapfinder.repositories.GapRepository;
import com.backend.gapfinder.repositories.MatchRepository;

import lombok.extern.slf4j.Slf4j;

import org.modelmapper.ModelMapper;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;

@Slf4j
@Service
public class MatchService {

    private final MatchRepository matchRepository;
    private final GapRepository gapRepository;
    private final GapService gapService;
    private final ModelMapper modelMapper;

    public MatchService(MatchRepository matchRepository, GapRepository gapRepository, GapService gapService,
                         ModelMapper modelMapper) {
        this.matchRepository = matchRepository;
        this.gapRepository = gapRepository;
        this.gapService = gapService;
        this.modelMapper = modelMapper;
    }

    // Get a match by its id
    @Transactional
    public MatchModel getById(Long id) {
        log.info("Inicia proceso de consultar el match con id = {}", id);
        return matchRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("El match con id " + id + " no existe"));
    }

    // Get all matches
    @Transactional
    public List<MatchModel> getAll() {
        log.info("Inicia proceso de consultar todos los matches");
        return matchRepository.findAll();
    }

    // Create a new match
    @Transactional
    public MatchModel create(MatchModel match) {
        log.info("Inicia proceso de creación de un match");

        validateMatchData(match);

        GapModel proposerGap = gapService.getById(match.getProposerGap().getId());
        GapModel acceptorGap = gapService.getById(match.getAcceptorGap().getId());

        match.setId(null);
        match.setProposerGap(proposerGap);
        match.setAcceptorGap(acceptorGap);

        log.info("Termina proceso de creación de un match");
        return matchRepository.save(match);
    }

    // Update the data from an existing match
    @Transactional
    public MatchModel update(Long id, MatchModel match) {
        log.info("Inicia proceso de actualización del match con id = {}", id);

        MatchModel existente = getById(id);
        validateMatchData(match);

        GapModel proposerGap = gapService.getById(match.getProposerGap().getId());
        GapModel acceptorGap = gapService.getById(match.getAcceptorGap().getId());

        existente.setProposerGap(proposerGap);
        existente.setAcceptorGap(acceptorGap);
        existente.setStartTime(match.getStartTime());
        existente.setEndTime(match.getEndTime());
        existente.setScore(match.getScore());
        existente.setStatus(match.getStatus());

        log.info("Termina proceso de actualización del match con id = {}", id);
        return matchRepository.save(existente);
    }

    // Delete an existing match
    @Transactional
    public void delete(Long id) {
        log.info("Inicia proceso de eliminación del match con id = {}", id);

        MatchModel existente = getById(id);
        matchRepository.delete(existente);

        log.info("Termina proceso de eliminación del match con id = {}", id);
    }

    // Validate that the match data is correct
    private void validateMatchData(MatchModel match) {
        if (match.getProposerGap() == null || match.getProposerGap().getId() == null) {
            throw new IllegalArgumentException("Debe indicar el gap del proponente (proposerGap)");
        }

        if (match.getAcceptorGap() == null || match.getAcceptorGap().getId() == null) {
            throw new IllegalArgumentException("Debe indicar el gap del aceptante (acceptorGap)");
        }

        if (match.getStartTime() == null || match.getEndTime() == null) {
            throw new IllegalArgumentException("La hora de inicio y fin son obligatorias");
        }

        if (!match.getStartTime().isBefore(match.getEndTime())) {
            throw new IllegalArgumentException("La hora de inicio debe ser antes de la hora de fin");
        }

        if (match.getScore() == null) {
            throw new IllegalArgumentException("El score del match es obligatorio");
        }

        if (match.getStatus() == null) {
            throw new IllegalArgumentException("Debe indicar el estado del match (status)");
        }
    }


    // Find candidate gaps for a given gap, scored and sorted from best to worst
    @Transactional(readOnly = true)
    public List<MatchCandidateResponseDTO> findCandidates(Long gapId) {
        log.info("Inicia proceso de búsqueda de candidatos para el gap con id = {}", gapId);

        GapModel targetGap = gapService.getById(gapId);

        List<GapModel> candidates = gapRepository.findOverlappingGapsExcludingUser(
                targetGap.getUser().getId(), targetGap.getStartTime(), targetGap.getEndTime());

        List<MatchCandidateResponseDTO> result = candidates.stream()
                .map(candidateGap -> {
                    // The score is the number of minutes both gaps have in common (1 point per minute)
                    LocalDateTime overlapStart = targetGap.getStartTime().isAfter(candidateGap.getStartTime())
                            ? targetGap.getStartTime() : candidateGap.getStartTime();
                    LocalDateTime overlapEnd = targetGap.getEndTime().isBefore(candidateGap.getEndTime())
                            ? targetGap.getEndTime() : candidateGap.getEndTime();

                    double score = overlapStart.isBefore(overlapEnd)
                            ? Duration.between(overlapStart, overlapEnd).toMinutes()
                            : 0.0;

                    MatchCandidateResponseDTO dto = new MatchCandidateResponseDTO();
                    dto.setProposerGap(modelMapper.map(targetGap, GapCompleteDTO.class));
                    dto.setAcceptorGap(modelMapper.map(candidateGap, GapCompleteDTO.class));
                    dto.setScore(score);
                    return dto;
                })
                .filter(dto -> dto.getScore() > 0)
                .sorted(Comparator.comparingDouble(MatchCandidateResponseDTO::getScore).reversed())
                .toList();

        log.info("Termina proceso de búsqueda de candidatos para el gap con id = {}, encontrados = {}",
                gapId, result.size());
        return result;
    }

    // Send a match request between two gaps, using a score already calculated (e.g. from findCandidates)
    @Transactional
    public MatchModel sendMatchRequest(Long proposerGapId, Long acceptorGapId, Double score) {
        log.info("Inicia proceso de envío de solicitud de match entre gaps {} y {}", proposerGapId, acceptorGapId);

        if (proposerGapId.equals(acceptorGapId)) {
            throw new IllegalArgumentException("Un gap no puede hacer match consigo mismo");
        }

        GapModel proposerGap = gapService.getById(proposerGapId);
        GapModel acceptorGap = gapService.getById(acceptorGapId);

        // A user cannot match with their own gap, even if the gap id is different
        if (proposerGap.getUser().getId().equals(acceptorGap.getUser().getId())) {
            throw new IllegalArgumentException("Un usuario no puede hacer match consigo mismo");
        }

        // The actual meeting time is the overlap window between both gaps
        LocalDateTime meetingStart = proposerGap.getStartTime().isAfter(acceptorGap.getStartTime())
                ? proposerGap.getStartTime() : acceptorGap.getStartTime();
        LocalDateTime meetingEnd = proposerGap.getEndTime().isBefore(acceptorGap.getEndTime())
                ? proposerGap.getEndTime() : acceptorGap.getEndTime();

        if (!meetingStart.isBefore(meetingEnd)) {
            throw new IllegalArgumentException("Los gaps seleccionados no tienen un solape de tiempo válido");
        }

        MatchModel match = new MatchModel();
        match.setProposerGap(proposerGap);
        match.setAcceptorGap(acceptorGap);
        match.setStartTime(meetingStart);
        match.setEndTime(meetingEnd);
        match.setScore(score);
        match.setStatus(MatchStatusEnum.PENDING);

        MatchModel saved = matchRepository.save(match);

        log.info("Termina proceso de envío de solicitud de match con id = {}", saved.getId());
        return saved;
    }

    // Accept a pending match request
    @Transactional
    public MatchModel acceptMatch(Long matchId, Long userId) {
        log.info("Inicia proceso de aceptación del match con id = {} por el usuario = {}", matchId, userId);

        MatchModel match = getById(matchId);
        validateAcceptor(match, userId);
        validatePending(match);

        match.setStatus(MatchStatusEnum.ACCEPTED);
        MatchModel saved = matchRepository.save(match);

        log.info("Termina proceso de aceptación del match con id = {}", matchId);
        return saved;
    }

    // Reject a pending match request
    @Transactional
    public MatchModel rejectMatch(Long matchId, Long userId) {
        log.info("Inicia proceso de rechazo del match con id = {} por el usuario = {}", matchId, userId);

        MatchModel match = getById(matchId);
        validateAcceptor(match, userId);
        validatePending(match);

        match.setStatus(MatchStatusEnum.REJECTED);
        MatchModel saved = matchRepository.save(match);

        log.info("Termina proceso de rechazo del match con id = {}", matchId);
        return saved;
    }

    // Check that the user responding is the owner of the acceptor gap
    private void validateAcceptor(MatchModel match, Long userId) {
        if (!match.getAcceptorGap().getUser().getId().equals(userId)) {
            throw new IllegalArgumentException("Solo el usuario del gap receptor puede responder esta solicitud");
        }
    }

    // Mark an accepted match as completed once its meeting time has ended
    @Transactional
    public MatchModel completeMatch(Long matchId) {
        log.info("Inicia proceso de finalización del match con id = {}", matchId);

        MatchModel match = getById(matchId);

        if (match.getStatus() != MatchStatusEnum.ACCEPTED) {
            throw new IllegalArgumentException("Solo un match aceptado puede marcarse como completado");
        }

        if (LocalDateTime.now().isBefore(match.getEndTime())) {
            throw new IllegalArgumentException("El match aún no ha llegado a su hora de finalización");
        }

        match.setStatus(MatchStatusEnum.COMPLETED);
        MatchModel saved = matchRepository.save(match);

        log.info("Termina proceso de finalización del match con id = {}", matchId);
        return saved;
    }


    // Check that the match request is still pending before responding to it
    private void validatePending(MatchModel match) {
        if (match.getStatus() != MatchStatusEnum.PENDING) {
            throw new IllegalArgumentException("La solicitud de match ya fue respondida");
        }
    }

    // Runs automatically every 5 minutes to complete accepted matches whose meeting time already ended
    @Scheduled(fixedRate = 5 * 60 * 1000)
    @Transactional
    public void completeExpiredMatches() {
        log.info("Inicia proceso de finalización automática de matches vencidos");

        List<MatchModel> acceptedMatches = matchRepository.findByStatus(MatchStatusEnum.ACCEPTED);

        LocalDateTime now = LocalDateTime.now();
        acceptedMatches.stream()
                .filter(match -> !now.isBefore(match.getEndTime()))
                .forEach(match -> match.setStatus(MatchStatusEnum.COMPLETED));

        matchRepository.saveAll(acceptedMatches);

        log.info("Termina proceso de finalización automática de matches vencidos");
    }

    // Get the pending match requests received by a user
    @Transactional(readOnly = true)
    public List<MatchModel> getPendingReceived(Long userId) {
        log.info("Inicia proceso de consultar las solicitudes de match pendientes del usuario con id = {}", userId);
        return matchRepository.findByAcceptorGapUserIdAndStatus(userId, MatchStatusEnum.PENDING);
}
}