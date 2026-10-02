package com.backend.gapfinder.controllers;

import com.backend.gapfinder.models.GoogleCredentialModel;
import com.backend.gapfinder.models.UserModel;
import com.backend.gapfinder.repositories.GoogleCredentialRepository;
import com.backend.gapfinder.services.UserService;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
import com.google.api.client.googleapis.auth.oauth2.GoogleTokenResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/google")
public class GoogleAuthController {

    private final GoogleAuthorizationCodeFlow flow;
    private final GoogleCredentialRepository googleCredentialRepository;
    private final UserService userService;

    @Value("${google.redirect-uri}")
    private String redirectUri;

    public GoogleAuthController(GoogleAuthorizationCodeFlow flow,
                                GoogleCredentialRepository googleCredentialRepository,
                                UserService userService) {
        this.flow = flow;
        this.googleCredentialRepository = googleCredentialRepository;
        this.userService = userService;
    }

    @GetMapping("/auth-url")
    public Map<String, String> getAuthUrl(@AuthenticationPrincipal UserModel user) {
        String url = flow.newAuthorizationUrl()
                .setRedirectUri(redirectUri)
                .setState(String.valueOf(user.getId()))
                .build();

        return Map.of("url", url);
    }

    @GetMapping("/callback")
    public String callback(@RequestParam("code") String code,
                           @RequestParam("state") String state) throws Exception {
        Long userId = Long.parseLong(state);

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
