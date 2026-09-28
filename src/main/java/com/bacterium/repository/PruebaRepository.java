package com.bacterium.repository;

import com.bacterium.model.Prueba;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface PruebaRepository extends JpaRepository<Prueba, Long> {
    List<Prueba> findByActivaTrueOrderByAreaAscOrdenAsc();
    List<Prueba> findByAreaIgnoreCaseAndActivaTrueOrderByOrdenAsc(String area);
    boolean existsByAreaIgnoreCaseAndNombreIgnoreCase(String area, String nombre);

    @Query("select distinct p.area from Prueba p where p.activa = true order by p.area")
    List<String> findAreasActivas();
}
