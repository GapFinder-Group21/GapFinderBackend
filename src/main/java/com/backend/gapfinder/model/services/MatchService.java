package com.backend.gapfinder.model.services;

import com.backend.gapfinder.view.dto.GapBasicDTO;
import com.backend.gapfinder.view.dto.GapCompleteDTO;
import com.backend.gapfinder.view.dto.responses.MatchCandidateResponseDTO;
import com.backend.gapfinder.model.enums.MatchStatusEnum;
import com.backend.gapfinder.model.enums.NotificationTypeEnum;
import com.backend.gapfinder.model.events.MatchEvent;
import com.backend.gapfinder.model.exceptions.ForbiddenException;
import com.backend.gapfinder.model.exceptions.AlreadyDoneException;
import com.backend.gapfinder.model.exceptions.NotFoundException;
import com.backend.gapfinder.model.models.GapModel;
import com.backend.gapfinder.model.models.MatchModel;
import com.backend.gapfinder.model.repositories.GapRepository;
import com.backend.gapfinder.model.repositories.MatchRepository;
import com.backend.gapfinder.model.strategies.EffortStrategy;
import com.backend.gapfinder.model.strategies.OverlapStrategy;
import com.backend.gapfinder.model.strategies.SameCareerStrategy;
import com.backend.gapfinder.model.strategies.SharedInterestStrategy;

import lombok.extern.slf4j.Slf4j;

import org.modelmapper.ModelMapper;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Slf4j
@Service
public class MatchService {

    // A gap with a match in one of these states already has a meeting booked
    private static final List<MatchStatusEnum> BOOKED_STATUSES =
            List.of(MatchStatusEnum.ACCEPTED, MatchStatusEnum.COMPLETED);

    // A request in one of these states between two gaps is still alive
    private static final List<MatchStatusEnum> OPEN_REQUEST_STATUSES =
            List.of(MatchStatusEnum.PENDING, MatchStatusEnum.ACCEPTED);

    private final MatchRepository matchRepository;
    private final GapRepository gapRepository;
    private final GapService gapService;
    private final OverlapStrategy overlapStrategy;
    private final SameCareerStrategy sameCareerStrategy;
    private final SharedInterestStrategy sharedInterestStrategy;
    private final EffortStrategy effortStrategy;
    private final ModelMapper modelMapper;
    private final ApplicationEventPublisher eventPublisher;

    public MatchService(MatchRepository matchRepository, GapRepository gapRepository, GapService gapService,
                         OverlapStrategy overlapStrategy, SameCareerStrategy sameCareerStrategy,
                         SharedInterestStrategy sharedInterestStrategy, EffortStrategy effortStrategy,
                         ModelMapper modelMapper, ApplicationEventPublisher eventPublisher) {
        this.matchRepository = matchRepository;
        this.gapRepository = gapRepository;
        this.gapService = gapService;
        this.overlapStrategy = overlapStrategy;
        this.sameCareerStrategy = sameCareerStrategy;
        this.sharedInterestStrategy = sharedInterestStrategy;
        this.effortStrategy = effortStrategy;
        this.modelMapper = modelMapper;
        this.eventPublisher = eventPublisher;
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

    // Get the matches of a user, as proposer or as acceptor
    @Transactional(readOnly = true)
    public List<MatchModel> getByUser(Long userId) {
        log.info("Inicia proceso de consultar los matches del usuario con id = {}", userId);
        return matchRepository.findByProposerGapUserIdOrAcceptorGapUserId(userId, userId);
    }

    // Create a new match
    // A new match is a match request: it starts PENDING and the meeting time is the overlap of both gaps
    @Transactional
    public MatchModel create(MatchModel match) {
        log.info("Inicia proceso de creación de un match");

        validateMatchData(match);

        MatchModel saved = sendMatchRequest(
                match.getProposerGap().getId(), match.getAcceptorGap().getId(), match.getScore());

        log.info("Termina proceso de creación de un match");
        return saved;
    }

    // Update the data from an existing match
    // Only a pending request can be edited; its status changes through accept, reject and complete
    @Transactional
    public MatchModel update(Long id, MatchModel match) {
        log.info("Inicia proceso de actualización del match con id = {}", id);

        MatchModel existente = getById(id);
        validateMatchData(match);
        validatePending(existente);

        GapModel proposerGap = gapService.getById(match.getProposerGap().getId());
        GapModel acceptorGap = gapService.getById(match.getAcceptorGap().getId());

        validateDifferentUsers(proposerGap, acceptorGap);
        validateScore(proposerGap, acceptorGap, match.getScore());

        LocalDateTime[] meeting = calculateMeetingWindow(proposerGap, acceptorGap);

        existente.setProposerGap(proposerGap);
        existente.setAcceptorGap(acceptorGap);
        existente.setStartTime(meeting[0]);
        existente.setEndTime(meeting[1]);
        existente.setScore(match.getScore());

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

        if (match.getScore() == null) {
            throw new IllegalArgumentException("El score del match es obligatorio");
        }
    }


    // Find candidate gaps for a given gap, scored and sorted from best to worst
    // Only gaps that have not ended and are free (no accepted or completed match) are candidates
    @Transactional(readOnly = true)
    public List<MatchCandidateResponseDTO> findCandidates(Long gapId, boolean useSameCareer,
                                                            boolean useSharedInterests, boolean useEffort) {
        log.info("Inicia proceso de búsqueda de candidatos para el gap con id = {}", gapId);

        GapModel targetGap = gapService.getById(gapId);

        // A gap that already has a meeting booked does not look for more matches
        if (matchRepository.existsByGapAndStatusIn(targetGap.getId(), BOOKED_STATUSES)) {
            log.info("El gap con id = {} ya tiene un match aceptado, no se buscan candidatos", gapId);
            return List.of();
        }

        List<GapModel> candidates = gapRepository.findOverlappingGapsExcludingUser(
                targetGap.getUser().getId(), targetGap.getStartTime(), targetGap.getEndTime(),
                LocalDateTime.now());

        List<MatchCandidateResponseDTO> result = candidates.stream()
                .map(candidateGap -> {
                    double score = overlapStrategy.calculateScore(targetGap, candidateGap);

                    if (useSameCareer) {
                        score += sameCareerStrategy.calculateScore(targetGap, candidateGap);
                    }
                    if (useSharedInterests) {
                        score += sharedInterestStrategy.calculateScore(targetGap, candidateGap);
                    }
                    if (useEffort) {
                        score += effortStrategy.calculateScore(targetGap, candidateGap);
                    }

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
    // and notify the owner of the acceptor gap
    @Transactional
    public MatchModel sendMatchRequest(Long proposerGapId, Long acceptorGapId, Double score) {
        log.info("Inicia proceso de envío de solicitud de match entre gaps {} y {}", proposerGapId, acceptorGapId);

        if (proposerGapId.equals(acceptorGapId)) {
            throw new IllegalArgumentException("Un gap no puede hacer match consigo mismo");
        }

        GapModel proposerGap = gapService.getById(proposerGapId);
        GapModel acceptorGap = gapService.getById(acceptorGapId);

        validateDifferentUsers(proposerGap, acceptorGap);

        // The actual meeting time is the overlap window between both gaps
        LocalDateTime[] meeting = calculateMeetingWindow(proposerGap, acceptorGap);

        // A meeting that already ended cannot be proposed
        if (!meeting[1].isAfter(LocalDateTime.now())) {
            throw new IllegalStateException("El tiempo en común entre los gaps ya pasó");
        }

        // A gap can only have one meeting booked
        validateGapsAreFree(proposerGap, acceptorGap);

        // Only one live request between the same two gaps
        if (matchRepository.existsBetweenGapsAndStatusIn(proposerGapId, acceptorGapId, OPEN_REQUEST_STATUSES)) {
            throw new AlreadyDoneException("Ya existe una solicitud de match entre estos gaps");
        }

        validateScore(proposerGap, acceptorGap, score);

        MatchModel match = new MatchModel();
        match.setProposerGap(proposerGap);
        match.setAcceptorGap(acceptorGap);
        match.setStartTime(meeting[0]);
        match.setEndTime(meeting[1]);
        match.setScore(score);
        match.setStatus(MatchStatusEnum.PENDING);

        MatchModel saved = matchRepository.save(match);

        eventPublisher.publishEvent(new MatchEvent(
                NotificationTypeEnum.MATCH_PROPOSED,
                acceptorGap.getUser().getId(),
                proposerGap.getUser().getName(),
                saved.getId()));

        log.info("Termina proceso de envío de solicitud de match con id = {}", saved.getId());
        return saved;
    }

    // Accept a pending match request and notify the proposer
    @Transactional
    public MatchModel acceptMatch(Long matchId, Long userId) {
        log.info("Inicia proceso de aceptación del match con id = {} por el usuario = {}", matchId, userId);

        MatchModel match = getById(matchId);
        validateAcceptor(match, userId);
        validatePending(match);

        // A request whose meeting time already passed cannot be accepted
        if (!match.getEndTime().isAfter(LocalDateTime.now())) {
            throw new IllegalStateException("La hora del match ya pasó, no es posible aceptarlo");
        }

        // A gap can only have one meeting booked
        validateGapsAreFree(match.getProposerGap(), match.getAcceptorGap());

        match.setStatus(MatchStatusEnum.ACCEPTED);
        MatchModel saved = matchRepository.save(match);

        eventPublisher.publishEvent(new MatchEvent(
                NotificationTypeEnum.MATCH_ACCEPTED,
                saved.getProposerGap().getUser().getId(),
                saved.getAcceptorGap().getUser().getName(),
                saved.getId()));

        log.info("Termina proceso de aceptación del match con id = {}", matchId);
        return saved;
    }

    // Reject a pending match request and notify the proposer
    @Transactional
    public MatchModel rejectMatch(Long matchId, Long userId) {
        log.info("Inicia proceso de rechazo del match con id = {} por el usuario = {}", matchId, userId);

        MatchModel match = getById(matchId);
        validateAcceptor(match, userId);
        validatePending(match);

        match.setStatus(MatchStatusEnum.REJECTED);
        MatchModel saved = matchRepository.save(match);

        eventPublisher.publishEvent(new MatchEvent(
                NotificationTypeEnum.MATCH_REJECTED,
                saved.getProposerGap().getUser().getId(),
                saved.getAcceptorGap().getUser().getName(),
                saved.getId()));

        log.info("Termina proceso de rechazo del match con id = {}", matchId);
        return saved;
    }

    // The proposer cancels their own request while nobody has answered it
    // There is no CANCELLED status, so the request ends as REJECTED (the receiver is not notified)
    @Transactional
    public MatchModel cancelMatch(Long matchId, Long userId) {
        log.info("Inicia proceso de cancelación del match con id = {} por el usuario = {}", matchId, userId);

        MatchModel match = getById(matchId);

        if (!match.getProposerGap().getUser().getId().equals(userId)) {
            throw new ForbiddenException("Solo quien envió la solicitud puede cancelarla");
        }
        if (match.getStatus() != MatchStatusEnum.PENDING) {
            throw new IllegalStateException("Solo se puede cancelar una solicitud que sigue pendiente");
        }

        match.setStatus(MatchStatusEnum.REJECTED);
        MatchModel saved = matchRepository.save(match);

        log.info("Termina proceso de cancelación del match con id = {}", matchId);
        return saved;
    }

    // Check that the user responding is the owner of the acceptor gap
    private void validateAcceptor(MatchModel match, Long userId) {
        if (!match.getAcceptorGap().getUser().getId().equals(userId)) {
            throw new ForbiddenException("Solo el usuario del gap receptor puede responder esta solicitud");
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

    // Both gaps must belong to different users
    private void validateDifferentUsers(GapModel proposerGap, GapModel acceptorGap) {
        if (proposerGap.getUser().getId().equals(acceptorGap.getUser().getId())) {
            throw new IllegalArgumentException("Un usuario no puede hacer match consigo mismo");
        }
    }

    // Neither gap can already have an accepted or completed match
    private void validateGapsAreFree(GapModel proposerGap, GapModel acceptorGap) {
        if (matchRepository.existsByGapAndStatusIn(proposerGap.getId(), BOOKED_STATUSES)
                || matchRepository.existsByGapAndStatusIn(acceptorGap.getId(), BOOKED_STATUSES)) {
            throw new IllegalStateException("Uno de los gaps ya tiene un match aceptado en ese horario");
        }
    }

    // The meeting time is the overlap of both gaps: [later start, earlier end]
    private LocalDateTime[] calculateMeetingWindow(GapModel proposerGap, GapModel acceptorGap) {
        LocalDateTime meetingStart = proposerGap.getStartTime().isAfter(acceptorGap.getStartTime())
                ? proposerGap.getStartTime() : acceptorGap.getStartTime();
        LocalDateTime meetingEnd = proposerGap.getEndTime().isBefore(acceptorGap.getEndTime())
                ? proposerGap.getEndTime() : acceptorGap.getEndTime();

        if (!meetingStart.isBefore(meetingEnd)) {
            throw new IllegalArgumentException("Los gaps seleccionados no tienen un solape de tiempo válido");
        }

        return new LocalDateTime[] {meetingStart, meetingEnd};
    }

    // The score comes from findCandidates, so it can never be higher than all the strategies together
    private void validateScore(GapModel proposerGap, GapModel acceptorGap, Double score) {
        if (score == null || score < 0) {
            throw new IllegalArgumentException("El score del match debe ser un número positivo");
        }

        double maxScore = overlapStrategy.calculateScore(proposerGap, acceptorGap)
                + sameCareerStrategy.calculateScore(proposerGap, acceptorGap)
                + sharedInterestStrategy.calculateScore(proposerGap, acceptorGap)
                + effortStrategy.calculateScore(proposerGap, acceptorGap);

        if (score > maxScore) {
            throw new IllegalArgumentException("El score no puede ser mayor al máximo posible para estos gaps ("
                    + maxScore + ")");
        }
    }

    // Runs automatically every 5 minutes to close the matches whose meeting time already ended:
    // accepted ones become COMPLETED and pending ones that nobody answered become REJECTED
    @Scheduled(fixedRate = 5 * 60 * 1000)
    @Transactional
    public void completeExpiredMatches() {
        LocalDateTime now = LocalDateTime.now();
        List<MatchModel> changed = new ArrayList<>();

        matchRepository.findByStatus(MatchStatusEnum.ACCEPTED).stream()
                .filter(match -> !now.isBefore(match.getEndTime()))
                .forEach(match -> {
                    match.setStatus(MatchStatusEnum.COMPLETED);
                    changed.add(match);
                });

        matchRepository.findByStatus(MatchStatusEnum.PENDING).stream()
                .filter(match -> !now.isBefore(match.getEndTime()))
                .forEach(match -> {
                    match.setStatus(MatchStatusEnum.REJECTED);
                    changed.add(match);
                });

        // Runs often, so it only saves and logs when something changed
        if (changed.isEmpty()) {
            return;
        }

        matchRepository.saveAll(changed);

        log.info("Finalización automática de matches vencidos: {} actualizados", changed.size());
    }

    // Get the pending match requests received by a user
    @Transactional(readOnly = true)
    public List<MatchModel> getPendingReceived(Long userId) {
        log.info("Inicia proceso de consultar las solicitudes de match pendientes del usuario con id = {}", userId);
        return matchRepository.findByAcceptorGapUserIdAndStatus(userId, MatchStatusEnum.PENDING);
}
}
