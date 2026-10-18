package com.backend.gapfinder.controller.controllers;

import com.backend.gapfinder.model.models.GoogleCredentialModel;
import com.backend.gapfinder.model.models.UserModel;
import com.backend.gapfinder.model.repositories.GoogleCredentialRepository;
import com.backend.gapfinder.model.services.UserService;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
import com.google.api.client.googleapis.auth.oauth2.GoogleTokenResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@RestController
@RequestMapping("/google")
public class GoogleAuthController {

    private final GoogleAuthorizationCodeFlow flow;
    private final GoogleCredentialRepository googleCredentialRepository;
    private final UserService userService;

    // How long the Google connection link stays valid
    private static final Duration STATE_TTL = Duration.ofMinutes(10);

    @Value("${google.redirect-uri}")
    private String redirectUri;

    // Random OAuth "state" -> user who asked for the link; the callback only trusts states created here
    private final Map<String, PendingState> pendingStates = new ConcurrentHashMap<>();

    private record PendingState(Long userId, Instant expiresAt) {}

    public GoogleAuthController(GoogleAuthorizationCodeFlow flow,
                                GoogleCredentialRepository googleCredentialRepository,
                                UserService userService) {
        this.flow = flow;
        this.googleCredentialRepository = googleCredentialRepository;
        this.userService = userService;
    }

    @GetMapping("/auth-url")
    public Map<String, String> getAuthUrl(@AuthenticationPrincipal UserModel user) {
        // Old links that were never used are discarded
        Instant now = Instant.now();
        pendingStates.values().removeIf(pending -> pending.expiresAt().isBefore(now));

        String state = UUID.randomUUID().toString();
        pendingStates.put(state, new PendingState(user.getId(), now.plus(STATE_TTL)));

        String url = flow.newAuthorizationUrl()
                .setRedirectUri(redirectUri)
                .setState(state)
                .build();

        return Map.of("url", url);
    }

    @GetMapping("/callback")
    public String callback(@RequestParam("code") String code,
                           @RequestParam("state") String state) throws Exception {
        // The state is single use and identifies the user who started the connection
        PendingState pending = pendingStates.remove(state);
        if (pending == null || pending.expiresAt().isBefore(Instant.now())) {
            throw new IllegalArgumentException("El enlace para conectar Google no es válido o expiró, genera uno nuevo");
        }
        Long userId = pending.userId();

        GoogleTokenResponse tokenResponse = flow.newTokenRequest(code)
                .setRedirectUri(redirectUri)
                .execute();

        UserModel user = userService.getById(userId);

        GoogleCredentialModel credential = googleCredentialRepository.findByUserId(userId)
                .orElse(new GoogleCredentialModel());

        credential.setUser(user);
        credential.setAccessToken(tokenResponse.getAccessToken());
        if (tokenResponse.getRefreshToken() != null) {
            credential.setRefreshToken(tokenResponse.getRefreshToken());
        }
        credential.setExpirationTime(Instant.now().plusSeconds(tokenResponse.getExpiresInSeconds()));

        googleCredentialRepository.save(credential);

        log.info("Credenciales de Google guardadas para el usuario {}", userId);

        return "Cuenta de Google conectada correctamente. Ya puedes cerrar esta ventana.";
    }
}
