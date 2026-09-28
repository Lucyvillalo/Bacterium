package com.bacterium.controller;

import com.bacterium.service.ConfiguracionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.Base64;
import java.util.Map;
import java.util.Set;

@RestController
@RequestMapping("/api/configuracion")
@RequiredArgsConstructor
public class ConfiguracionController {

    private static final Set<String> TIPOS_LOGO = Set.of("image/png", "image/jpeg", "image/webp");

    private final ConfiguracionService service;

    @GetMapping
    public Map<String, String> obtener() {
        return service.obtenerConfiguracionLaboratorio();
    }

    @PutMapping
    public Map<String, String> guardar(@RequestBody Map<String, String> valores) {
        return service.guardarConfiguracionLaboratorio(valores);
    }

    @PostMapping(value = "/logo", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Map<String, String>> subirLogo(@RequestParam("logo") MultipartFile logo) throws Exception {
        if (logo.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "El archivo esta vacio"));
        }
        String contentType = logo.getContentType() == null ? "" : logo.getContentType().toLowerCase();
        if (!TIPOS_LOGO.contains(contentType)) {
            return ResponseEntity.badRequest().body(Map.of("error", "Use PNG, JPG o WebP"));
        }
        String base64 = Base64.getEncoder().encodeToString(logo.getBytes());
        service.guardarLogo("data:" + contentType + ";base64," + base64);
        return ResponseEntity.ok(service.obtenerConfiguracionLaboratorio());
    }

    @DeleteMapping("/logo")
    public ResponseEntity<Void> eliminarLogo() {
        service.eliminarLogo();
        return ResponseEntity.noContent().build();
    }
}
