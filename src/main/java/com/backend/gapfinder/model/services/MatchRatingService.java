package com.backend.gapfinder.model.services;

import com.backend.gapfinder.model.enums.MatchStatusEnum;
import com.backend.gapfinder.model.enums.NotificationTypeEnum;
import com.backend.gapfinder.model.events.MatchEvent;
import com.backend.gapfinder.model.exceptions.AlreadyDoneException;
import com.backend.gapfinder.model.exceptions.ForbiddenException;
import com.backend.gapfinder.model.exceptions.NotFoundException;
import com.backend.gapfinder.model.models.MatchModel;
import com.backend.gapfinder.model.models.MatchRatingModel;
import com.backend.gapfinder.model.models.UserModel;
import com.backend.gapfinder.model.repositories.MatchRatingRepository;
import com.backend.gapfinder.model.repositories.MatchRepository;
import com.backend.gapfinder.view.dto.MatchHistoryItemDTO;
import com.backend.gapfinder.view.dto.MatchRatingDTO;
import com.backend.gapfinder.view.dto.UserBasicDTO;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

// "Mis matches": history of the matches of a user and the ratings of the finished ones
@Slf4j
@Service
public class MatchRatingService {

    private final MatchRatingRepository ratingRepository;
    private final MatchRepository matchRepository;
    private final ApplicationEventPublisher eventPublisher;

    public MatchRatingService(MatchRatingRepository ratingRepository,
                              MatchRepository matchRepository,
                              ApplicationEventPublisher eventPublisher) {
        this.ratingRepository = ratingRepository;
        this.matchRepository = matchRepository;
        this.eventPublisher = eventPublisher;
    }

    // All the matches of the user, newest first, with the ratings of each one
    @Transactional(readOnly = true)
    public List<MatchHistoryItemDTO> getHistory(Long userId) {
        log.info("Inicia proceso de consultar el historial de matches del usuario {}", userId);

        List<MatchModel> matches = matchRepository.findByProposerGapUserIdOrAcceptorGapUserId(userId, userId)
                .stream()
                .sorted(Comparator.comparing(MatchModel::getStartTime,
                        Comparator.nullsLast(Comparator.reverseOrder())))
                .toList();
        if (matches.isEmpty()) {
            return List.of();
        }

        // One query for the ratings of all the matches
        List<Long> ids = matches.stream().map(MatchModel::getId).toList();
        Map<Long, List<MatchRatingModel>> ratingsByMatch = ratingRepository.findByMatchIdIn(ids).stream()
                .collect(Collectors.groupingBy(r -> r.getMatch().getId()));

        return matches.stream()
                .map(m -> toItem(m, userId, ratingsByMatch.getOrDefault(m.getId(), List.of())))
                .toList();
    }

    // The user rates a finished match. Returns the match as it is seen in the history after rating
    @Transactional
    public MatchHistoryItemDTO rate(Long matchId, Long raterId, Integer stars, String comment) {
        log.info("Inicia proceso de calificar el match {} por el usuario {}", matchId, raterId);

        MatchModel match = matchRepository.findById(matchId)
                .orElseThrow(() -> new NotFoundException("El match con id " + matchId + " no existe"));

        UserModel proposer = match.getProposerGap().getUser();
        UserModel acceptor = match.getAcceptorGap().getUser();
        boolean isProposer = proposer.getId().equals(raterId);
        if (!isProposer && !acceptor.getId().equals(raterId)) {
            throw new ForbiddenException("Solo los participantes del match pueden calificarlo");
        }

        String cleanComment = validate(stars, comment);

        // Business rule → 422
        if (match.getStatus() != MatchStatusEnum.COMPLETED) {
            throw new IllegalStateException("Solo puedes calificar matches que ya terminaron");
        }
        // Already rated → 409 (the app outbox counts it as done)
        if (ratingRepository.findByMatchIdAndRaterId(matchId, raterId).isPresent()) {
            throw new AlreadyDoneException("Ya calificaste este match");
        }

        UserModel rater = isProposer ? proposer : acceptor;
        UserModel rated = isProposer ? acceptor : proposer;

        MatchRatingModel rating = new MatchRatingModel();
        rating.setMatch(match);
        rating.setRater(rater);
        rating.setRated(rated);
        rating.setStars(stars);
        rating.setComment(cleanComment);
        rating.setCreatedAt(LocalDateTime.now());

        try {
            ratingRepository.saveAndFlush(rating);
        } catch (DataIntegrityViolationException ex) {
            // Two retries at the same time: the unique (match_id, rater_id) kept only one
            throw new AlreadyDoneException("Ya calificaste este match");
        }

        eventPublisher.publishEvent(new MatchEvent(
                NotificationTypeEnum.MATCH_RATED,
                rated.getId(),
                rater.getName(),
                match.getId()));

        log.info("Termina proceso de calificar el match {}", matchId);
        List<MatchRatingModel> ratings = ratingRepository.findByMatchIdIn(List.of(matchId));
        return toItem(match, raterId, ratings);
    }

    // Stars from 1 to 5 and comment up to 300 → 400. Returns the comment without spaces (null if empty)
    private static String validate(Integer stars, String comment) {
        if (stars == null || stars < MatchRatingModel.MIN_STARS || stars > MatchRatingModel.MAX_STARS) {
            throw new IllegalArgumentException("La calificación debe ser de "
                    + MatchRatingModel.MIN_STARS + " a " + MatchRatingModel.MAX_STARS + " estrellas");
        }
        if (comment == null || comment.isBlank()) {
            return null;
        }
        String trimmed = comment.trim();
        if (trimmed.length() > MatchRatingModel.MAX_COMMENT_LENGTH) {
            throw new IllegalArgumentException("El comentario no puede tener más de "
                    + MatchRatingModel.MAX_COMMENT_LENGTH + " caracteres");
        }
        return trimmed;
    }

    private static MatchHistoryItemDTO toItem(MatchModel match, Long userId, List<MatchRatingModel> ratings) {
        UserModel proposer = match.getProposerGap().getUser();
        UserModel other = proposer.getId().equals(userId) ? match.getAcceptorGap().getUser() : proposer;

        MatchRatingModel mine = null;
        MatchRatingModel theirs = null;
        for (MatchRatingModel r : ratings) {
            if (r.getRater().getId().equals(userId)) {
                mine = r;
            } else {
                theirs = r;
            }
        }

        MatchHistoryItemDTO dto = new MatchHistoryItemDTO();
        dto.setMatchId(match.getId());
        dto.setStatus(match.getStatus());
        dto.setStartTime(match.getStartTime());
        dto.setEndTime(match.getEndTime());
        dto.setScore(match.getScore());
        dto.setOtherUser(toUser(other));
        dto.setMyRating(MatchRatingDTO.from(mine));
        // The other rating is only shown after I rate, so it does not influence mine
        dto.setOtherRating(mine == null ? null : MatchRatingDTO.from(theirs));
        dto.setCanRate(match.getStatus() == MatchStatusEnum.COMPLETED && mine == null);
        return dto;
    }

    private static UserBasicDTO toUser(UserModel user) {
        UserBasicDTO dto = new UserBasicDTO();
        dto.setId(user.getId());
        dto.setName(user.getName());
        dto.setEmail(user.getEmail());
        dto.setPhoneNumber(user.getPhoneNumber());
        dto.setCareer(user.getCareer());
        dto.setSemester(user.getSemester());
        dto.setAvatarUrl(user.getAvatarUrl());
        dto.setVerified(user.isVerified());
        dto.setPreferredEffort(user.getPreferredEffort());
        return dto;
    }
}
