package co.impresion3d.costos.repository;

import co.impresion3d.costos.entity.Producto;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ProductoRepositorio extends JpaRepository<Producto, Long> {

    @Query("select p from Producto p order by p.nombre")
    List<Producto> listarOrdenados();

    @Query("select count(p) from Producto p where p.filamento.id = :filamentoId")
    long contarPorFilamento(@Param("filamentoId") Long filamentoId);
}
