package com.backend.gapfinder.controller.controllers;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

import com.backend.gapfinder.model.models.UserModel;
import com.backend.gapfinder.model.repositories.GoogleCredentialRepository;
import com.backend.gapfinder.model.services.UserService;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;

import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.Map;

class GoogleAuthControllerTest {

    private static final String PUBLIC_REDIRECT = "https://gapfinder.example.org/google/callback";

    private GoogleAuthController controllerWithRedirect(String redirectUri) {
        GoogleClientSecrets secrets = new GoogleClientSecrets().setWeb(
                new GoogleClientSecrets.Details().setClientId("dummy-id").setClientSecret("dummy-secret"));
        GoogleAuthorizationCodeFlow flow = new GoogleAuthorizationCodeFlow.Builder(
                new NetHttpTransport(), GsonFactory.getDefaultInstance(), secrets,
                Collections.singleton("https://www.googleapis.com/auth/calendar.readonly")).build();

        GoogleAuthController controller = new GoogleAuthController(
                flow, mock(GoogleCredentialRepository.class), mock(UserService.class));
        ReflectionTestUtils.setField(controller, "redirectUri", redirectUri);
        return controller;
    }

    @Test
    void authUrlUsesTheConfiguredRedirectUriAndNeverLocalhost() {
        UserModel user = new UserModel();
        user.setId(1L);

        Map<String, String> response = controllerWithRedirect(PUBLIC_REDIRECT).getAuthUrl(user);

        // The query may come encoded or not, so it is compared decoded
        String url = URLDecoder.decode(response.get("url"), StandardCharsets.UTF_8);
        assertTrue(url.contains("redirect_uri=" + PUBLIC_REDIRECT + "&"), url);
        assertFalse(url.contains("localhost"), url);
    }
}
