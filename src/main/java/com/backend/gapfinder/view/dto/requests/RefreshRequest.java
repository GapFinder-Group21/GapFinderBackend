package com.backend.gapfinder.view.dto.requests;

import jakarta.validation.constraints.NotBlank;

// Body que se envía para pedir un access token nuevo usando el refresh token
public record RefreshRequest(
    @NotBlank(message = "El refresh token es obligatorio")
    String refreshToken
) {}