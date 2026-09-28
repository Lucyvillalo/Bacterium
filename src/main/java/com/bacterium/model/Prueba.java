package com.bacterium.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "pruebas")
@Data
@NoArgsConstructor
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class Prueba {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 150)
    private String nombre;

    @Column(length = 50)
    private String unidades;

    @Column(length = 100)
    private String intervaloReferencia;

    @Column(length = 100)
    private String area;

    @Column(nullable = false)
    private Integer orden = 0;

    private boolean activa = true;
}
