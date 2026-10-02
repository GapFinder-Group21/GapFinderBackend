package com.backend.gapfinder.controllers;

import com.backend.gapfinder.dto.OpenTableBasicDTO;
import com.backend.gapfinder.dto.OpenTableCompleteDTO;
import com.backend.gapfinder.dto.OpenTableListDTO;
import com.backend.gapfinder.models.OpenTableModel;
import com.backend.gapfinder.services.OpenTableService;
import java.util.List;

import org.modelmapper.ModelMapper;
import org.modelmapper.TypeToken;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/open-tables")
public class OpenTableController {

    private final OpenTableService openTableService;
    private final ModelMapper modelMapper;

    public OpenTableController(OpenTableService openTableService, ModelMapper modelMapper) {
        this.openTableService = openTableService;
        this.modelMapper = modelMapper;
    }

    // Get an open table by its id
    // GET /open-tables/{id}
    @GetMapping("/{id}")
    public OpenTableListDTO getOpenTable(@PathVariable Long id) {
        OpenTableModel openTable = openTableService.getById(id);
        return modelMapper.map(openTable, OpenTableListDTO.class);
    }

    // Get all open tables
    // GET /open-tables
    @GetMapping
    public List<OpenTableListDTO> getAll() {
        List<OpenTableModel> openTables = openTableService.getAll();
        return modelMapper.map(openTables, new TypeToken<List<OpenTableListDTO>>() {}.getType());
    }


    // Create a new open table
    // POST /open-tables
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public OpenTableBasicDTO createOpenTable(@RequestBody OpenTableCompleteDTO dto) {
        OpenTableModel openTableModel = modelMapper.map(dto, OpenTableModel.class);
        OpenTableModel created = openTableService.create(openTableModel);
        return modelMapper.map(created, OpenTableBasicDTO.class);
    }

    // Update an existing open table
    // PUT /open-tables/{id}
    @PutMapping("/{id}")
    public OpenTableBasicDTO updateOpenTable(@PathVariable Long id, @RequestBody OpenTableCompleteDTO dto) {
        OpenTableModel openTableModel = modelMapper.map(dto, OpenTableModel.class);
        OpenTableModel updated = openTableService.update(id, openTableModel);
        return modelMapper.map(updated, OpenTableBasicDTO.class);
    }

    // Delete an open table
    // DELETE /open-tables/{id}
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteOpenTable(@PathVariable Long id) {
        openTableService.delete(id);
    }
}