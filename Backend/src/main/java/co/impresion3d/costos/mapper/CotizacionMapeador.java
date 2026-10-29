package co.impresion3d.costos.mapper;

import co.impresion3d.costos.dto.SolicitudCotizacion;
import co.impresion3d.costos.entity.Cotizacion;
import co.impresion3d.costos.entity.ItemCotizacion;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Copia los datos del editor sobre la cotización. Tolera datos incompletos (nulos)
 * porque la vista previa de totales se pide mientras el usuario todavía escribe.
 */
@Component
public class CotizacionMapeador {

    public Cotizacion aplicar(Cotizacion cotizacion, SolicitudCotizacion s) {
        cotizacion.setNombreCliente(s.nombreCliente() == null ? null : s.nombreCliente().trim());
        cotizacion.setContactoCliente(s.contactoCliente());
        cotizacion.setFechaEmision(s.fechaEmision());
        cotizacion.setDiasVigencia(s.diasVigencia() == null ? 0 : s.diasVigencia());
        cotizacion.setMargenPct(cero(s.margenPct()));
        cotizacion.setDescuentoPct(cero(s.descuentoPct()));
        cotizacion.setCostoDiseno(cero(s.costoDiseno()));
        cotizacion.setCostoEnvio(cero(s.costoEnvio()));
        cotizacion.setOtrosCostos(cero(s.otrosCostos()));
        cotizacion.setCondiciones(s.condiciones());
        cotizacion.setNotas(s.notas());

        // Se reemplazan todas las líneas; orphanRemoval borra las anteriores.
        cotizacion.getItems().clear();
        for (SolicitudCotizacion.Item entrada : s.items() == null ? List.<SolicitudCotizacion.Item>of() : s.items()) {
            cotizacion.getItems().add(aItem(entrada));
        }
        return cotizacion;
    }

    private ItemCotizacion aItem(SolicitudCotizacion.Item entrada) {
        ItemCotizacion item = new ItemCotizacion();
        item.setProductoId(entrada.productoId());
        item.setFilamentoId(entrada.filamentoId());
        item.setDescripcion(entrada.descripcion() == null ? "" : entrada.descripcion().trim());
        item.setCantidad(entrada.cantidad() == null ? 0 : Math.max(0, entrada.cantidad()));
        item.setCostoUnitario(cero(entrada.costoUnitario()));
        item.setPrecioUnitario(cero(entrada.precioUnitario()));
        item.setGramosMaterialPorUnidad(cero(entrada.gramosMaterialPorUnidad()));
        return item;
    }

    private static double cero(Double valor) {
        return valor == null ? 0 : valor;
    }
}
