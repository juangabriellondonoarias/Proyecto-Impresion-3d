package co.impresion3d.costos.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

import java.time.LocalDate;
import java.util.List;

/** Datos que envía el editor de cotizaciones. */
public record SolicitudCotizacion(
        @NotBlank(message = "Indica el nombre del cliente")
        String nombreCliente,

        String contactoCliente,

        @NotNull(message = "Indica la fecha")
        LocalDate fechaEmision,

        @Min(value = 0, message = "La vigencia no puede ser negativa")
        Integer diasVigencia,

        @PositiveOrZero(message = "El margen no puede ser negativo")
        @DecimalMax(value = "95", message = "El margen debe ser menor o igual a 95 %")
        Double margenPct,

        @PositiveOrZero(message = "El descuento no puede ser negativo")
        @DecimalMax(value = "100", message = "El descuento no puede superar 100 %")
        Double descuentoPct,

        @PositiveOrZero(message = "El diseño no puede ser negativo")
        Double costoDiseno,

        @PositiveOrZero(message = "El envío no puede ser negativo")
        Double costoEnvio,

        @PositiveOrZero(message = "Otros costos no pueden ser negativos")
        Double otrosCostos,

        String condiciones,
        String notas,

        @NotEmpty(message = "Agrega al menos un producto")
        List<@Valid Item> items
) {

    public record Item(
            Long productoId,
            Long filamentoId,

            @NotBlank(message = "Describe el producto")
            String descripcion,

            @NotNull(message = "Indica la cantidad")
            @Min(value = 1, message = "La cantidad debe ser al menos 1")
            Integer cantidad,

            @PositiveOrZero(message = "El costo no puede ser negativo")
            Double costoUnitario,

            @PositiveOrZero(message = "El precio no puede ser negativo")
            Double precioUnitario,

            @PositiveOrZero(message = "Los gramos no pueden ser negativos")
            Double gramosMaterialPorUnidad
    ) {
    }
}
