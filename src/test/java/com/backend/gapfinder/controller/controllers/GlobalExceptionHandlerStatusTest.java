package com.backend.gapfinder.controller.controllers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.backend.gapfinder.controller.handlers.GlobalExceptionHandler;
import com.backend.gapfinder.model.exceptions.AlreadyDoneException;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

// The app reads these codes: 409 = "already done" (not an error), 422 = refused by a rule, 400 = bad parameter
class GlobalExceptionHandlerStatusTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void alreadyDoneAnswers409() {
        ResponseEntity<String> res = handler.handleAlreadyDone(new AlreadyDoneException("El usuario ya está registrado"));

        assertEquals(HttpStatus.CONFLICT, res.getStatusCode());
        assertEquals("El usuario ya está registrado", res.getBody());
    }

    @Test
    void aBusinessRuleRefusalAnswers422WithItsMessage() {
        ResponseEntity<String> res = handler.handleBusinessRule(new IllegalStateException("La open table está llena"));

        assertEquals(HttpStatus.UNPROCESSABLE_ENTITY, res.getStatusCode());
        assertEquals("La open table está llena", res.getBody());
    }

    @Test
    void aWrongParameterFormatAnswers400NotAn500() {
        MethodArgumentTypeMismatchException ex = mock(MethodArgumentTypeMismatchException.class);
        when(ex.getName()).thenReturn("since");

        ResponseEntity<String> res = handler.handleTypeMismatch(ex);

        assertEquals(HttpStatus.BAD_REQUEST, res.getStatusCode());
        assertTrue(res.getBody().contains("since"));
    }

    @Test
    void aMissingParameterAnswers400() {
        ResponseEntity<String> res = handler.handleMissingParameter(
                new MissingServletRequestParameterException("userId", "Long"));

        assertEquals(HttpStatus.BAD_REQUEST, res.getStatusCode());
        assertTrue(res.getBody().contains("userId"));
    }
}
