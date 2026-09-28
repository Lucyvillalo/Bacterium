package com.bacterium.repository;

import com.bacterium.model.Configuracion;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ConfiguracionRepository extends JpaRepository<Configuracion, String> {
}
