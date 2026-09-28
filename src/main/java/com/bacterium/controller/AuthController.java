package com.bacterium.controller;

import com.bacterium.security.JwtTokenProvider;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthenticationManager authManager;
    private final JwtTokenProvider tokenProvider;

    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody LoginRequest req) {
        Authentication auth = authManager.authenticate(
                new UsernamePasswordAuthenticationToken(req.getUsername(), req.getPassword()));

        String username = ((UserDetails) auth.getPrincipal()).getUsername();
        String role = auth.getAuthorities().stream()
                .findFirst()
                .map(a -> a.getAuthority().replace("ROLE_", ""))
                .orElse("");
        String token = tokenProvider.generarToken(username, role);

        return ResponseEntity.ok(Map.of(
                "token", token,
                "tipo", "Bearer",
                "usuario", auth.getName(),
                "rol", role
        ));
    }

    @Data
    static class LoginRequest {
        @NotBlank private String username;
        @NotBlank private String password;
    }
}
