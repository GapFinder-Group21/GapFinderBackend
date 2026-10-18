package com.backend.gapfinder.controller.handlers;

import com.backend.gapfinder.model.exceptions.DuplicateClassBlockException;
import com.backend.gapfinder.model.exceptions.ForbiddenException;
import com.backend.gapfinder.model.exceptions.NotFoundException;
import com.backend.gapfinder.model.exceptions.ScheduleSourceUnavailableException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    // Recurso no encontrado → 404
    @ExceptionHandler(NotFoundException.class)
    public ResponseEntity<String> handleNotFound(NotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ex.getMessage());
    }

    // La clase ya existe en el horario del usuario → 409
    @ExceptionHandler(DuplicateClassBlockException.class)
    public ResponseEntity<String> handleDuplicateClassBlock(DuplicateClassBlockException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(ex.getMessage());
    }

    // Google Calendar no respondió o rechazó la conexión → 503
    @ExceptionHandler(ScheduleSourceUnavailableException.class)
    public ResponseEntity<String> handleScheduleSourceUnavailable(ScheduleSourceUnavailableException ex) {
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(ex.getMessage());
    }

    // La foto supera el tamaño máximo permitido → 413
    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<String> handleMaxUploadSize(MaxUploadSizeExceededException ex) {
        return ResponseEntity.status(HttpStatus.CONTENT_TOO_LARGE).body("La foto no puede pesar más de 5 MB");
    }

    // El usuario autenticado no es dueño del recurso → 403
    @ExceptionHandler(ForbiddenException.class)
    public ResponseEntity<String> handleForbidden(ForbiddenException ex) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(ex.getMessage());
    }

    // Campos inválidos en un body con @Valid → 400 con el error de cada campo
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, String>> handleValidation(MethodArgumentNotValidException ex) {
        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getFieldErrors()
                .forEach(err -> errors.put(err.getField(), err.getDefaultMessage()));
        return ResponseEntity.badRequest().body(errors);
    }

    // Datos inválidos en la petición → 400
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<String> handleBadRequest(IllegalArgumentException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ex.getMessage());
    }

    // Operación no permitida por estado del recurso → 409
    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<String> handleConflict(IllegalStateException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(ex.getMessage());
    }

    // CAPTURA ERRORES DE BASE DE DATOS (Campos null, duplicados, etc.) → 400
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<String> handleDataIntegrity(DataIntegrityViolationException ex) {
        // Obtenemos la causa raíz del error (el mensaje directo de la Base de Datos)
        Throwable rootCause = ex.getRootCause();
        String rootMessage = (rootCause != null) ? rootCause.getMessage() : ex.getMessage();

        // Lo imprimimos LIMPIO en la consola sin todo el stack trace largo
        System.err.println("\n=======================================================");
        System.err.println(">>> ERROR EN BASE DE DATOS: " + rootMessage);
        System.err.println("=======================================================\n");

        // The database detail stays in the console, the client only gets a generic message
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body("Los datos enviados no cumplen las restricciones de la base de datos");
    }

    // CAPTURA CUALQUIER OTRO ERROR INESPERADO → 500
    @ExceptionHandler(Exception.class)
    public ResponseEntity<String> handleGeneralException(Exception ex) {
        Throwable rootCause = ex;
        while (rootCause.getCause() != null) {
            rootCause = rootCause.getCause();
        }

        System.err.println("\n=======================================================");
        System.err.println(">>> EXCEPCIÓN NO CONTROLADA: " + rootCause.getMessage());
        System.err.println("=======================================================\n");

        // The internal detail stays in the console, the client only gets a generic message
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body("Error interno del servidor");
    }
}