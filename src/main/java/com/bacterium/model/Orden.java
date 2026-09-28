package com.bacterium.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "ordenes")
@Data
@NoArgsConstructor
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class Orden {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 30)
    private String numeroOrden;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "paciente_id", nullable = false)
    @JsonIgnoreProperties({"ordenes"})
    private Paciente paciente;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "medico_referente_id")
    private Medico medicoReferente;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "recepcionado_por_id")
    @JsonIgnoreProperties({"password"})
    private Usuario recepcionadoPor;

    @Column(updatable = false)
    private LocalDateTime fechaRecepcion = LocalDateTime.now();

    private LocalDateTime fechaEntrega;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EstadoOrden estado = EstadoOrden.PENDIENTE;

    @Column(length = 500)
    private String observaciones;

    @Column(length = 100)
    private String tipoMuestra = "Orina (chorro medio)";

    @OneToMany(mappedBy = "orden", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @JsonIgnoreProperties({"orden"})
    private List<Resultado> resultados;

    public enum EstadoOrden {
        PENDIENTE,
        EN_PROCESO,
        COMPLETADO,
        ENTREGADO
    }
}
