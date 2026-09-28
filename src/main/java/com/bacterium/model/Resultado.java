package com.bacterium.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "resultados")
@Data
@NoArgsConstructor
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class Resultado {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "orden_id", nullable = false)
    @JsonIgnoreProperties({"resultados", "paciente", "medicoReferente", "recepcionadoPor"})
    private Orden orden;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "prueba_id", nullable = false)
    private Prueba prueba;

    @Column(length = 200)
    private String valor;

    private boolean alarma = false;

    // "H" = alto, "L" = bajo, null = normal
    @Column(length = 2)
    private String tipoAlarma;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ingresado_por_id")
    @JsonIgnoreProperties({"password"})
    private Usuario ingresadoPor;

    private LocalDateTime ingresadoEn = LocalDateTime.now();
}
