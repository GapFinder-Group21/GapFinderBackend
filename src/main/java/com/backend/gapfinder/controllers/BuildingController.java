package com.backend.gapfinder.controllers;

import com.backend.gapfinder.dto.BuildingBasicDTO;
import com.backend.gapfinder.exceptions.NotFoundException;
import com.backend.gapfinder.models.BuildingModel;
import com.backend.gapfinder.services.BuildingService;

import java.util.List;

import org.modelmapper.ModelMapper;
import org.modelmapper.TypeToken;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/buildings")
public class BuildingController {

    private final BuildingService buildingService;
    private final ModelMapper modelMapper;

    public BuildingController(BuildingService buildingService, ModelMapper modelMapper) {
        this.buildingService = buildingService;
        this.modelMapper = modelMapper;
    }

    // Get a building by its id
    // GET /buildings/{id}
    @GetMapping("/{id}")
    public BuildingBasicDTO getBuilding(@PathVariable Long id) {
        BuildingModel building = buildingService.getById(id);
        return modelMapper.map(building, BuildingBasicDTO.class);
    }

    // Get all buildings
    // GET /buildings
    @GetMapping
    public List<BuildingBasicDTO> getAll() {
        List<BuildingModel> buildings = buildingService.getAll();
        return modelMapper.map(buildings, new TypeToken<List<BuildingBasicDTO>>() {}.getType());
    }

    // Create a new building
    // POST /buildings
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public BuildingBasicDTO createBuilding(@RequestBody BuildingBasicDTO dto) {
        BuildingModel buildingModel = modelMapper.map(dto, BuildingModel.class);
        BuildingModel created = buildingService.create(buildingModel);
        return modelMapper.map(created, BuildingBasicDTO.class);
    }

    // Update an existing building
    // PUT /buildings/{id}
    @PutMapping("/{id}")
    public BuildingBasicDTO updateBuilding(@PathVariable Long id, @RequestBody BuildingBasicDTO dto) {
        BuildingModel buildingModel = modelMapper.map(dto, BuildingModel.class);
        BuildingModel updated = buildingService.update(id, buildingModel);
        return modelMapper.map(updated, BuildingBasicDTO.class);
    }

    // Delete a building
    // DELETE /buildings/{id}
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteBuilding(@PathVariable Long id) {
        buildingService.delete(id);
    }

    // Find the building that contains the given GPS coordinates
    // GET /buildings/locate?latitude=4.6015&longitude=-74.0661
    @GetMapping("/locate")
    public BuildingBasicDTO locateBuilding(
            @RequestParam double latitude,
            @RequestParam double longitude) {

        BuildingModel building = buildingService.findBuildingContainingUser(latitude, longitude)
                .orElseThrow(() -> new NotFoundException("No hay ningún edificio en las coordenadas indicadas"));
        return modelMapper.map(building, BuildingBasicDTO.class);
    }
    
}