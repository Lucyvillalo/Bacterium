package com.bacterium.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "pacientes")
@Data
@NoArgsConstructor
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler", "ordenes"})
public class Paciente {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Código único que el paciente usa para ver sus resultados en el portal
    @Column(nullable = false, unique = true, length = 20)
    private String codigoPaciente;

    @Column(nullable = false, length = 150)
    private String nombreCompleto;

    @Column(nullable = false)
    private LocalDate fechaNacimiento;

    @Enumerated(EnumType.STRING)
    private Sexo sexo;

    @Column(length = 20)
    private String telefono;

    @Column(length = 150)
    private String email;

    @Column(length = 300)
    private String direccion;

    @Column(updatable = false)
    private LocalDateTime creadoEn = LocalDateTime.now();

    @OneToMany(mappedBy = "paciente", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<Orden> ordenes;

    public enum Sexo {
        MASCULINO, FEMENINO, OTRO
    }
}
