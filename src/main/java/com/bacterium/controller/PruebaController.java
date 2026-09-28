package com.bacterium.controller;

import com.bacterium.model.Prueba;
import com.bacterium.repository.PruebaRepository;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/pruebas")
@RequiredArgsConstructor
public class PruebaController {

    private final PruebaRepository repo;

    @GetMapping
    public List<Prueba> listar(@RequestParam(required = false) String area) {
        if (area != null && !area.isBlank()) {
            return repo.findByAreaIgnoreCaseAndActivaTrueOrderByOrdenAsc(area.trim());
        }
        return repo.findByActivaTrueOrderByAreaAscOrdenAsc();
    }

    @GetMapping("/todas")
    @PreAuthorize("hasRole('ADMIN')")
    public List<Prueba> listarTodas() {
        return repo.findAll().stream()
                .sorted((a, b) -> {
                    int area = String.valueOf(a.getArea()).compareToIgnoreCase(String.valueOf(b.getArea()));
                    if (area != 0) return area;
                    return Integer.compare(a.getOrden(), b.getOrden());
                })
                .toList();
    }

    @GetMapping("/areas")
    public List<String> areas() {
        return repo.findAreasActivas();
    }

    @GetMapping("/{id}")
    public ResponseEntity<Prueba> obtener(@PathVariable Long id) {
        return repo.findById(id).map(ResponseEntity::ok).orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    @Transactional
    @PreAuthorize("hasRole('ADMIN')")
    public Prueba crear(@Valid @RequestBody PruebaRequest req) {
        Prueba prueba = new Prueba();
        copiar(req, prueba);
        return repo.save(prueba);
    }

    @PutMapping("/{id}")
    @Transactional
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Prueba> actualizar(@PathVariable Long id, @Valid @RequestBody PruebaRequest req) {
        return repo.findById(id)
                .map(prueba -> {
                    copiar(req, prueba);
                    return ResponseEntity.ok(repo.save(prueba));
                })
                .orElse(ResponseEntity.notFound().build());
    }

    private void copiar(PruebaRequest req, Prueba prueba) {
        prueba.setArea(req.getArea().trim().toUpperCase());
        prueba.setNombre(req.getNombre().trim().toUpperCase());
        prueba.setUnidades(req.getUnidades() == null ? "" : req.getUnidades().trim());
        prueba.setIntervaloReferencia(req.getIntervaloReferencia() == null ? "" : req.getIntervaloReferencia().trim());
        prueba.setOrden(req.getOrden() == null ? 0 : req.getOrden());
        prueba.setActiva(req.isActiva());
    }

    @Data
    public static class PruebaRequest {
        @NotBlank private String area;
        @NotBlank private String nombre;
        private String unidades;
        private String intervaloReferencia;
        private Integer orden = 0;
        private boolean activa = true;
    }
}
