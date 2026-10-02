package com.backend.gapfinder.services;

import com.backend.gapfinder.dto.requests.LoginRequest;
import com.backend.gapfinder.dto.requests.RefreshRequest;
import com.backend.gapfinder.dto.requests.RegisterRequest;
import com.backend.gapfinder.dto.responses.AuthResponse;
import com.backend.gapfinder.models.RefreshTokenModel;
import com.backend.gapfinder.models.UserModel;
import com.backend.gapfinder.repositories.UserRepository;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

// Lógica de registro, login, refresh y logout
@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final RefreshTokenService refreshTokenService;

    public AuthService(UserRepository userRepository,
                        PasswordEncoder passwordEncoder,
                        JwtService jwtService,
                        RefreshTokenService refreshTokenService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.refreshTokenService = refreshTokenService;
    }

    // Crea un usuario nuevo y le devuelve sus tokens de sesión
    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.email())) {
            throw new IllegalArgumentException("Ya existe una cuenta con ese correo");
        }

        // Antes de crear el usuario: normaliza el correo y úsalo también en la validación
        String email = request.email().toLowerCase().trim();
        if (userRepository.existsByEmail(email)) {
            throw new IllegalArgumentException("Ya existe una cuenta con ese correo");
        }

        UserModel user = new UserModel();
        user.setName(request.name());
        user.setEmail(email);
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        user.setCareer(request.career());
        user.setSemester(request.semester());
        user.setPhoneNumber(request.phoneNumber());
        user.setPreferredEffort(request.preferredEffort());
        user.setAvatarUrl(null);
        user.setVerified(false);
        user.setCreatedAt(LocalDateTime.now());

            userRepository.save(user);

            return buildAuthResponse(user);
        }

    // Valida credenciales y devuelve los tokens de sesión
    @Transactional
    public AuthResponse login(LoginRequest request) {
        UserModel user = userRepository.findByEmail(request.email().toLowerCase().trim())
                .orElseThrow(() -> new BadCredentialsException("Correo o contraseña incorrectos"));

        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new BadCredentialsException("Correo o contraseña incorrectos");
        }

        return buildAuthResponse(user);
    }

    // Genera un access token nuevo a partir de un refresh token válido
    public AuthResponse refresh(RefreshRequest request) {
        RefreshTokenModel refreshToken = refreshTokenService.verifyAndGet(request.refreshToken());
        UserModel user = refreshToken.getUser();

        String newAccessToken = jwtService.generateAccessToken(user.getEmail());

        return new AuthResponse(user.getId(), newAccessToken, refreshToken.getToken(), user.getEmail(), user.getName());
    }

    // Cierra sesión, eliminando el refresh token del usuario
    public void logout(UserModel user) {
        refreshTokenService.revokeByUserId(user.getId());
    }

    // Arma la respuesta de auth generando ambos tokens (access + refresh)
    private AuthResponse buildAuthResponse(UserModel user) {
        String accessToken = jwtService.generateAccessToken(user.getEmail());
        RefreshTokenModel refreshToken = refreshTokenService.createRefreshToken(user);
        return new AuthResponse(user.getId(), accessToken, refreshToken.getToken(), user.getEmail(), user.getName());
    }
}