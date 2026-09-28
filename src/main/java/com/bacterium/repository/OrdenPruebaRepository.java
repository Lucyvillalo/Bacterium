package com.bacterium.repository;

import com.bacterium.model.OrdenPrueba;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface OrdenPruebaRepository extends JpaRepository<OrdenPrueba, Long> {
    List<OrdenPrueba> findByOrdenIdOrderByPruebaAreaAscOrdenPdfAscPruebaOrdenAsc(Long ordenId);
    List<OrdenPrueba> findByOrdenIdAndIncluidaEnPdfTrueOrderByPruebaAreaAscOrdenPdfAscPruebaOrdenAsc(Long ordenId);
    Optional<OrdenPrueba> findByOrdenIdAndPruebaId(Long ordenId, Long pruebaId);
    void deleteByOrdenId(Long ordenId);
}
