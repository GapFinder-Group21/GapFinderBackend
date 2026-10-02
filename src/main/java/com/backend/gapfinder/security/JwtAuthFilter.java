package com.backend.gapfinder.security;

import com.backend.gapfinder.repositories.UserRepository;
import com.backend.gapfinder.services.JwtService;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

// Filtro que revisa el JWT de cada petición y autentica al usuario si es válido
@Component
public class JwtAuthFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final UserRepository userRepository;

    public JwtAuthFilter(JwtService jwtService, UserRepository userRepository) {
        this.jwtService = jwtService;
        this.userRepository = userRepository;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                     HttpServletResponse response,
                                     FilterChain filterChain) throws ServletException, IOException {

        // Lee el header Authorization de la petición
        final String authHeader = request.getHeader("Authorization");

        // Si no viene token, sigue de largo sin autenticar
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        // Saca el token, quitando el prefijo "Bearer "
        final String jwt = authHeader.substring(7);

        // Si el token es válido y todavía no hay usuario autenticado en esta petición
        if (jwtService.isTokenValid(jwt) && SecurityContextHolder.getContext().getAuthentication() == null) {
            String email = jwtService.extractUsername(jwt);

            // Busca el usuario del token y lo marca como autenticado para esta petición
            userRepository.findByEmail(email).ifPresent(user -> {
                var authToken = new UsernamePasswordAuthenticationToken(user, null, List.of());
                authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                SecurityContextHolder.getContext().setAuthentication(authToken);
            });
        }

        // Continúa con el resto de filtros/controller
        filterChain.doFilter(request, response);
    }
}