package co.impresion3d.costos.service;

import co.impresion3d.costos.entity.Configuracion;
import co.impresion3d.costos.repository.ConfiguracionRepositorio;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ConfiguracionServicio {

    private final ConfiguracionRepositorio repositorio;

    public ConfiguracionServicio(ConfiguracionRepositorio repositorio) {
        this.repositorio = repositorio;
    }

    /** La configuración guardada, o una vacía si todavía no existe. */
    public Configuracion obtener() {
        return repositorio.findById(Configuracion.ID_UNICO).orElseGet(Configuracion::new);
    }

    @Transactional
    public Configuracion guardar(Configuracion configuracion) {
        configuracion.setId(Configuracion.ID_UNICO);
        return repositorio.save(configuracion);
    }
}
