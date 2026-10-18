package com.backend.gapfinder.services;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Date;

// Se encarga de crear y validar los JWT (access tokens)
@Component
public class JwtService {

    @Value("${jwt.secret}")
    private String secret; // clave para firmar y verificar los tokens

    @Value("${jwt.expiration-ms}")
    private long expirationMs; // cuánto dura un token antes de expirar

    // Convierte el secret en la llave que usa la librería para firmar/verificar
    private SecretKey getKey() {
        return Keys.hmacShaKeyFor(secret.getBytes());
    }

    // Genera un JWT nuevo para un usuario, a partir de su email
    public String generateAccessToken(String email) {
        return Jwts.builder()
                .subject(email)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + expirationMs))
                .signWith(getKey())
                .compact();
    }

    // Saca el email guardado dentro de un token
    public String extractUsername(String token) {
        return parseClaims(token).getSubject();
    }

    // Dice si un token es válido (firma correcta y no expirado)
    public boolean isTokenValid(String token) {
        try {
            return !isTokenExpired(token);
        } catch (Exception e) {
            return false;
        }
    }

    // Revisa si la fecha de expiración del token ya pasó
    private boolean isTokenExpired(String token) {
        return parseClaims(token).getExpiration().before(new Date());
    }

    // Verifica la firma del token con el secret y extrae su contenido
    private Claims parseClaims(String token) {
        return Jwts.parser()
                .verifyWith(getKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}