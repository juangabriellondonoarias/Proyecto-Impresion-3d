package co.impresion3d.costos.dto;

import java.util.List;

/**
 * Comparación de descuentos para una cantidad. Dinero en COP, porcentajes de 0 a 100.
 * Todos los descuentos se aplican sobre el precio de lista (el precio para la cantidad de referencia).
 */
public record ResultadoDescuentos(
        String producto,
        int cantidad,
        int cantidadReferencia,
        double margenPct,
        double margenMinimoPct,

        /** Precio por unidad cuando se compra la cantidad de referencia. */
        double precioLista,
        /** Costo real por unidad para la cantidad pedida (con los costos del pedido repartidos). */
        double costoUnitario,
        /** Precio más bajo que respeta el margen mínimo para la cantidad pedida. */
        double precioMinimo,
        /** Precio recomendado con el margen deseado para la cantidad pedida. */
        double precioSugerido,
        double descuentoSugeridoPct,
        /** Descuento más alto sobre el precio de lista que respeta el margen mínimo. */
        double descuentoMaximoPct,

        List<Opcion> opciones,
        List<Escala> escala,
        List<String> advertencias
) {

    /** Una fila de la tabla de descuentos. */
    public record Opcion(
            double descuentoPct,
            double precioUnitario,
            double totalPedido,
            double gananciaTotal,
            double margenPct,
            double ahorroCliente,
            boolean permitido
    ) {
    }

    /** Cómo cambian costo y descuento admisible con la cantidad. */
    public record Escala(
            int cantidad,
            double costoUnitario,
            double precioSugerido,
            double precioMinimo,
            double descuentoMaximoPct
    ) {
    }
}
