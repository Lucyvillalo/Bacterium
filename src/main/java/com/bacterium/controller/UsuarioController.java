package com.bacterium.controller;

import com.bacterium.model.Usuario;
import com.bacterium.repository.UsuarioRepository;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Comparator;
import java.util.List;

@RestController
@RequestMapping("/api/usuarios")
@RequiredArgsConstructor
public class UsuarioController {

    private final UsuarioRepository repo;
    private final PasswordEncoder passwordEncoder;

    @GetMapping
    @Transactional(readOnly = true)
    public List<Usuario> listar() {
        return repo.findAll().stream()
                .sorted(Comparator.comparing(Usuario::getUsername, String.CASE_INSENSITIVE_ORDER))
                .toList();
    }

    @GetMapping("/{id}")
    @Transactional(readOnly = true)
    public ResponseEntity<Usuario> obtener(@PathVariable Long id) {
        return repo.findById(id).map(ResponseEntity::ok).orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    @Transactional
    public ResponseEntity<?> crear(@Valid @RequestBody UsuarioRequest req) {
        if (req.getPassword() == null || req.getPassword().isBlank()) {
            return ResponseEntity.badRequest().body("La contrasena es obligatoria");
        }
        if (repo.existsByUsername(req.getUsername())) {
            return ResponseEntity.badRequest().body("El usuario ya existe");
        }
        if (req.getEmail() != null && !req.getEmail().isBlank() && repo.existsByEmail(req.getEmail())) {
            return ResponseEntity.badRequest().body("El correo ya existe");
        }
        Usuario usuario = new Usuario();
        copiar(req, usuario);
        usuario.setPassword(passwordEncoder.encode(req.getPassword()));
        return ResponseEntity.ok(repo.save(usuario));
    }

    @PutMapping("/{id}")
    @Transactional
    public ResponseEntity<?> actualizar(@PathVariable Long id, @Valid @RequestBody UsuarioRequest req) {
        Usuario usuario = repo.findById(id)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

        boolean usuarioDuplicado = repo.findByUsername(req.getUsername())
                .filter(u -> !u.getId().equals(id))
                .isPresent();
        if (usuarioDuplicado) {
            return ResponseEntity.badRequest().body("El usuario ya existe");
        }
        if (req.getEmail() != null && !req.getEmail().isBlank()) {
            boolean emailDuplicado = repo.findByEmail(req.getEmail())
                    .filter(u -> !u.getId().equals(id))
                    .isPresent();
            if (emailDuplicado) {
                return ResponseEntity.badRequest().body("El correo ya existe");
            }
        }

        copiar(req, usuario);
        if (req.getPassword() != null && !req.getPassword().isBlank()) {
            usuario.setPassword(passwordEncoder.encode(req.getPassword()));
        }
        return ResponseEntity.ok(repo.save(usuario));
    }

    private void copiar(UsuarioRequest req, Usuario usuario) {
        usuario.setUsername(req.getUsername().trim());
        usuario.setNombreCompleto(req.getNombreCompleto().trim());
        usuario.setEmail(req.getEmail() == null || req.getEmail().isBlank() ? null : req.getEmail().trim());
        usuario.setRol(req.getRol());
        usuario.setActivo(req.isActivo());
    }

    @Data
    public static class UsuarioRequest {
        @NotBlank private String username;
        private String password;
        @NotBlank private String nombreCompleto;
        private String email;
        @NotNull private Usuario.Rol rol;
        private boolean activo = true;
    }
}
