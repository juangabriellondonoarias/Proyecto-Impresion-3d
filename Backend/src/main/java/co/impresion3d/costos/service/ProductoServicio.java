package co.impresion3d.costos.service;

import co.impresion3d.costos.dto.SolicitudProducto;
import co.impresion3d.costos.entity.Filamento;
import co.impresion3d.costos.entity.Producto;
import co.impresion3d.costos.exception.RecursoNoEncontradoExcepcion;
import co.impresion3d.costos.mapper.ProductoMapeador;
import co.impresion3d.costos.repository.FilamentoRepositorio;
import co.impresion3d.costos.repository.ProductoRepositorio;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class ProductoServicio {

    private final ProductoRepositorio repositorio;
    private final FilamentoRepositorio filamentos;
    private final ProductoMapeador mapeador;

    public ProductoServicio(ProductoRepositorio repositorio, FilamentoRepositorio filamentos, ProductoMapeador mapeador) {
        this.repositorio = repositorio;
        this.filamentos = filamentos;
        this.mapeador = mapeador;
    }

    public List<Producto> listar() {
        return repositorio.listarOrdenados();
    }

    public Producto obtener(Long id) {
        return repositorio.findById(id).orElseThrow(() -> new RecursoNoEncontradoExcepcion("El producto"));
    }

    @Transactional
    public Producto crear(SolicitudProducto solicitud) {
        return repositorio.save(mapeador.aplicar(new Producto(), solicitud, filamento(solicitud.filamentoId())));
    }

    @Transactional
    public Producto actualizar(Long id, SolicitudProducto solicitud) {
        return mapeador.aplicar(obtener(id), solicitud, filamento(solicitud.filamentoId()));
    }

    @Transactional
    public void eliminar(Long id) {
        repositorio.delete(obtener(id));
    }

    private Filamento filamento(Long id) {
        return id == null ? null
                : filamentos.findById(id).orElseThrow(() -> new RecursoNoEncontradoExcepcion("El filamento seleccionado"));
    }
}
