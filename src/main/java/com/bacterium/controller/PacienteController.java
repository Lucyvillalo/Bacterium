package com.bacterium.controller;

import com.bacterium.model.Paciente;
import com.bacterium.repository.PacienteRepository;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/pacientes")
@RequiredArgsConstructor
public class PacienteController {

    private final PacienteRepository repo;

    @GetMapping
    public Page<Paciente> listar(
            @RequestParam(required = false) String q,
            @PageableDefault(size = 20) Pageable pageable) {
        if (q != null && !q.isBlank()) {
            return repo.buscar(q.trim(), pageable);
        }
        return repo.findAll(pageable);
    }

    @GetMapping("/{id}")
    public ResponseEntity<Paciente> obtener(@PathVariable Long id) {
        return repo.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/codigo/{codigo}")
    public ResponseEntity<Paciente> porCodigo(@PathVariable String codigo) {
        return repo.findByCodigoPaciente(codigo)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN','RECEPCION')")
    public ResponseEntity<Paciente> crear(@Valid @RequestBody Paciente paciente) {
        if (paciente.getCodigoPaciente() == null || paciente.getCodigoPaciente().isBlank()) {
            paciente.setCodigoPaciente(generarCodigo());
        }
        return ResponseEntity.ok(repo.save(paciente));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','RECEPCION')")
    public ResponseEntity<Paciente> actualizar(@PathVariable Long id,
                                               @Valid @RequestBody Paciente datos) {
        return repo.findById(id).map(p -> {
            p.setNombreCompleto(datos.getNombreCompleto());
            p.setFechaNacimiento(datos.getFechaNacimiento());
            p.setSexo(datos.getSexo());
            p.setTelefono(datos.getTelefono());
            p.setEmail(datos.getEmail());
            p.setDireccion(datos.getDireccion());
            return ResponseEntity.ok(repo.save(p));
        }).orElse(ResponseEntity.notFound().build());
    }

    private String generarCodigo() {
        long total = repo.count() + 1;
        return String.format("PAC-%d-%06d", java.time.Year.now().getValue(), total);
    }
}
