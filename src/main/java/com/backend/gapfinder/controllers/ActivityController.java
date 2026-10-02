package com.backend.gapfinder.controllers;

import com.backend.gapfinder.dto.ActivityBasicDTO;
import com.backend.gapfinder.models.ActivityModel;
import com.backend.gapfinder.services.ActivityService;

import java.util.List;

import org.modelmapper.ModelMapper;
import org.modelmapper.TypeToken;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/activities")
public class ActivityController {

    private final ActivityService activityService;
    private final ModelMapper modelMapper;

    public ActivityController(ActivityService activityService, ModelMapper modelMapper) {
        this.activityService = activityService;
        this.modelMapper = modelMapper;
    }

    // Get an activity by its id
    // GET /activities/{id}
    @GetMapping("/{id}")
    public ActivityBasicDTO getActivity(@PathVariable Long id) {
        ActivityModel activity = activityService.getById(id);
        return modelMapper.map(activity, ActivityBasicDTO.class);
    }

    // Get all activities
    // GET /activities
    @GetMapping
    public List<ActivityBasicDTO> getAll() {
        List<ActivityModel> activities = activityService.getAll();
        return modelMapper.map(activities, new TypeToken<List<ActivityBasicDTO>>() {}.getType());
    }

    // Create a new activity
    // POST /activities
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ActivityBasicDTO createActivity(@RequestBody ActivityBasicDTO dto) {
        ActivityModel activityModel = modelMapper.map(dto, ActivityModel.class);
        ActivityModel created = activityService.create(activityModel);
        return modelMapper.map(created, ActivityBasicDTO.class);
    }

    // Update an existing activity
    // PUT /activities/{id}
    @PutMapping("/{id}")
    public ActivityBasicDTO updateActivity(@PathVariable Long id, @RequestBody ActivityBasicDTO dto) {
        ActivityModel activityModel = modelMapper.map(dto, ActivityModel.class);
        ActivityModel updated = activityService.update(id, activityModel);
        return modelMapper.map(updated, ActivityBasicDTO.class);
    }

    // Delete an activity
    // DELETE /activities/{id}
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteActivity(@PathVariable Long id) {
        activityService.delete(id);
    }
}