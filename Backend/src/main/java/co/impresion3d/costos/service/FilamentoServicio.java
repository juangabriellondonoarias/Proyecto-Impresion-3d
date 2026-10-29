package co.impresion3d.costos.service;

import co.impresion3d.costos.dto.SolicitudFilamento;
import co.impresion3d.costos.entity.Filamento;
import co.impresion3d.costos.exception.ConflictoExcepcion;
import co.impresion3d.costos.exception.RecursoNoEncontradoExcepcion;
import co.impresion3d.costos.mapper.FilamentoMapeador;
import co.impresion3d.costos.repository.FilamentoRepositorio;
import co.impresion3d.costos.repository.ProductoRepositorio;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class FilamentoServicio {

    private final FilamentoRepositorio repositorio;
    private final ProductoRepositorio productos;
    private final FilamentoMapeador mapeador;

    public FilamentoServicio(FilamentoRepositorio repositorio, ProductoRepositorio productos, FilamentoMapeador mapeador) {
        this.repositorio = repositorio;
        this.productos = productos;
        this.mapeador = mapeador;
    }

    public List<Filamento> listar() {
        return repositorio.listarOrdenados();
    }

    public Filamento obtener(Long id) {
        return repositorio.findById(id).orElseThrow(() -> new RecursoNoEncontradoExcepcion("El filamento"));
    }

    @Transactional
    public Filamento crear(SolicitudFilamento solicitud) {
        validarPesos(solicitud);
        return repositorio.save(mapeador.aplicar(new Filamento(), solicitud));
    }

    @Transactional
    public Filamento actualizar(Long id, SolicitudFilamento solicitud) {
        validarPesos(solicitud);
        return mapeador.aplicar(obtener(id), solicitud);
    }

    @Transactional
    public void eliminar(Long id) {
        Filamento filamento = obtener(id);
        long enUso = productos.contarPorFilamento(id);
        if (enUso > 0) {
            throw new ConflictoExcepcion("No se puede eliminar: el filamento está asignado a " + enUso
                    + " producto(s). Cámbialo en esos productos primero.");
        }
        repositorio.delete(filamento);
    }

    private static void validarPesos(SolicitudFilamento s) {
        if (s.pesoDisponibleG() > s.pesoInicialG()) {
            throw new ConflictoExcepcion("El peso disponible no puede ser mayor que el peso inicial.");
        }
    }
}
