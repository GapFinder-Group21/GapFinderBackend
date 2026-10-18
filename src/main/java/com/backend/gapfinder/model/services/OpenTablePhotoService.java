package com.backend.gapfinder.model.services;

import com.backend.gapfinder.model.exceptions.NotFoundException;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Pattern;

// Stores and reads the photos of the open tables on disk
@Slf4j
@Service
public class OpenTablePhotoService {

    // Public path where the photos are served (see OpenTablePhotoController)
    public static final String URL_PREFIX = "/uploads/open-tables/";

    // Accepted image types and the file extension used for each one
    private static final Map<String, String> EXTENSIONS = Map.of(
            "image/jpeg", "jpg",
            "image/png", "png");

    // Stored file names are a UUID plus the extension
    private static final Pattern FILE_NAME = Pattern.compile("^[0-9a-f-]{36}\\.(jpg|png)$");

    private final Path directory;

    public OpenTablePhotoService(@Value("${uploads.dir:uploads}") String uploadsDir) {
        this.directory = Path.of(uploadsDir, "open-tables").toAbsolutePath().normalize();
    }

    // Validates the photo, saves it with a random name and returns its relative URL
    public String store(MultipartFile photo) {
        if (photo == null || photo.isEmpty()) {
            throw new IllegalArgumentException("La foto de la open table es obligatoria");
        }

        String extension = EXTENSIONS.get(photo.getContentType());
        if (extension == null) {
            throw new IllegalArgumentException("La foto debe ser una imagen JPG o PNG");
        }

        // The content must really be an image, not only have an image type
        try (InputStream input = photo.getInputStream()) {
            if (ImageIO.read(input) == null) {
                throw new IllegalArgumentException("El archivo enviado no es una imagen válida");
            }
        } catch (IOException e) {
            throw new IllegalArgumentException("No se pudo leer la foto enviada");
        }

        String fileName = UUID.randomUUID() + "." + extension;

        try {
            Files.createDirectories(directory);
            photo.transferTo(directory.resolve(fileName));
        } catch (IOException e) {
            throw new IllegalStateException("No se pudo guardar la foto de la open table", e);
        }

        log.info("Foto de open table guardada: {}", fileName);
        return URL_PREFIX + fileName;
    }

    // Gets a stored photo by its file name
    public Resource load(String fileName) {
        // Only names created by store() are read, so no other file of the server can be reached
        if (!FILE_NAME.matcher(fileName).matches()) {
            throw new NotFoundException("La foto no existe");
        }

        Path file = directory.resolve(fileName);
        if (!Files.exists(file)) {
            throw new NotFoundException("La foto no existe");
        }

        try {
            return new UrlResource(file.toUri());
        } catch (MalformedURLException e) {
            throw new NotFoundException("La foto no existe");
        }
    }

    // Removes a stored photo by its relative URL (used when the table could not be saved)
    public void delete(String url) {
        if (url == null || !url.startsWith(URL_PREFIX)) {
            return;
        }

        String fileName = url.substring(URL_PREFIX.length());
        if (!FILE_NAME.matcher(fileName).matches()) {
            return;
        }

        try {
            Files.deleteIfExists(directory.resolve(fileName));
        } catch (IOException e) {
            log.warn("No se pudo borrar la foto {}: {}", fileName, e.getMessage());
        }
    }
}
