package co.impresion3d.costos.dto;

/** Costo y precio unitario de un producto del catálogo, listo para una línea de cotización. */
public record PrecioItem(
        Long productoId,
        Long filamentoId,
        String descripcion,
        int cantidad,
        double costoUnitario,
        double precioUnitario,
        double gramosMaterialPorUnidad
) {
}
