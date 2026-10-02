package com.backend.gapfinder.controllers;

import com.backend.gapfinder.dto.InterestBasicDTO;
import com.backend.gapfinder.models.InterestModel;
import com.backend.gapfinder.services.InterestService;

import java.util.List;

import org.modelmapper.ModelMapper;
import org.modelmapper.TypeToken;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/interests")
public class InterestController {

    private final InterestService interestService;
    private final ModelMapper modelMapper;

    public InterestController(InterestService interestService, ModelMapper modelMapper) {
        this.interestService = interestService;
        this.modelMapper = modelMapper;
    }

    // Get an interest by its id
    // GET /interests/{id}
    @GetMapping("/{id}")
    public InterestBasicDTO getInterest(@PathVariable Long id) {
        InterestModel interest = interestService.getById(id);
        return modelMapper.map(interest, InterestBasicDTO.class);
    }

    // Get all interests
    // GET /interests
    @GetMapping
    public List<InterestBasicDTO> getAll() {
        List<InterestModel> interests = interestService.getAll();
        return modelMapper.map(interests, new TypeToken<List<InterestBasicDTO>>() {}.getType());
    }

    // Create a new interest
    // POST /interests
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public InterestBasicDTO createInterest(@RequestBody InterestBasicDTO dto) {
        InterestModel interestModel = modelMapper.map(dto, InterestModel.class);
        InterestModel created = interestService.create(interestModel);
        return modelMapper.map(created, InterestBasicDTO.class);
    }

    // Update an existing interest
    // PUT /interests/{id}
    @PutMapping("/{id}")
    public InterestBasicDTO updateInterest(@PathVariable Long id, @RequestBody InterestBasicDTO dto) {
        InterestModel interestModel = modelMapper.map(dto, InterestModel.class);
        InterestModel updated = interestService.update(id, interestModel);
        return modelMapper.map(updated, InterestBasicDTO.class);
    }

    // Delete an interest
    // DELETE /interests/{id}
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteInterest(@PathVariable Long id) {
        interestService.delete(id);
    }
}