package co.impresion3d.costos.mapper;

import co.impresion3d.costos.dto.SolicitudProducto;
import co.impresion3d.costos.entity.Filamento;
import co.impresion3d.costos.entity.Producto;
import org.springframework.stereotype.Component;

@Component
public class ProductoMapeador {

    /** Copia los datos del formulario sobre la entidad. El filamento ya viene resuelto. */
    public Producto aplicar(Producto producto, SolicitudProducto solicitud, Filamento filamento) {
        producto.setNombre(solicitud.nombre().trim());
        producto.setReferencia(solicitud.referencia() == null || solicitud.referencia().isBlank()
                ? null : solicitud.referencia().trim());
        producto.setFilamento(filamento);
        producto.setPesoG(solicitud.pesoG());
        producto.setDesperdicioPorUnidadG(solicitud.desperdicioPorUnidadG());
        producto.setTiempoImpresionMinPorUnidad(solicitud.tiempoImpresionMinPorUnidad());
        producto.setPosprocesoMinPorUnidad(solicitud.posprocesoMinPorUnidad());
        producto.setEmpaquePorUnidad(solicitud.empaquePorUnidad());
        producto.setNotas(solicitud.notas());
        return producto;
    }
}
