package com.bacterium.repository;

import com.bacterium.model.Paciente;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface PacienteRepository extends JpaRepository<Paciente, Long> {

    Optional<Paciente> findByCodigoPaciente(String codigo);

    @Query("""
        SELECT p FROM Paciente p
        WHERE LOWER(p.nombreCompleto) LIKE LOWER(CONCAT('%', :q, '%'))
           OR LOWER(p.codigoPaciente) LIKE LOWER(CONCAT('%', :q, '%'))
    """)
    Page<Paciente> buscar(@Param("q") String query, Pageable pageable);

    boolean existsByCodigoPaciente(String codigo);
}
