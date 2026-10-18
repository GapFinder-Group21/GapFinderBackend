package com.backend.gapfinder.controller.controllers;

import com.backend.gapfinder.view.dto.OpenTableParticipantBasicDTO;
import com.backend.gapfinder.view.dto.OpenTableParticipantCompleteDTO;
import com.backend.gapfinder.model.models.OpenTableParticipantModel;
import com.backend.gapfinder.model.models.UserModel;
import com.backend.gapfinder.model.services.OpenTableParticipantService;
import com.backend.gapfinder.security.AccessGuard;

import java.util.List;

import org.modelmapper.ModelMapper;
import org.modelmapper.TypeToken;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/open-table-participants")
public class OpenTableParticipantController {

    private final OpenTableParticipantService openTableParticipantService;
    private final ModelMapper modelMapper;

    public OpenTableParticipantController(OpenTableParticipantService openTableParticipantService,
                                          ModelMapper modelMapper) {
        this.openTableParticipantService = openTableParticipantService;
        this.modelMapper = modelMapper;
    }

    // Get a participant record by its id
    // GET /open-table-participants/{id}
    @GetMapping("/{id}")
    public OpenTableParticipantBasicDTO getParticipant(@PathVariable Long id) {
        OpenTableParticipantModel participant = openTableParticipantService.getById(id);
        return modelMapper.map(participant, OpenTableParticipantBasicDTO.class);
    }

    // Get all participant records
    // GET /open-table-participants
    @GetMapping
    public List<OpenTableParticipantBasicDTO> getAll() {
        List<OpenTableParticipantModel> participants = openTableParticipantService.getAll();
        return modelMapper.map(participants, new TypeToken<List<OpenTableParticipantBasicDTO>>() {}.getType());
    }

    // Create a new participant record
    // POST /open-table-participants
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public OpenTableParticipantBasicDTO createParticipant(@RequestBody OpenTableParticipantBasicDTO dto,
                                                          @AuthenticationPrincipal UserModel me) {
        OpenTableParticipantModel participantModel = modelMapper.map(dto, OpenTableParticipantModel.class);
        // A user can only sign themselves up
        if (participantModel.getUser() != null && participantModel.getUser().getId() != null) {
            AccessGuard.requireSameUser(me, participantModel.getUser().getId());
        }
        OpenTableParticipantModel created = openTableParticipantService.create(participantModel);
        return modelMapper.map(created, OpenTableParticipantBasicDTO.class);
    }

    // Update an existing participant record
    // PUT /open-table-participants/{id}
    @PutMapping("/{id}")
    public OpenTableParticipantBasicDTO updateParticipant(@PathVariable Long id,
                                                          @RequestBody OpenTableParticipantBasicDTO dto,
                                                          @AuthenticationPrincipal UserModel me) {
        // Only the participant can change their own record
        AccessGuard.requireSameUser(me, openTableParticipantService.getById(id).getUser().getId());
        OpenTableParticipantModel participantModel = modelMapper.map(dto, OpenTableParticipantModel.class);
        OpenTableParticipantModel updated = openTableParticipantService.update(id, participantModel);
        return modelMapper.map(updated, OpenTableParticipantBasicDTO.class);
    }

    // Delete a participant record
    // DELETE /open-table-participants/{id}
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteParticipant(@PathVariable Long id, @AuthenticationPrincipal UserModel me) {
        // The participant or the creator of the table can remove the record
        OpenTableParticipantModel participant = openTableParticipantService.getById(id);
        Long creatorId = participant.getOpenTable().getCreator().getId();
        if (me == null || !me.getId().equals(creatorId)) {
            AccessGuard.requireSameUser(me, participant.getUser().getId());
        }
        openTableParticipantService.delete(id);
    }

    // Get the participants of an open table
    // GET /open-table-participants/table/{tableId}
    @GetMapping("/table/{tableId}")
    public List<OpenTableParticipantCompleteDTO> getByOpenTable(@PathVariable Long tableId) {
        List<OpenTableParticipantModel> participants = openTableParticipantService.getByOpenTable(tableId);
        return modelMapper.map(participants, new TypeToken<List<OpenTableParticipantCompleteDTO>>() {}.getType());
    }

    // A user joins an open table
    // POST /open-table-participants/table/{tableId}/join?userId=1
    @PostMapping("/table/{tableId}/join")
    @ResponseStatus(HttpStatus.CREATED)
    public OpenTableParticipantCompleteDTO join(@PathVariable Long tableId, @RequestParam Long userId,
                                                @AuthenticationPrincipal UserModel me) {
        // A user can only join a table in their own name
        AccessGuard.requireSameUser(me, userId);
        OpenTableParticipantModel joined = openTableParticipantService.join(tableId, userId);
        return modelMapper.map(joined, OpenTableParticipantCompleteDTO.class);
    }

    // A user leaves an open table
    // DELETE /open-table-participants/table/{tableId}/leave?userId=1
    @DeleteMapping("/table/{tableId}/leave")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void leave(@PathVariable Long tableId, @RequestParam Long userId,
                      @AuthenticationPrincipal UserModel me) {
        // A user can only leave a table in their own name
        AccessGuard.requireSameUser(me, userId);
        openTableParticipantService.leave(tableId, userId);
    }

    
}