package com.bacterium.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import java.util.Map;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthFilter jwtAuthFilter;
    private final ObjectMapper  objectMapper;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            .headers(headers -> headers.frameOptions(frame -> frame.sameOrigin()))
            .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

            // ── Respuesta JSON en lugar de redirect al login ──────────────────
            .exceptionHandling(ex -> ex
                .authenticationEntryPoint((request, response, authException) -> {
                    response.setStatus(HttpStatus.UNAUTHORIZED.value());
                    response.setContentType(MediaType.APPLICATION_JSON_VALUE);
                    response.setCharacterEncoding("UTF-8");
                    response.getWriter().write(
                        objectMapper.writeValueAsString(Map.of(
                            "error", "No autenticado",
                            "mensaje", "Se requiere iniciar sesion",
                            "status", 401
                        ))
                    );
                })
                .accessDeniedHandler((request, response, accessDeniedException) -> {
                    response.setStatus(HttpStatus.FORBIDDEN.value());
                    response.setContentType(MediaType.APPLICATION_JSON_VALUE);
                    response.setCharacterEncoding("UTF-8");
                    response.getWriter().write(
                        objectMapper.writeValueAsString(Map.of(
                            "error", "Acceso denegado",
                            "mensaje", "No tiene permiso para realizar esta accion",
                            "status", 403
                        ))
                    );
                })
            )

            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/", "/index.html", "/portal.html").permitAll()
                .requestMatchers("/api/auth/**").permitAll()
                .requestMatchers("/api/ordenes/portal/**").permitAll()
                .requestMatchers("/api/pacientes/codigo/**").permitAll()
                .requestMatchers("/css/**", "/js/**", "/img/**", "/favicon.ico").permitAll()
                .requestMatchers("/h2-console/**").permitAll()
                .requestMatchers("/api/usuarios/**").hasRole("ADMIN")
                .requestMatchers("/api/configuracion/**").hasRole("ADMIN")
                .requestMatchers("/api/pacientes/**", "/api/ordenes/**", "/api/pruebas/**", "/api/medicos/**")
                    .hasAnyRole("ADMIN", "RECEPCION", "LABORATORISTA", "MEDICO")
                .anyRequest().authenticated()
            )
            .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration configuration) throws Exception {
        return configuration.getAuthenticationManager();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(12);
    }
}
