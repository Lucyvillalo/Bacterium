package com.bacterium.controller;

import com.bacterium.model.Medico;
import com.bacterium.repository.MedicoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/medicos")
@RequiredArgsConstructor
public class MedicoController {

    private final MedicoRepository repo;

    @GetMapping
    public List<Medico> listar() {
        return repo.findAll();
    }

    @GetMapping("/{id}")
    public ResponseEntity<Medico> obtener(@PathVariable Long id) {
        return repo.findById(id).map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN','RECEPCION')")
    public ResponseEntity<Medico> crear(@RequestBody Medico medico) {
        return ResponseEntity.ok(repo.save(medico));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','RECEPCION')")
    public ResponseEntity<Medico> actualizar(@PathVariable Long id, @RequestBody Medico datos) {
        return repo.findById(id).map(m -> {
            m.setNombreCompleto(datos.getNombreCompleto());
            m.setEspecialidad(datos.getEspecialidad());
            m.setTelefono(datos.getTelefono());
            m.setActivo(datos.isActivo());
            return ResponseEntity.ok(repo.save(m));
        }).orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> eliminar(@PathVariable Long id) {
        return repo.findById(id).map(m -> {
            m.setActivo(false);
            repo.save(m);
            return ResponseEntity.ok().<Void>build();
        }).orElse(ResponseEntity.notFound().build());
    }
}
