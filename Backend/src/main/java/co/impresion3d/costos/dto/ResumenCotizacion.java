package co.impresion3d.costos.dto;

import co.impresion3d.costos.entity.ItemCotizacion;

import java.util.ArrayList;
import java.util.List;

/**
 * Totales de una cotización. Dinero en COP, porcentajes de 0 a 100.
 *
 * Los costos del pedido (diseño, envío, otros) se cobran una sola vez como línea aparte,
 * con el margen de la cotización. El descuento se aplica sobre el total bruto.
 */
public record ResumenCotizacion(
        int unidades,
        double costoProductos,
        double precioProductos,
        double costoFijo,
        double precioFijo,
        double totalBruto,
        double descuentoPct,
        double montoDescuento,
        double total,
        double costoTotal,
        double ganancia,
        double margenPct,
        double margenMinimoPct,
        double descuentoMaximoPct,
        boolean bajoMargenMinimo,
        List<String> advertencias
) {

    public static ResumenCotizacion de(List<ItemCotizacion> items, double costoDiseno, double costoEnvio,
                                       double otrosCostos, double margenPct, double margenMinimoPct,
                                       double descuentoPct) {
        List<String> advertencias = new ArrayList<>();
        int unidades = items.stream().mapToInt(ItemCotizacion::getCantidad).sum();
        double costoProductos = items.stream().mapToDouble(ItemCotizacion::getCostoLinea).sum();
        double precioProductos = items.stream().mapToDouble(ItemCotizacion::getPrecioLinea).sum();

        double margen = limitar(margenPct, 0, 95) / 100;
        double margenMinimo = limitar(margenMinimoPct, 0, 95) / 100;
        double costoFijo = noNegativo(costoDiseno) + noNegativo(costoEnvio) + noNegativo(otrosCostos);
        double precioFijo = Math.round(costoFijo / (1 - margen));

        double bruto = precioProductos + precioFijo;
        double descuento = limitar(descuentoPct, 0, 100);
        double montoDescuento = Math.round(bruto * descuento / 100);
        double total = bruto - montoDescuento;
        double costoTotal = costoProductos + costoFijo;
        double ganancia = total - costoTotal;
        double margenReal = total > 0 ? ganancia / total * 100 : 0;

        // Descuento máximo para no bajar del margen mínimo: total >= costo / (1 - margen mínimo).
        double totalMinimo = costoTotal / (1 - margenMinimo);
        double descuentoMaximo = bruto > 0 ? Math.max(0, Math.floor((1 - totalMinimo / bruto) * 1000) / 10.0) : 0;

        boolean bajoMinimo = total > 0 && margenReal < margenMinimo * 100 - 1e-9;
        if (ganancia < 0) {
            advertencias.add("La cotización deja pérdida: el total es menor que el costo.");
        } else if (bajoMinimo) {
            advertencias.add("El margen real está por debajo del margen mínimo configurado.");
        }
        for (ItemCotizacion item : items) {
            if (item.getPrecioUnitario() < item.getCostoUnitario()) {
                advertencias.add("«" + item.getDescripcion() + "» tiene un precio por debajo de su costo.");
            }
        }

        return new ResumenCotizacion(unidades, costoProductos, precioProductos, costoFijo, precioFijo, bruto,
                descuento, montoDescuento, total, costoTotal, ganancia, margenReal,
                margenMinimo * 100, descuentoMaximo, bajoMinimo, advertencias);
    }

    private static double limitar(double valor, double minimo, double maximo) {
        return Double.isNaN(valor) ? minimo : Math.min(maximo, Math.max(minimo, valor));
    }

    private static double noNegativo(double valor) {
        return Double.isNaN(valor) || valor < 0 ? 0 : valor;
    }
}
