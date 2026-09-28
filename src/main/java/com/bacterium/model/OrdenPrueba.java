package com.bacterium.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "orden_pruebas",
       uniqueConstraints = @UniqueConstraint(columnNames = {"orden_id", "prueba_id"}))
@Data
@NoArgsConstructor
public class OrdenPrueba {

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

    @Column(nullable = false)
    private boolean incluidaEnPdf = true;

    @Column(nullable = false)
    private Integer ordenPdf = 0;
}
