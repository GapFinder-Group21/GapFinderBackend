package com.backend.gapfinder.controller.controllers;

import com.backend.gapfinder.view.dto.OpenTableBasicDTO;
import com.backend.gapfinder.view.dto.OpenTableCompleteDTO;
import com.backend.gapfinder.view.dto.OpenTableListDTO;
import com.backend.gapfinder.model.models.OpenTableModel;
import com.backend.gapfinder.model.models.UserModel;
import com.backend.gapfinder.model.services.OpenTableService;
import com.backend.gapfinder.security.AccessGuard;
import java.util.List;

import org.modelmapper.ModelMapper;
import org.modelmapper.TypeToken;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

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
    // POST /open-tables (multipart: "openTable" with the JSON data and "photo" with the image)
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    public OpenTableBasicDTO createOpenTable(@RequestPart("openTable") OpenTableCompleteDTO dto,
                                             // Optional here so the service answers with a clear message when it is missing
                                             @RequestPart(value = "photo", required = false) MultipartFile photo,
                                             @AuthenticationPrincipal UserModel me) {
        OpenTableModel openTableModel = modelMapper.map(dto, OpenTableModel.class);
        // A user can only create open tables in their own name
        if (openTableModel.getCreator() != null && openTableModel.getCreator().getId() != null) {
            AccessGuard.requireSameUser(me, openTableModel.getCreator().getId());
        }
        OpenTableModel created = openTableService.create(openTableModel, photo);
        return modelMapper.map(created, OpenTableBasicDTO.class);
    }

    // Update an existing open table
    // PUT /open-tables/{id}
    @PutMapping("/{id}")
    public OpenTableBasicDTO updateOpenTable(@PathVariable Long id, @RequestBody OpenTableCompleteDTO dto,
                                             @AuthenticationPrincipal UserModel me) {
        // Only the creator can edit the open table
        AccessGuard.requireSameUser(me, openTableService.getById(id).getCreator().getId());
        OpenTableModel openTableModel = modelMapper.map(dto, OpenTableModel.class);
        OpenTableModel updated = openTableService.update(id, openTableModel);
        return modelMapper.map(updated, OpenTableBasicDTO.class);
    }

    // Delete an open table
    // DELETE /open-tables/{id}
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteOpenTable(@PathVariable Long id, @AuthenticationPrincipal UserModel me) {
        // Only the creator can delete the open table
        AccessGuard.requireSameUser(me, openTableService.getById(id).getCreator().getId());
        openTableService.delete(id);
    }
}