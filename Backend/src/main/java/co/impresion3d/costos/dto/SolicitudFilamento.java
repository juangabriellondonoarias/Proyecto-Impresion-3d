package co.impresion3d.costos.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;

/** Datos que envía el formulario de filamentos. */
public record SolicitudFilamento(
        @NotBlank(message = "Indica el material")
        String material,

        @NotBlank(message = "Indica el color")
        String color,

        String marca,

        @NotNull(message = "Indica el precio de compra")
        @PositiveOrZero(message = "El precio no puede ser negativo")
        Double precioCompra,

        @NotNull(message = "Indica el peso inicial del rollo")
        @Positive(message = "El peso inicial debe ser mayor que 0")
        Double pesoInicialG,

        @NotNull(message = "Indica el peso disponible")
        @PositiveOrZero(message = "El peso disponible no puede ser negativo")
        Double pesoDisponibleG,

        String notas
) {
}
