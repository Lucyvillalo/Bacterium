package com.bacterium.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "configuracion")
@Data
@NoArgsConstructor
public class Configuracion {

    @Id
    @Column(name = "config_key", length = 80)
    private String clave;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String valor;

    public Configuracion(String clave, String valor) {
        this.clave = clave;
        this.valor = valor;
    }
}
