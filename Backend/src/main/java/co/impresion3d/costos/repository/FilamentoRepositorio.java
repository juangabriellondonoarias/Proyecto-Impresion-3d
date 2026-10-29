package co.impresion3d.costos.repository;

import co.impresion3d.costos.entity.Filamento;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface FilamentoRepositorio extends JpaRepository<Filamento, Long> {

    @Query("select f from Filamento f order by f.material, f.color")
    List<Filamento> listarOrdenados();
}
