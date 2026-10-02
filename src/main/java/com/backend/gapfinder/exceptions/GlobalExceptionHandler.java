package com.backend.gapfinder.exceptions;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    // Recurso no encontrado → 404
    @ExceptionHandler(NotFoundException.class)
    public ResponseEntity<String> handleNotFound(NotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ex.getMessage());
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

        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body("Error de integridad en BD: " + rootMessage);
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

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body("Error interno del servidor: " + rootCause.getMessage());
    }
}