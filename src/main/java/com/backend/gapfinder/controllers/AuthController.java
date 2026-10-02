package com.backend.gapfinder.controllers;

import com.backend.gapfinder.dto.requests.LoginRequest;
import com.backend.gapfinder.dto.requests.RefreshRequest;
import com.backend.gapfinder.dto.requests.RegisterRequest;
import com.backend.gapfinder.dto.responses.AuthResponse;
import com.backend.gapfinder.models.UserModel;
import com.backend.gapfinder.services.AuthService;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    // Registra un usuario nuevo
    // POST /api/auth/register
    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(authService.register(request));
    }

    // Inicia sesión
    // POST /api/auth/login
    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(authService.login(request));
    }

    // Genera un access token nuevo usando el refresh token
    // POST /api/auth/refresh
    @PostMapping("/refresh")
    public ResponseEntity<AuthResponse> refresh(@Valid @RequestBody RefreshRequest request) {
        return ResponseEntity.ok(authService.refresh(request));
    }

    // Cierra sesión (revoca el refresh token del usuario)
    // POST /api/auth/logout
    @PostMapping("/logout")
    public ResponseEntity<Void> logout(@AuthenticationPrincipal UserModel user) {
        authService.logout(user);
        return ResponseEntity.noContent().build();
    }
}