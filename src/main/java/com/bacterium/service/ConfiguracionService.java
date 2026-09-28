package com.bacterium.service;

import com.bacterium.model.Configuracion;
import com.bacterium.repository.ConfiguracionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class ConfiguracionService {

    public static final String LOGO_CLAVE = "lab.logo.dataUrl";

    private final ConfiguracionRepository repo;

    @Value("${lab.nombre:LABORATORIO CLINICO BACTERIUM}")
    private String labNombre;
    @Value("${lab.direccion:Direccion del laboratorio}")
    private String labDireccion;
    @Value("${lab.telefono:2222-0000}")
    private String labTelefono;
    @Value("${lab.email:info@bacterium.com}")
    private String labEmail;
    @Value("${lab.web:www.bacterium.com}")
    private String labWeb;
    @Value("${lab.desde:Desde 1987}")
    private String labDesde;
    @Value("${lab.director:Director Tecnico}")
    private String labDirector;
    @Value("${lab.cedula:}")
    private String labCedula;

    @Transactional(readOnly = true)
    public Map<String, String> obtenerConfiguracionLaboratorio() {
        Map<String, String> config = defaults();
        repo.findAll().forEach(item -> config.put(item.getClave(), item.getValor()));
        return config;
    }

    @Transactional
    public Map<String, String> guardarConfiguracionLaboratorio(Map<String, String> valores) {
        Map<String, String> permitidas = defaults();
        for (String clave : permitidas.keySet()) {
            if (valores.containsKey(clave)) {
                String valor = valores.get(clave) == null ? "" : valores.get(clave).trim();
                repo.save(new Configuracion(clave, valor));
            }
        }
        return obtenerConfiguracionLaboratorio();
    }

    @Transactional(readOnly = true)
    public String valor(String clave) {
        return repo.findById(clave)
                .map(Configuracion::getValor)
                .orElse(defaults().getOrDefault(clave, ""));
    }

    @Transactional
    public void guardarLogo(String dataUrl) {
        repo.save(new Configuracion(LOGO_CLAVE, dataUrl));
    }

    @Transactional
    public void eliminarLogo() {
        repo.deleteById(LOGO_CLAVE);
    }

    private Map<String, String> defaults() {
        Map<String, String> map = new LinkedHashMap<>();
        map.put("lab.nombre", labNombre);
        map.put("lab.direccion", labDireccion);
        map.put("lab.telefono", labTelefono);
        map.put("lab.email", labEmail);
        map.put("lab.web", labWeb);
        map.put("lab.desde", labDesde);
        map.put("lab.director", labDirector);
        map.put("lab.cedula", labCedula);
        map.put(LOGO_CLAVE, "");
        map.put("pdf.mostrarLogo", "true");
        map.put("pdf.mostrarContacto", "true");
        map.put("pdf.mostrarMedico", "true");
        map.put("pdf.mostrarObservaciones", "true");
        map.put("pdf.mostrarFirmas", "true");
        map.put("pdf.mostrarReferencias", "true");
        map.put("pdf.soloConResultado", "true");
        return map;
    }
}
