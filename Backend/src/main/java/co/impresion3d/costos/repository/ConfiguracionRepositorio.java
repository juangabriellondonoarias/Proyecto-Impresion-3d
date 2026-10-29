package co.impresion3d.costos.repository;

import co.impresion3d.costos.entity.Configuracion;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ConfiguracionRepositorio extends JpaRepository<Configuracion, Long> {
}
