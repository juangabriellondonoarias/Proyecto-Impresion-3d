package co.impresion3d.costos.dto;

import java.time.LocalDate;
import java.util.List;

/**
 * Análisis de rentabilidad de un periodo. Dinero en COP, porcentajes de 0 a 100.
 * Las ventas son solo cotizaciones registradas como VENDIDA dentro del periodo (por fecha de venta);
 * las cotizaciones abiertas son ingresos posibles y nunca se suman a las ventas.
 */
public record ResultadoAnalisis(
        LocalDate desde,
        LocalDate hasta,
        Ventas ventas,
        Abiertas abiertas,
        int rechazadas,
        /** Ventas / (ventas + rechazadas) entre las cotizaciones emitidas en el periodo. */
        double tasaCierrePct,
        List<Mes> ventasPorMes,
        List<ProductoRentable> productos,
        List<Descuento> descuentos
) {

    public record Ventas(
            int cantidad,
            double ingresos,
            double costos,
            double costoProductos,
            double costoPedido,
            double ganancia,
            double margenPct,
            double ticketPromedio,
            double descuentosOtorgados,
            double descuentoPromedioPct
    ) {
    }

    public record Abiertas(
            int cantidad,
            double valor,
            double gananciaEstimada,
            double margenPct,
            double descuentosOfrecidos,
            int bajoMargenMinimo
    ) {
    }

    /** Ventas de un mes. «mes» es 2026-10; «etiqueta» es «oct 2026». */
    public record Mes(String mes, String etiqueta, int ventas, double ingresos, double costos, double ganancia) {
    }

    /** Lo vendido de un producto, con el descuento de cada venta repartido entre sus líneas. */
    public record ProductoRentable(String nombre, int unidades, double ingresos, double costos, double ganancia,
                                   double margenPct) {
    }

    /** Un descuento dado (venta) u ofrecido (cotización abierta). */
    public record Descuento(Long id, String numero, String cliente, boolean esVenta, String estado,
                            LocalDate fecha, double descuentoPct, double monto, double margenPct,
                            boolean bajoMargenMinimo) {
    }
}
