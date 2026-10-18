package com.backend.gapfinder.services;

import com.backend.gapfinder.models.RefreshTokenModel;
import com.backend.gapfinder.models.UserModel;
import com.backend.gapfinder.repositories.RefreshTokenRepository;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.UUID;

// Crea, valida y revoca los refresh tokens de los usuarios
@Service
public class RefreshTokenService {

    @Value("${jwt.refresh-expiration-days}")
    private long refreshExpirationDays; // cuántos días dura un refresh token

    private final RefreshTokenRepository refreshTokenRepository;

    public RefreshTokenService(RefreshTokenRepository refreshTokenRepository) {
        this.refreshTokenRepository = refreshTokenRepository;
    }

    // Genera un refresh token nuevo para un usuario (borrando el anterior si tenía)
    public RefreshTokenModel createRefreshToken(UserModel user) {
        // Un dispositivo "de confianza" activo a la vez por usuario.
        refreshTokenRepository.deleteByUserId(user.getId());

        RefreshTokenModel refreshToken = new RefreshTokenModel();
        refreshToken.setUser(user);
        refreshToken.setToken(UUID.randomUUID().toString());
        refreshToken.setExpiryDate(LocalDateTime.now().plusDays(refreshExpirationDays));

        return refreshTokenRepository.save(refreshToken);
    }

    // Busca un refresh token y falla si no existe, está revocado o ya expiró
    public RefreshTokenModel verifyAndGet(String token) {
        RefreshTokenModel refreshToken = refreshTokenRepository.findByToken(token)
                .orElseThrow(() -> new IllegalArgumentException("Refresh token inválido"));

        if (refreshToken.isRevoked() || refreshToken.getExpiryDate().isBefore(LocalDateTime.now())) {
            throw new IllegalArgumentException("Sesión expirada, inicia sesión de nuevo");
        }

        return refreshToken;
    }

    // Elimina el refresh token de un usuario (por ejemplo, al cerrar sesión)
    public void revokeByUserId(Long userId) {
        refreshTokenRepository.deleteByUserId(userId);
    }
}