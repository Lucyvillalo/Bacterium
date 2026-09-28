package com.bacterium.config;

import com.bacterium.model.Configuracion;
import com.bacterium.model.Medico;
import com.bacterium.model.Paciente;
import com.bacterium.model.Prueba;
import com.bacterium.model.Usuario;
import com.bacterium.repository.ConfiguracionRepository;
import com.bacterium.repository.MedicoRepository;
import com.bacterium.repository.PacienteRepository;
import com.bacterium.repository.PruebaRepository;
import com.bacterium.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDate;
import java.util.List;

@Configuration
@RequiredArgsConstructor
public class DataInitializer {

    private final PasswordEncoder passwordEncoder;

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

    @Bean
    CommandLineRunner inicializarDatos(UsuarioRepository usuarios,
                                       PruebaRepository pruebas,
                                       PacienteRepository pacientes,
                                       MedicoRepository medicos,
                                       ConfiguracionRepository configuracion) {
        return args -> {

            // ── Usuarios ─────────────────────────────────────────────────────
            if (!usuarios.existsByUsername("admin")) {
                Usuario u = new Usuario();
                u.setUsername("admin");
                u.setPassword(passwordEncoder.encode("admin123"));
                u.setNombreCompleto("Administrador Bacterium");
                u.setEmail("admin@bacterium.local");
                u.setRol(Usuario.Rol.ADMIN);
                usuarios.save(u);
            }
            if (!usuarios.existsByUsername("laboratorista")) {
                Usuario u = new Usuario();
                u.setUsername("laboratorista");
                u.setPassword(passwordEncoder.encode("lab123"));
                u.setNombreCompleto("Laboratorista Principal");
                u.setEmail("lab@bacterium.local");
                u.setRol(Usuario.Rol.LABORATORISTA);
                usuarios.save(u);
            }
            if (!usuarios.existsByUsername("recepcion")) {
                Usuario u = new Usuario();
                u.setUsername("recepcion");
                u.setPassword(passwordEncoder.encode("rec123"));
                u.setNombreCompleto("Recepcionista");
                u.setEmail("recepcion@bacterium.local");
                u.setRol(Usuario.Rol.RECEPCION);
                usuarios.save(u);
            }

            // ── Médico de ejemplo ─────────────────────────────────────────────
            if (medicos.count() == 0) {
                Medico m = new Medico();
                m.setNombreCompleto("Dr. Ejemplo Médico");
                m.setEspecialidad("Medicina General");
                m.setTelefono("2222-1111");
                medicos.save(m);
            }

            // ── Paciente de ejemplo ──────────────────────────────────────────
            if (pacientes.count() == 0) {
                Paciente p = new Paciente();
                p.setCodigoPaciente("PAC-2026-000001");
                p.setNombreCompleto("Paciente de Prueba");
                p.setFechaNacimiento(LocalDate.of(1990, 1, 1));
                p.setSexo(Paciente.Sexo.MASCULINO);
                p.setTelefono("7777-0000");
                pacientes.save(p);
            }

            // ── Pruebas de Urología con intervalos completos ─────────────────
            seedConfig(configuracion, "lab.nombre", labNombre);
            seedConfig(configuracion, "lab.direccion", labDireccion);
            seedConfig(configuracion, "lab.telefono", labTelefono);
            seedConfig(configuracion, "lab.email", labEmail);
            seedConfig(configuracion, "lab.web", labWeb);
            seedConfig(configuracion, "lab.desde", labDesde);
            seedConfig(configuracion, "lab.director", labDirector);
            seedConfig(configuracion, "lab.cedula", labCedula);
            seedConfig(configuracion, "pdf.mostrarLogo", "true");
            seedConfig(configuracion, "pdf.mostrarContacto", "true");
            seedConfig(configuracion, "pdf.mostrarMedico", "true");
            seedConfig(configuracion, "pdf.mostrarObservaciones", "true");
            seedConfig(configuracion, "pdf.mostrarFirmas", "true");
            seedConfig(configuracion, "pdf.mostrarReferencias", "true");
            seedConfig(configuracion, "pdf.soloConResultado", "true");

            seedPruebas(pruebas, "UROLOGIA", pruebasUrologia());
            seedPruebas(pruebas, "HEMATOLOGIA", pruebasHematologia());
            seedPruebas(pruebas, "QUIMICA SANGUINEA", pruebasQuimicaSanguinea());
            seedPruebas(pruebas, "COPROCULTIVO", pruebasCoprocultivo());
        };
    }

    private void seedConfig(ConfiguracionRepository repo, String clave, String valor) {
        if (!repo.existsById(clave)) {
            repo.save(new Configuracion(clave, valor == null ? "" : valor));
        }
    }

    private void seedPruebas(PruebaRepository pruebas, String area, List<PruebaSeed> seeds) {
        for (PruebaSeed s : seeds) {
            if (pruebas.existsByAreaIgnoreCaseAndNombreIgnoreCase(area, s.nombre())) {
                continue;
            }
            Prueba pr = new Prueba();
            pr.setArea(area);
            pr.setNombre(s.nombre());
            pr.setUnidades(s.unidades());
            pr.setIntervaloReferencia(s.referencia());
            pr.setOrden(s.orden());
            pruebas.save(pr);
        }
    }

    private List<PruebaSeed> pruebasUrologia() {
        return List.of(
            new PruebaSeed( 1, "COLOR",                  "",          "Amarillo claro a ambar"),
            new PruebaSeed( 2, "ASPECTO",                "",          "Transparente / Claro"),
            new PruebaSeed( 3, "DENSIDAD",               "",          "1.005 - 1.030"),
            new PruebaSeed( 4, "PH",                     "",          "4.5 - 8.0"),
            new PruebaSeed( 5, "NITRITOS",               "",          "NEGATIVO"),
            new PruebaSeed( 6, "UROBILINOGENO",          "mg/dl",     "NEGATIVO (<1.0 mg/dl)"),
            new PruebaSeed( 7, "PROTEINAS",              "mg/dl",     "NEGATIVO (<14 mg/dl)"),
            new PruebaSeed( 8, "SANGRE OCULTA",          "ery/ul",    "NEGATIVO"),
            new PruebaSeed( 9, "ESTERASA LEUCOCITARIA",  "leuco/ul",  "NEGATIVO"),
            new PruebaSeed(10, "C. CETONICOS",           "mg/dl",     "NEGATIVO"),
            new PruebaSeed(11, "BILIRRUBINA",            "",          "NEGATIVO"),
            new PruebaSeed(12, "GLUCOSA",                "mg/dl",     "NEGATIVO (<15 mg/dl)"),
            new PruebaSeed(13, "HEMATIES",               "X Campo",   "0 - 2 por campo"),
            new PruebaSeed(14, "LEUCOCITOS",             "X Campo",   "0 - 5 por campo"),
            new PruebaSeed(15, "CRISTALES",              "",          "Ausentes o escasos"),
            new PruebaSeed(16, "CILINDROS",              "",          "Ausentes"),
            new PruebaSeed(17, "CELULAS EPITELIALES",    "",          "Escasas (normal)"),
            new PruebaSeed(18, "BACTERIAS",              "",          "NO SE OBSERVAN"),
            new PruebaSeed(19, "LEVADURAS",              "",          "NO SE OBSERVAN"),
            new PruebaSeed(20, "FILAMENTO MUCOIDE",      "",          "NO SE OBSERVAN")
        );
    }

    private List<PruebaSeed> pruebasHematologia() {
        return List.of(
            new PruebaSeed( 1, "HEMOGLOBINA",              "g/dL",      "12.0 - 17.5"),
            new PruebaSeed( 2, "HEMATOCRITO",              "%",         "36 - 52"),
            new PruebaSeed( 3, "ERITROCITOS",              "10^6/uL",   "4.2 - 5.9"),
            new PruebaSeed( 4, "LEUCOCITOS",               "10^3/uL",   "4.0 - 10.0"),
            new PruebaSeed( 5, "NEUTROFILOS",              "%",         "40 - 75"),
            new PruebaSeed( 6, "LINFOCITOS",               "%",         "20 - 45"),
            new PruebaSeed( 7, "MONOCITOS",                "%",         "2 - 10"),
            new PruebaSeed( 8, "EOSINOFILOS",              "%",         "1 - 6"),
            new PruebaSeed( 9, "BASOFILOS",                "%",         "0 - 2"),
            new PruebaSeed(10, "PLAQUETAS",                "10^3/uL",   "150 - 450"),
            new PruebaSeed(11, "VCM",                      "fL",        "80 - 100"),
            new PruebaSeed(12, "HCM",                      "pg",        "27 - 33"),
            new PruebaSeed(13, "CHCM",                     "g/dL",      "32 - 36")
        );
    }

    private List<PruebaSeed> pruebasQuimicaSanguinea() {
        return List.of(
            new PruebaSeed( 1, "GLUCOSA",                  "mg/dL",     "70 - 110"),
            new PruebaSeed( 2, "COLESTEROL TOTAL",         "mg/dL",     "< 200"),
            new PruebaSeed( 3, "TRIGLICERIDOS",            "mg/dL",     "< 150"),
            new PruebaSeed( 4, "HDL COLESTEROL",           "mg/dL",     "> 40"),
            new PruebaSeed( 5, "LDL COLESTEROL",           "mg/dL",     "< 100"),
            new PruebaSeed( 6, "UREA",                     "mg/dL",     "15 - 45"),
            new PruebaSeed( 7, "NITROGENO UREICO",         "mg/dL",     "7 - 20"),
            new PruebaSeed( 8, "CREATININA",               "mg/dL",     "0.6 - 1.3"),
            new PruebaSeed( 9, "ACIDO URICO",              "mg/dL",     "3.5 - 7.2"),
            new PruebaSeed(10, "TGO/AST",                  "U/L",       "0 - 40"),
            new PruebaSeed(11, "TGP/ALT",                  "U/L",       "0 - 41")
        );
    }

    private List<PruebaSeed> pruebasCoprocultivo() {
        return List.of(
            new PruebaSeed( 1, "COLOR",                    "",          "Cafe"),
            new PruebaSeed( 2, "CONSISTENCIA",             "",          "Formada"),
            new PruebaSeed( 3, "MOCO",                     "",          "Ausente"),
            new PruebaSeed( 4, "SANGRE OCULTA",            "",          "Negativo"),
            new PruebaSeed( 5, "LEUCOCITOS FECALES",       "x campo",   "0 - 5"),
            new PruebaSeed( 6, "HEMATIES",                 "x campo",   "0 - 2"),
            new PruebaSeed( 7, "FLORA BACTERIANA",         "",          "Normal"),
            new PruebaSeed( 8, "CULTIVO",                  "",          "Sin crecimiento de patogenos"),
            new PruebaSeed( 9, "SALMONELLA SPP.",          "",          "No aislada"),
            new PruebaSeed(10, "SHIGELLA SPP.",            "",          "No aislada"),
            new PruebaSeed(11, "ESCHERICHIA COLI PATOGENA","",          "No aislada"),
            new PruebaSeed(12, "ANTIBIOGRAMA",             "",          "Segun aislamiento")
        );
    }

    private record PruebaSeed(int orden, String nombre, String unidades, String referencia) {}
}
