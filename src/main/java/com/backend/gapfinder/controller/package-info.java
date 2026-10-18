/**
 * Capa Controller del patrón MVC.
 * Contiene los @RestController que reciben las peticiones HTTP, delegan el trabajo
 * a los servicios del Model y devuelven las respuestas de la View (DTOs).
 * En handlers/ están los @RestControllerAdvice que traducen las excepciones
 * del Model a respuestas HTTP (códigos de estado y mensajes).
 */
package com.backend.gapfinder.controller;
