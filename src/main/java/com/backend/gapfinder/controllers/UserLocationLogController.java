package com.backend.gapfinder.controllers;

import com.backend.gapfinder.dto.UserLocationLogBasicDTO;
import com.backend.gapfinder.models.UserLocationLogModel;
import com.backend.gapfinder.services.UserLocationLogService;

import java.util.List;

import org.modelmapper.ModelMapper;
import org.modelmapper.TypeToken;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/user-location-logs")
public class UserLocationLogController {

    private final UserLocationLogService userLocationLogService;
    private final ModelMapper modelMapper;

    public UserLocationLogController(UserLocationLogService userLocationLogService, ModelMapper modelMapper) {
        this.userLocationLogService = userLocationLogService;
        this.modelMapper = modelMapper;
    }

    // Get a location log by its id
    // GET /user-location-logs/{id}
    @GetMapping("/{id}")
    public UserLocationLogBasicDTO getLocationLog(@PathVariable Long id) {
        UserLocationLogModel locationLog = userLocationLogService.getById(id);
        return modelMapper.map(locationLog, UserLocationLogBasicDTO.class);
    }

    // Get all location logs
    // GET /user-location-logs
    @GetMapping
    public List<UserLocationLogBasicDTO> getAll() {
        List<UserLocationLogModel> locationLogs = userLocationLogService.getAll();
        return modelMapper.map(locationLogs, new TypeToken<List<UserLocationLogBasicDTO>>() {}.getType());
    }

    // Create a new location log
    // POST /user-location-logs
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public UserLocationLogBasicDTO createLocationLog(@RequestBody UserLocationLogBasicDTO dto) {
        UserLocationLogModel locationLogModel = modelMapper.map(dto, UserLocationLogModel.class);
        UserLocationLogModel created = userLocationLogService.create(locationLogModel);
        return modelMapper.map(created, UserLocationLogBasicDTO.class);
    }

    // Update an existing location log
    // PUT /user-location-logs/{id}
    @PutMapping("/{id}")
    public UserLocationLogBasicDTO updateLocationLog(@PathVariable Long id,
                                                     @RequestBody UserLocationLogBasicDTO dto) {
        UserLocationLogModel locationLogModel = modelMapper.map(dto, UserLocationLogModel.class);
        UserLocationLogModel updated = userLocationLogService.update(id, locationLogModel);
        return modelMapper.map(updated, UserLocationLogBasicDTO.class);
    }

    // Delete a location log
    // DELETE /user-location-logs/{id}
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteLocationLog(@PathVariable Long id) {
        userLocationLogService.delete(id);
    }
}