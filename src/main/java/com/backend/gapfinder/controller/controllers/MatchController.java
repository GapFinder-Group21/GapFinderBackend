package com.backend.gapfinder.controller.controllers;

import com.backend.gapfinder.view.dto.MatchBasicDTO;
import com.backend.gapfinder.view.dto.MatchCompleteDTO;
import com.backend.gapfinder.view.dto.responses.MatchCandidateResponseDTO;
import com.backend.gapfinder.model.models.MatchModel;
import com.backend.gapfinder.model.models.UserModel;
import com.backend.gapfinder.model.services.GapService;
import com.backend.gapfinder.model.services.MatchService;
import com.backend.gapfinder.security.AccessGuard;

import java.util.List;

import org.modelmapper.ModelMapper;
import org.modelmapper.TypeToken;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/matches")
public class MatchController {

    private final MatchService matchService;
    private final GapService gapService;
    private final ModelMapper modelMapper;

    public MatchController(MatchService matchService, GapService gapService, ModelMapper modelMapper) {
        this.matchService = matchService;
        this.gapService = gapService;
        this.modelMapper = modelMapper;
    }

    // Get a match by its id; only the two users of the match can see it
    // GET /matches/{id}
    @GetMapping("/{id}")
    public MatchCompleteDTO getMatch(@PathVariable Long id, @AuthenticationPrincipal UserModel me) {
        MatchModel match = matchService.getById(id);
        requireParticipant(match, me);
        return modelMapper.map(match, MatchCompleteDTO.class);
    }

    // Get the matches of the authenticated user (matches of other users carry their email and phone)
    // GET /matches
    @GetMapping
    public List<MatchCompleteDTO> getAll(@AuthenticationPrincipal UserModel me) {
        return getByUser(me.getId(), me);
    }

    // Get the matches of a user, as proposer or as acceptor
    // GET /matches/user/{userId}
    @GetMapping("/user/{userId}")
    public List<MatchCompleteDTO> getByUser(@PathVariable Long userId, @AuthenticationPrincipal UserModel me) {
        AccessGuard.requireSameUser(me, userId);
        List<MatchModel> matches = matchService.getByUser(userId);
        return modelMapper.map(matches, new TypeToken<List<MatchCompleteDTO>>() {}.getType());
    }

    // Find scored candidate gaps for a given gap, sorted from best to worst
    // GET /matches/gap/{gapId}/candidates?useSameCareer=true&useSharedInterests=true&useEffort=false
    @GetMapping("/gap/{gapId}/candidates")
    public List<MatchCandidateResponseDTO> findCandidates(
            @PathVariable Long gapId,
            @RequestParam(defaultValue = "false") boolean useSameCareer,
            @RequestParam(defaultValue = "false") boolean useSharedInterests,
            @RequestParam(defaultValue = "false") boolean useEffort,
            @AuthenticationPrincipal UserModel me) {

        // Only the owner of the gap can look for candidates for it
        AccessGuard.requireSameUser(me, gapService.getById(gapId).getUser().getId());
        return matchService.findCandidates(gapId, useSameCareer, useSharedInterests, useEffort);
    }

    // Send a match request between two gaps using an already calculated score
    // POST /matches/request?proposerGapId=10&acceptorGapId=25&score=87.5
    @PostMapping("/request")
    @ResponseStatus(HttpStatus.CREATED)
    public MatchBasicDTO sendMatchRequest(
            @RequestParam Long proposerGapId,
            @RequestParam Long acceptorGapId,
            @RequestParam Double score,
            @AuthenticationPrincipal UserModel me) {

        // A user can only propose matches from their own gap
        AccessGuard.requireSameUser(me, gapService.getById(proposerGapId).getUser().getId());
        MatchModel match = matchService.sendMatchRequest(proposerGapId, acceptorGapId, score);
        return modelMapper.map(match, MatchBasicDTO.class);
    }

    // Accept a pending match request
    // PATCH /matches/57/accept?userId=400
    @PatchMapping("/{id}/accept")
    public MatchCompleteDTO acceptMatch(
            @PathVariable Long id,
            @RequestParam Long userId,
            @AuthenticationPrincipal UserModel me) {

        // A user can only answer requests in their own name
        AccessGuard.requireSameUser(me, userId);

        MatchModel match = matchService.acceptMatch(id, userId);
        return modelMapper.map(match, MatchCompleteDTO.class);
    }

    // Reject a pending match request
    // PATCH /matches/57/reject?userId=400
    @PatchMapping("/{id}/reject")
    public MatchCompleteDTO rejectMatch(
            @PathVariable Long id,
            @RequestParam Long userId,
            @AuthenticationPrincipal UserModel me) {

        // A user can only answer requests in their own name
        AccessGuard.requireSameUser(me, userId);

        MatchModel match = matchService.rejectMatch(id, userId);
        return modelMapper.map(match, MatchCompleteDTO.class);
    }

    // Cancel a request the user sent while it is still pending (it ends as REJECTED)
    // POST /matches/57/cancel
    @PostMapping("/{id}/cancel")
    public MatchCompleteDTO cancelMatch(@PathVariable Long id, @AuthenticationPrincipal UserModel me) {
        MatchModel match = matchService.cancelMatch(id, me.getId());
        return modelMapper.map(match, MatchCompleteDTO.class);
    }

    // Mark an accepted match as completed once its meeting time has ended
    // PATCH /matches/57/complete
    @PatchMapping("/{id}/complete")
    public MatchBasicDTO completeMatch(@PathVariable Long id, @AuthenticationPrincipal UserModel me) {
        requireParticipant(matchService.getById(id), me);
        MatchModel match = matchService.completeMatch(id);
        return modelMapper.map(match, MatchBasicDTO.class);
    }

    // Create a new match
    // POST /matches
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public MatchBasicDTO createMatch(@RequestBody MatchBasicDTO dto, @AuthenticationPrincipal UserModel me) {
        // A user can only propose matches from their own gap
        if (dto.getProposerGapId() != null) {
            AccessGuard.requireSameUser(me, gapService.getById(dto.getProposerGapId()).getUser().getId());
        }
        MatchModel matchModel = modelMapper.map(dto, MatchModel.class);
        MatchModel created = matchService.create(matchModel);
        return modelMapper.map(created, MatchBasicDTO.class);
    }

    // Update an existing match
    // PUT /matches/{id}
    @PutMapping("/{id}")
    public MatchBasicDTO updateMatch(@PathVariable Long id, @RequestBody MatchBasicDTO dto,
                                     @AuthenticationPrincipal UserModel me) {
        requireParticipant(matchService.getById(id), me);
        MatchModel matchModel = modelMapper.map(dto, MatchModel.class);
        MatchModel updated = matchService.update(id, matchModel);
        return modelMapper.map(updated, MatchBasicDTO.class);
    }

    // Delete a match
    // DELETE /matches/{id}
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteMatch(@PathVariable Long id, @AuthenticationPrincipal UserModel me) {
        requireParticipant(matchService.getById(id), me);
        matchService.delete(id);
    }

    // Get the pending match requests received by a user
    // GET /matches/user/{userId}/pending
    @GetMapping("/user/{userId}/pending")
    public List<MatchCompleteDTO> getPendingReceived(@PathVariable Long userId,
                                                     @AuthenticationPrincipal UserModel me) {
        // Only the user can see the requests they received
        AccessGuard.requireSameUser(me, userId);
        List<MatchModel> matches = matchService.getPendingReceived(userId);
        return modelMapper.map(matches, new TypeToken<List<MatchCompleteDTO>>() {}.getType());
}

    // Only the two users of the match (proposer or receiver) can change it
    private void requireParticipant(MatchModel match, UserModel me) {
        Long proposerId = match.getProposerGap().getUser().getId();
        if (me == null || !me.getId().equals(proposerId)) {
            AccessGuard.requireSameUser(me, match.getAcceptorGap().getUser().getId());
        }
    }
}