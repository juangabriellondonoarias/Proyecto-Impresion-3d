package co.impresion3d.costos.mapper;

import co.impresion3d.costos.dto.SolicitudPrecio;
import co.impresion3d.costos.entity.Configuracion;
import co.impresion3d.costos.entity.Filamento;
import co.impresion3d.costos.entity.Producto;
import org.springframework.stereotype.Component;

/**
 * Arma la solicitud de la calculadora para un producto del catálogo, con la configuración
 * del negocio y los costos del pedido. La usan las cotizaciones y los descuentos por mayor,
 * para que todos calculen igual que la calculadora.
 */
@Component
public class PrecioProductoMapeador {

    /** Costos que se cobran una vez por pedido (no por unidad). */
    public record CostosPedido(double costoDiseno, double costoEnvio, double otrosCostos, double tiempoPreparacionMin) {
        public static final CostosPedido NINGUNO = new CostosPedido(0, 0, 0, 0);
    }

    public SolicitudPrecio aSolicitud(Producto producto, Configuracion c, int cantidad, double margenPct,
                                      double margenMinimoPct, CostosPedido pedido) {
        Filamento f = producto.getFilamento();
        return new SolicitudPrecio(
                producto.getNombre(), producto.getReferencia(),
                f == null ? null : f.getMaterial(), f == null ? null : f.getColor(),
                f == null ? 0.0 : f.getCostoPorKg(),
                cero(producto.getPesoG()), producto.getDesperdicioPorUnidadG(), 0.0, cantidad,
                producto.getTiempoImpresionMinPorUnidad(), 0.0, pedido.tiempoPreparacionMin(),
                producto.getPosprocesoMinPorUnidad(), 0.0,
                c.getPotenciaImpresoraW(), c.getPrecioKwh(), c.getValorHoraManoObra(), c.getCostoMaquinaPorHora(),
                pedido.costoDiseno(), producto.getEmpaquePorUnidad(), pedido.costoEnvio(), pedido.otrosCostos(),
                margenPct, 0.0, margenMinimoPct, c.getPasoRedondeo());
    }

    /** «Soporte para celular · PLA Negro» */
    public String descripcion(Producto producto) {
        Filamento f = producto.getFilamento();
        return producto.getNombre() + (f == null ? "" : " · " + f.getMaterial() + " " + f.getColor());
    }

    private static double cero(Double valor) {
        return valor == null ? 0 : valor;
    }
}
