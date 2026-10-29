package co.impresion3d.costos.repository;

import co.impresion3d.costos.entity.Cotizacion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface CotizacionRepositorio extends JpaRepository<Cotizacion, Long> {

    /** Las más recientes primero. */
    @Query("select c from Cotizacion c order by c.id desc")
    List<Cotizacion> listarRecientes();
}
