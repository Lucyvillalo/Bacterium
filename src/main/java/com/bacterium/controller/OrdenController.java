package com.bacterium.controller;

import com.bacterium.model.Medico;
import com.bacterium.model.Orden;
import com.bacterium.model.OrdenPrueba;
import com.bacterium.model.Paciente;
import com.bacterium.model.Prueba;
import com.bacterium.model.Resultado;
import com.bacterium.repository.MedicoRepository;
import com.bacterium.repository.OrdenPruebaRepository;
import com.bacterium.repository.OrdenRepository;
import com.bacterium.repository.PacienteRepository;
import com.bacterium.repository.PruebaRepository;
import com.bacterium.repository.ResultadoRepository;
import com.bacterium.service.PdfResultadoService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/ordenes")
@RequiredArgsConstructor
public class OrdenController {

    private final OrdenRepository    ordenRepo;
    private final PacienteRepository pacienteRepo;
    private final MedicoRepository   medicoRepo;
    private final PruebaRepository   pruebaRepo;
    private final ResultadoRepository resultadoRepo;
    private final OrdenPruebaRepository ordenPruebaRepo;
    private final PdfResultadoService pdfService;

    @GetMapping
    @Transactional(readOnly = true)
    public Page<Map<String, Object>> listar(
            @RequestParam(required = false) String q,
            @PageableDefault(size = 20) Pageable pageable) {
        Page<Orden> page = q != null && !q.isBlank()
                ? ordenRepo.buscar(q.trim(), pageable)
                : ordenRepo.findAll(pageable);
        return page.map(this::resumenOrden);
    }

    @GetMapping("/{id}")
    @Transactional(readOnly = true)
    public ResponseEntity<Map<String, Object>> obtener(@PathVariable Long id) {
        return ordenRepo.findById(id)
                .map(o -> ResponseEntity.ok(resumenOrden(o)))
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    @Transactional
    @PreAuthorize("hasAnyRole('ADMIN','RECEPCION')")
    public ResponseEntity<Map<String, Object>> crear(@Valid @RequestBody CrearOrdenRequest req) {
        Paciente paciente = pacienteRepo.findById(req.getPacienteId())
                .orElseThrow(() -> new RuntimeException("Paciente no encontrado"));

        Orden orden = new Orden();
        orden.setPaciente(paciente);
        orden.setNumeroOrden(generarNumeroOrden());
        orden.setEstado(Orden.EstadoOrden.PENDIENTE);
        orden.setObservaciones(req.getObservaciones());
        if (req.getTipoMuestra() != null && !req.getTipoMuestra().isBlank()) {
            orden.setTipoMuestra(req.getTipoMuestra());
        }

        if (req.getMedicoReferenteId() != null) {
            medicoRepo.findById(req.getMedicoReferenteId())
                    .ifPresent(orden::setMedicoReferente);
        }

        Orden guardada = ordenRepo.save(orden);
        guardarPruebasSolicitadas(guardada, req.getPruebaIds());

        return ResponseEntity.ok(resumenOrden(guardada));
    }

    @GetMapping("/{id}/pruebas")
    @Transactional(readOnly = true)
    public List<Map<String, Object>> pruebasSolicitadas(@PathVariable Long id) {
        return ordenPruebaRepo.findByOrdenIdOrderByPruebaAreaAscOrdenPdfAscPruebaOrdenAsc(id)
                .stream()
                .map(this::resumenOrdenPrueba)
                .toList();
    }

    @PutMapping("/{id}/pruebas")
    @Transactional
    @PreAuthorize("hasAnyRole('ADMIN','RECEPCION','LABORATORISTA')")
    public ResponseEntity<Map<String, Object>> actualizarPruebasSolicitadas(
            @PathVariable Long id,
            @RequestBody ActualizarPruebasOrdenRequest req) {
        Orden orden = ordenRepo.findById(id)
                .orElseThrow(() -> new RuntimeException("Orden no encontrada"));
        ordenPruebaRepo.deleteByOrdenId(id);
        guardarPruebasSolicitadasDetalle(orden, req.getPruebas());
        return ResponseEntity.ok(resumenOrden(orden));
    }

    @GetMapping("/{id}/resultados")
    @Transactional(readOnly = true)
    public List<Resultado> resultados(@PathVariable Long id) {
        return resultadoRepo.findByOrdenIdOrderByPruebaOrdenAsc(id);
    }

    @PutMapping("/{id}/uroanalisis")
    @Transactional
    @PreAuthorize("hasAnyRole('ADMIN','LABORATORISTA')")
    public ResponseEntity<Map<String, Object>> guardarUroanalisis(
            @PathVariable Long id,
            @RequestBody GuardarResultadosRequest request) {

        Orden orden = ordenRepo.findById(id)
                .orElseThrow(() -> new RuntimeException("Orden no encontrada"));

        // Actualizar observaciones si vienen en el request
        if (request.getObservaciones() != null) {
            orden.setObservaciones(request.getObservaciones());
        }

        for (ResultadoRequest item : request.getResultados()) {
            var prueba = pruebaRepo.findById(item.getPruebaId())
                    .orElseThrow(() -> new RuntimeException("Prueba no encontrada"));
            ordenPruebaRepo.findByOrdenIdAndPruebaId(id, prueba.getId())
                    .orElseGet(() -> crearOrdenPrueba(orden, prueba, prueba.getOrden(), true));
            Resultado resultado = resultadoRepo.findByOrdenIdAndPruebaId(id, prueba.getId())
                    .orElseGet(Resultado::new);
            resultado.setOrden(orden);
            resultado.setPrueba(prueba);
            resultado.setValor(item.getValor());
            resultado.setAlarma(Boolean.TRUE.equals(item.getAlarma()));
            resultadoRepo.save(resultado);
        }

        orden.setEstado(Orden.EstadoOrden.COMPLETADO);
        orden.setFechaEntrega(java.time.LocalDateTime.now());
        ordenRepo.save(orden);

        return ResponseEntity.ok(resumenOrden(orden));
    }

    @GetMapping("/{id}/pdf")
    @Transactional(readOnly = true)
    public ResponseEntity<byte[]> descargarPdf(@PathVariable Long id) throws Exception {
        Orden orden = ordenRepo.findById(id)
                .orElseThrow(() -> new RuntimeException("Orden no encontrada"));
        orden.setResultados(resultadosParaPdf(id));
        byte[] pdf = pdfService.generarPdf(orden);
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"resultado-" + orden.getNumeroOrden() + ".pdf\"")
                .body(pdf);
    }

    @GetMapping("/portal/{codigoPaciente}/{numeroOrden}/check")
    @Transactional(readOnly = true)
    public ResponseEntity<Map<String, Object>> checkPublico(
            @PathVariable String codigoPaciente,
            @PathVariable String numeroOrden) {
        return ordenRepo.findByNumeroOrden(numeroOrden)
                .filter(o -> o.getPaciente().getCodigoPaciente().equalsIgnoreCase(codigoPaciente))
                .filter(o -> o.getEstado() == Orden.EstadoOrden.COMPLETADO
                        || o.getEstado() == Orden.EstadoOrden.ENTREGADO)
                .map(o -> ResponseEntity.ok(resumenOrden(o)))
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/portal/{codigoPaciente}/{numeroOrden}/pdf")
    @Transactional(readOnly = true)
    public ResponseEntity<byte[]> pdfPublico(
            @PathVariable String codigoPaciente,
            @PathVariable String numeroOrden) throws Exception {
        Orden orden = ordenRepo.findByNumeroOrden(numeroOrden)
                .filter(o -> o.getPaciente().getCodigoPaciente().equalsIgnoreCase(codigoPaciente))
                .filter(o -> o.getEstado() == Orden.EstadoOrden.COMPLETADO
                        || o.getEstado() == Orden.EstadoOrden.ENTREGADO)
                .orElseThrow(() -> new RuntimeException("No se encontraron resultados"));
        orden.setResultados(resultadoRepo.findByOrdenIdOrderByPruebaOrdenAsc(orden.getId()));
        orden.setResultados(resultadosParaPdf(orden.getId()));
        byte[] pdf = pdfService.generarPdf(orden);
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "inline; filename=\"resultado-" + numeroOrden + ".pdf\"")
                .body(pdf);
    }

    // ── Stats para dashboard ─────────────────────────────────────────────────
    @GetMapping("/stats")
    @Transactional(readOnly = true)
    public Map<String, Object> stats() {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("pendientes",  ordenRepo.countByEstado(Orden.EstadoOrden.PENDIENTE));
        m.put("enProceso",   ordenRepo.countByEstado(Orden.EstadoOrden.EN_PROCESO));
        m.put("completadas", ordenRepo.countByEstado(Orden.EstadoOrden.COMPLETADO));
        m.put("entregadas",  ordenRepo.countByEstado(Orden.EstadoOrden.ENTREGADO));
        return m;
    }

    // ── helpers ──────────────────────────────────────────────────────────────

    private Map<String, Object> resumenOrden(Orden o) {
        Map<String, Object> pac = new LinkedHashMap<>();
        pac.put("id",              o.getPaciente().getId());
        pac.put("codigoPaciente",  o.getPaciente().getCodigoPaciente());
        pac.put("nombreCompleto",  o.getPaciente().getNombreCompleto());

        Map<String, Object> med = new LinkedHashMap<>();
        if (o.getMedicoReferente() != null) {
            med.put("id",            o.getMedicoReferente().getId());
            med.put("nombreCompleto",o.getMedicoReferente().getNombreCompleto());
            med.put("especialidad",  o.getMedicoReferente().getEspecialidad());
        }

        Map<String, Object> ord = new LinkedHashMap<>();
        ord.put("id",             o.getId());
        ord.put("numeroOrden",    o.getNumeroOrden());
        ord.put("estado",         o.getEstado());
        ord.put("fechaRecepcion", o.getFechaRecepcion());
        ord.put("fechaEntrega",   o.getFechaEntrega());
        ord.put("observaciones",  o.getObservaciones());
        ord.put("paciente",       pac);
        ord.put("medico",         med.isEmpty() ? null : med);
        return ord;
    }

    private Map<String, Object> resumenOrdenPrueba(OrdenPrueba op) {
        Prueba p = op.getPrueba();
        Map<String, Object> prueba = new LinkedHashMap<>();
        prueba.put("id", p.getId());
        prueba.put("area", p.getArea());
        prueba.put("nombre", p.getNombre());
        prueba.put("unidades", p.getUnidades());
        prueba.put("intervaloReferencia", p.getIntervaloReferencia());
        prueba.put("orden", p.getOrden());
        prueba.put("activa", p.isActiva());

        Map<String, Object> item = new LinkedHashMap<>();
        item.put("id", op.getId());
        item.put("incluidaEnPdf", op.isIncluidaEnPdf());
        item.put("ordenPdf", op.getOrdenPdf());
        item.put("prueba", prueba);
        return item;
    }

    private String generarNumeroOrden() {
        long total = ordenRepo.count() + 1;
        return String.format("ORD-%d-%06d", java.time.Year.now().getValue(), total);
    }

    private void guardarPruebasSolicitadas(Orden orden, List<Long> pruebaIds) {
        if (pruebaIds == null || pruebaIds.isEmpty()) {
            return;
        }
        int ordenPdf = 1;
        for (Long pruebaId : pruebaIds.stream().distinct().toList()) {
            Prueba prueba = pruebaRepo.findById(pruebaId)
                    .orElseThrow(() -> new RuntimeException("Prueba no encontrada"));
            crearOrdenPrueba(orden, prueba, ordenPdf++, true);
        }
    }

    private void guardarPruebasSolicitadasDetalle(Orden orden, List<PruebaOrdenRequest> pruebas) {
        if (pruebas == null || pruebas.isEmpty()) {
            return;
        }
        for (int i = 0; i < pruebas.size(); i++) {
            PruebaOrdenRequest item = pruebas.get(i);
            Prueba prueba = pruebaRepo.findById(item.getPruebaId())
                    .orElseThrow(() -> new RuntimeException("Prueba no encontrada"));
            crearOrdenPrueba(orden, prueba, item.getOrdenPdf() == null ? i + 1 : item.getOrdenPdf(),
                    item.getIncluidaEnPdf() == null || item.getIncluidaEnPdf());
        }
    }

    private OrdenPrueba crearOrdenPrueba(Orden orden, Prueba prueba, Integer ordenPdf, boolean incluidaEnPdf) {
        OrdenPrueba op = new OrdenPrueba();
        op.setOrden(orden);
        op.setPrueba(prueba);
        op.setOrdenPdf(ordenPdf == null ? prueba.getOrden() : ordenPdf);
        op.setIncluidaEnPdf(incluidaEnPdf);
        return ordenPruebaRepo.save(op);
    }

    private List<Resultado> resultadosParaPdf(Long ordenId) {
        List<Long> pruebaIds = ordenPruebaRepo
                .findByOrdenIdAndIncluidaEnPdfTrueOrderByPruebaAreaAscOrdenPdfAscPruebaOrdenAsc(ordenId)
                .stream()
                .map(op -> op.getPrueba().getId())
                .toList();
        List<Resultado> resultados = resultadoRepo.findByOrdenIdOrderByPruebaOrdenAsc(ordenId);
        if (pruebaIds.isEmpty()) {
            return resultados;
        }
        return resultados.stream()
                .filter(r -> r.getPrueba() != null && pruebaIds.contains(r.getPrueba().getId()))
                .sorted((a, b) -> Integer.compare(pruebaIds.indexOf(a.getPrueba().getId()), pruebaIds.indexOf(b.getPrueba().getId())))
                .toList();
    }

    // ── DTOs ─────────────────────────────────────────────────────────────────

    @Data
    public static class CrearOrdenRequest {
        @NotNull private Long pacienteId;
        private Long   medicoReferenteId;
        private String observaciones;
        private String tipoMuestra;
        private List<Long> pruebaIds = List.of();
    }

    @Data
    public static class GuardarResultadosRequest {
        private List<ResultadoRequest> resultados    = List.of();
        private String                 observaciones;
    }

    @Data
    public static class ResultadoRequest {
        @NotNull private Long    pruebaId;
        private String  valor;
        private Boolean alarma;
    }

    @Data
    public static class ActualizarPruebasOrdenRequest {
        private List<PruebaOrdenRequest> pruebas = List.of();
    }

    @Data
    public static class PruebaOrdenRequest {
        @NotNull private Long pruebaId;
        private Boolean incluidaEnPdf = true;
        private Integer ordenPdf;
    }
}












