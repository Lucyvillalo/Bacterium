package com.bacterium.repository;

import com.bacterium.model.Orden;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface OrdenRepository extends JpaRepository<Orden, Long> {

    Page<Orden> findByPacienteId(Long pacienteId, Pageable pageable);

    Optional<Orden> findByNumeroOrden(String numero);

    Page<Orden> findByEstado(Orden.EstadoOrden estado, Pageable pageable);

    long countByEstado(Orden.EstadoOrden estado);

    @Query("""
        SELECT o FROM Orden o
        JOIN o.paciente p
        WHERE LOWER(p.nombreCompleto) LIKE LOWER(CONCAT('%', :q, '%'))
           OR LOWER(o.numeroOrden) LIKE LOWER(CONCAT('%', :q, '%'))
        ORDER BY o.fechaRecepcion DESC
    """)
    Page<Orden> buscar(@Param("q") String query, Pageable pageable);
}
