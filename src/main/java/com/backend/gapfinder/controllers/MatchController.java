package com.backend.gapfinder.controllers;

import com.backend.gapfinder.dto.MatchBasicDTO;
import com.backend.gapfinder.dto.MatchCompleteDTO;
import com.backend.gapfinder.dto.responses.MatchCandidateResponseDTO;
import com.backend.gapfinder.models.MatchModel;
import com.backend.gapfinder.services.MatchService;

import java.util.List;

import org.modelmapper.ModelMapper;
import org.modelmapper.TypeToken;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/matches")
public class MatchController {

    private final MatchService matchService;
    private final ModelMapper modelMapper;

    public MatchController(MatchService matchService, ModelMapper modelMapper) {
        this.matchService = matchService;
        this.modelMapper = modelMapper;
    }

    // Get a match by its id
    // GET /matches/{id}
    @GetMapping("/{id}")
    public MatchCompleteDTO getMatch(@PathVariable Long id) {
        MatchModel match = matchService.getById(id);
        return modelMapper.map(match, MatchCompleteDTO.class);
    }

    // Get all matches
    // GET /matches
    @GetMapping
    public List<MatchCompleteDTO> getAll() {
        List<MatchModel> matches = matchService.getAll();
        return modelMapper.map(matches, new TypeToken<List<MatchCompleteDTO>>() {}.getType());
    }

    // Find scored candidate gaps for a given gap, sorted from best to worst
    // GET /matches/gap/{gapId}/candidates
    @GetMapping("/gap/{gapId}/candidates")
    public List<MatchCandidateResponseDTO> findCandidates(@PathVariable Long gapId) {
        return matchService.findCandidates(gapId);
    }
    
    // Send a match request between two gaps using an already calculated score
    // POST /matches/request?proposerGapId=10&acceptorGapId=25&score=87.5
    @PostMapping("/request")
    @ResponseStatus(HttpStatus.CREATED)
    public MatchBasicDTO sendMatchRequest(
            @RequestParam Long proposerGapId,
            @RequestParam Long acceptorGapId,
            @RequestParam Double score) {

        MatchModel match = matchService.sendMatchRequest(proposerGapId, acceptorGapId, score);
        return modelMapper.map(match, MatchBasicDTO.class);
    }

    // Accept a pending match request
    // PATCH /matches/57/accept?userId=400
    @PatchMapping("/{id}/accept")
    public MatchCompleteDTO acceptMatch(
            @PathVariable Long id,
            @RequestParam Long userId) {

        MatchModel match = matchService.acceptMatch(id, userId);
        return modelMapper.map(match, MatchCompleteDTO.class);
    }

    // Reject a pending match request
    // PATCH /matches/57/reject?userId=400
    @PatchMapping("/{id}/reject")
    public MatchCompleteDTO rejectMatch(
            @PathVariable Long id,
            @RequestParam Long userId) {

        MatchModel match = matchService.rejectMatch(id, userId);
        return modelMapper.map(match, MatchCompleteDTO.class);
    }

    // Mark an accepted match as completed once its meeting time has ended
    // PATCH /matches/57/complete
    @PatchMapping("/{id}/complete")
    public MatchBasicDTO completeMatch(@PathVariable Long id) {
        MatchModel match = matchService.completeMatch(id);
        return modelMapper.map(match, MatchBasicDTO.class);
    }

    // Create a new match
    // POST /matches
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public MatchBasicDTO createMatch(@RequestBody MatchBasicDTO dto) {
        MatchModel matchModel = modelMapper.map(dto, MatchModel.class);
        MatchModel created = matchService.create(matchModel);
        return modelMapper.map(created, MatchBasicDTO.class);
    }

    // Update an existing match
    // PUT /matches/{id}
    @PutMapping("/{id}")
    public MatchBasicDTO updateMatch(@PathVariable Long id, @RequestBody MatchBasicDTO dto) {
        MatchModel matchModel = modelMapper.map(dto, MatchModel.class);
        MatchModel updated = matchService.update(id, matchModel);
        return modelMapper.map(updated, MatchBasicDTO.class);
    }

    // Delete a match
    // DELETE /matches/{id}
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteMatch(@PathVariable Long id) {
        matchService.delete(id);
    }

    // Get the pending match requests received by a user
    // GET /matches/user/{userId}/pending
    @GetMapping("/user/{userId}/pending")
    public List<MatchCompleteDTO> getPendingReceived(@PathVariable Long userId) {
        List<MatchModel> matches = matchService.getPendingReceived(userId);
        return modelMapper.map(matches, new TypeToken<List<MatchCompleteDTO>>() {}.getType());
}
}