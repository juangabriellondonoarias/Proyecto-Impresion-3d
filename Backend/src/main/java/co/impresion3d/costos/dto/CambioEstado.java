package co.impresion3d.costos.dto;

import co.impresion3d.costos.enums.EstadoCotizacion;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

/** Cambio de estado de una cotización. La fecha solo se usa al registrar la venta. */
public record CambioEstado(
        @NotNull(message = "Indica el estado")
        EstadoCotizacion estado,

        LocalDate fechaVenta
) {
}
