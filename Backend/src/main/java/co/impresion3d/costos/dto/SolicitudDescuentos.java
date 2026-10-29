package co.impresion3d.costos.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import java.util.List;

/**
 * Comparador de descuentos por mayor para un producto del catálogo.
 * Los campos opcionales vacíos toman el valor de la configuración (márgenes) o 0 (costos del pedido).
 */
public record SolicitudDescuentos(
        @NotNull(message = "Elige un producto")
        Long productoId,

        @NotNull(message = "Indica la cantidad")
        @Min(value = 1, message = "La cantidad debe ser al menos 1")
        Integer cantidad,

        /** Cantidad con la que se fija el precio de lista (normalmente 1 unidad). */
        @Min(value = 1, message = "La cantidad de referencia debe ser al menos 1")
        Integer cantidadReferencia,

        @PositiveOrZero(message = "El margen no puede ser negativo")
        @DecimalMax(value = "95", message = "El margen debe ser menor o igual a 95 %")
        Double margenPct,

        /** Margen que ningún descuento puede vulnerar. */
        @PositiveOrZero(message = "El margen mínimo no puede ser negativo")
        @DecimalMax(value = "95", message = "El margen mínimo debe ser menor o igual a 95 %")
        Double margenMinimoPct,

        @Size(max = 12, message = "Compara como máximo 12 descuentos")
        List<@NotNull @PositiveOrZero(message = "El descuento no puede ser negativo")
             @DecimalMax(value = "100", message = "El descuento no puede superar 100 %") Double> descuentos,

        @PositiveOrZero(message = "El diseño no puede ser negativo")
        Double costoDiseno,

        @PositiveOrZero(message = "El envío no puede ser negativo")
        Double costoEnvio,

        @PositiveOrZero(message = "Otros costos no pueden ser negativos")
        Double otrosCostos,

        @PositiveOrZero(message = "La preparación no puede ser negativa")
        Double tiempoPreparacionMin
) {
}
