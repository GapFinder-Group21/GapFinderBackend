package com.backend.gapfinder.services;

import com.backend.gapfinder.exceptions.NotFoundException;
import com.backend.gapfinder.models.GoogleCredentialModel;
import com.backend.gapfinder.repositories.GoogleCredentialRepository;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
import com.google.api.client.googleapis.auth.oauth2.GoogleCredential;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.client.util.DateTime;
import com.google.api.services.calendar.Calendar;
import com.google.api.services.calendar.model.Event;
import com.google.api.services.calendar.model.Events;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Date;
import java.util.List;

@Slf4j
@Service
public class GoogleCalendarService {

    private final GoogleCredentialRepository googleCredentialRepository;
    private final GoogleAuthorizationCodeFlow flow;

    @Value("${google.client-id}")
    private String clientId;

    @Value("${google.client-secret}")
    private String clientSecret;

    public GoogleCalendarService(GoogleCredentialRepository googleCredentialRepository,
                                 GoogleAuthorizationCodeFlow flow) {
        this.googleCredentialRepository = googleCredentialRepository;
        this.flow = flow;
    }

    // Refresca el access token si ya expiró, usando el refresh token guardado
    private String getValidAccessToken(GoogleCredentialModel credential) throws Exception {
        if (credential.getExpirationTime().isAfter(Instant.now().plusSeconds(60))) {
            return credential.getAccessToken();
        }

        log.info("Access token expirado, refrescando con refresh_token para el usuario {}",
                credential.getUser().getId());

        GoogleCredential googleCredential = new GoogleCredential.Builder()
                .setTransport(GoogleNetHttpTransport.newTrustedTransport())
                .setJsonFactory(GsonFactory.getDefaultInstance())
                .setClientSecrets(clientId, clientSecret)
                .build()
                .setRefreshToken(credential.getRefreshToken());

        googleCredential.refreshToken();

        credential.setAccessToken(googleCredential.getAccessToken());
        credential.setExpirationTime(Instant.now().plusSeconds(googleCredential.getExpiresInSeconds()));
        googleCredentialRepository.save(credential);

        return credential.getAccessToken();
    }

        // Trae ocurrencias individuales de una semana del calendario primario del usuario
    public List<Event> getEvents(Long userId) throws Exception {
        GoogleCredentialModel credential = googleCredentialRepository.findByUserId(userId)
                .orElseThrow(() -> new NotFoundException(
                        "El usuario " + userId + " no ha conectado su cuenta de Google"));

        String accessToken = getValidAccessToken(credential);

        Calendar service = new Calendar.Builder(
                GoogleNetHttpTransport.newTrustedTransport(),
                GsonFactory.getDefaultInstance(),
                request -> request.getHeaders().setAuthorization("Bearer " + accessToken))
                .setApplicationName("GapFinder")
                .build();

        // Ventana puntual de lunes a sábado; el usuario dispara la importación manualmente.
        LocalDate hoy = LocalDate.now();
        LocalDate lunes = hoy.with(DayOfWeek.MONDAY);
        LocalDate domingoSiguiente = lunes.plusDays(7); // límite exclusivo, incluye todo el sábado

        Date timeMin = Date.from(lunes.atStartOfDay(ZoneId.systemDefault()).toInstant());
        Date timeMax = Date.from(domingoSiguiente.atStartOfDay(ZoneId.systemDefault()).toInstant());

        // singleEvents=true: Google expande la recurrencia y devuelve cada ocurrencia real.
        Events events = service.events().list("primary")
                .setSingleEvents(true)
                .setOrderBy("startTime")
                .setTimeMin(new DateTime(timeMin))
                .setTimeMax(new DateTime(timeMax))
                .setMaxResults(250)
                .execute();

        return events.getItems();
    }
}
