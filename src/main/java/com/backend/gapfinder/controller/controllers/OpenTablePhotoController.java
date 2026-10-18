package com.backend.gapfinder.controller.controllers;

import com.backend.gapfinder.model.services.OpenTablePhotoService;

import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/uploads/open-tables")
public class OpenTablePhotoController {

    private final OpenTablePhotoService openTablePhotoService;

    public OpenTablePhotoController(OpenTablePhotoService openTablePhotoService) {
        this.openTablePhotoService = openTablePhotoService;
    }

    // Get the photo of an open table
    // GET /uploads/open-tables/{fileName}
    @GetMapping("/{fileName:.+}")
    public ResponseEntity<Resource> getPhoto(@PathVariable String fileName) {
        Resource photo = openTablePhotoService.load(fileName);
        MediaType type = fileName.endsWith(".png") ? MediaType.IMAGE_PNG : MediaType.IMAGE_JPEG;
        return ResponseEntity.ok().contentType(type).body(photo);
    }
}
