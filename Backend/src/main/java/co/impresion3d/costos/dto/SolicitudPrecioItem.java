package co.impresion3d.costos.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

/** Pide el costo y el precio de un producto del catálogo para cierta cantidad. */
public record SolicitudPrecioItem(
        @NotNull(message = "Elige un producto")
        Long productoId,

        @NotNull(message = "Indica la cantidad")
        @Min(value = 1, message = "La cantidad debe ser al menos 1")
        Integer cantidad,

        @PositiveOrZero(message = "El margen no puede ser negativo")
        @DecimalMax(value = "95", message = "El margen debe ser menor o igual a 95 %")
        Double margenPct
) {
}
