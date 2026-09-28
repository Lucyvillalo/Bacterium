package com.bacterium.repository;

import com.bacterium.model.Resultado;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ResultadoRepository extends JpaRepository<Resultado, Long> {
    List<Resultado> findByOrdenIdOrderByPruebaOrdenAsc(Long ordenId);
    Optional<Resultado> findByOrdenIdAndPruebaId(Long ordenId, Long pruebaId);
}
