package co.impresion3d.costos.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

/** Datos que envía el formulario de productos. El filamento se indica por su id. */
public record SolicitudProducto(
        @NotBlank(message = "Indica el nombre del producto")
        String nombre,

        String referencia,

        Long filamentoId,

        @NotNull(message = "Indica el peso de la pieza")
        @PositiveOrZero(message = "El peso no puede ser negativo")
        Double pesoG,

        @PositiveOrZero(message = "El desperdicio no puede ser negativo")
        Double desperdicioPorUnidadG,

        @PositiveOrZero(message = "El tiempo de impresión no puede ser negativo")
        Double tiempoImpresionMinPorUnidad,

        @PositiveOrZero(message = "El posprocesado no puede ser negativo")
        Double posprocesoMinPorUnidad,

        @PositiveOrZero(message = "El empaque no puede ser negativo")
        Double empaquePorUnidad,

        String notas
) {
}
