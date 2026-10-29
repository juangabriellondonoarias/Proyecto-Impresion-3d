package co.impresion3d.costos.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

/**
 * Datos de entrada de la calculadora. Tiempos en minutos, pesos en gramos, dinero en COP.
 * Los campos opcionales nulos se tratan como 0.
 */
public record SolicitudPrecio(
        String nombreProducto,
        String referencia,
        String material,
        String color,

        @NotNull(message = "Indica el precio del filamento por kg")
        @PositiveOrZero(message = "El precio del filamento no puede ser negativo")
        Double precioFilamentoKg,

        @NotNull(message = "Indica el peso de la pieza")
        @PositiveOrZero(message = "El peso no puede ser negativo")
        Double pesoPiezaG,

        @PositiveOrZero(message = "El desperdicio por unidad no puede ser negativo")
        Double desperdicioPorUnidadG,

        @PositiveOrZero(message = "El desperdicio por lote no puede ser negativo")
        Double desperdicioPorLoteG,

        @NotNull(message = "Indica la cantidad de unidades")
        @Min(value = 1, message = "La cantidad debe ser al menos 1")
        Integer cantidad,

        @PositiveOrZero(message = "El tiempo de impresión no puede ser negativo")
        Double tiempoImpresionPorUnidadMin,

        /** Tiempo real del lote completo. Si es 0 se usa tiempo por unidad × cantidad. */
        @PositiveOrZero(message = "El tiempo del lote no puede ser negativo")
        Double tiempoImpresionLoteMin,

        @PositiveOrZero(message = "El tiempo de preparación no puede ser negativo")
        Double tiempoPreparacionMin,

        @PositiveOrZero(message = "El posprocesado por unidad no puede ser negativo")
        Double posprocesoPorUnidadMin,

        @PositiveOrZero(message = "El posprocesado por lote no puede ser negativo")
        Double posprocesoPorLoteMin,

        @PositiveOrZero(message = "La potencia no puede ser negativa")
        Double potenciaImpresoraW,

        @PositiveOrZero(message = "El precio del kWh no puede ser negativo")
        Double precioKwh,

        @PositiveOrZero(message = "La mano de obra no puede ser negativa")
        Double valorHoraManoObra,

        /** Mantenimiento + depreciación de la impresora por hora de máquina. */
        @PositiveOrZero(message = "El costo de máquina no puede ser negativo")
        Double costoMaquinaPorHora,

        @PositiveOrZero(message = "El costo de diseño no puede ser negativo")
        Double costoDiseno,

        @PositiveOrZero(message = "El empaque no puede ser negativo")
        Double empaquePorUnidad,

        @PositiveOrZero(message = "El envío no puede ser negativo")
        Double costoEnvio,

        @PositiveOrZero(message = "Otros costos no pueden ser negativos")
        Double otrosCostos,

        @NotNull(message = "Indica el margen deseado")
        @DecimalMin(value = "0", message = "El margen no puede ser negativo")
        @DecimalMax(value = "95", message = "El margen debe ser menor o igual a 95 %")
        Double margenObjetivoPct,

        @DecimalMin(value = "0", message = "El descuento no puede ser negativo")
        @DecimalMax(value = "100", message = "El descuento no puede superar 100 %")
        Double descuentoPct,

        @DecimalMin(value = "0", message = "El margen mínimo no puede ser negativo")
        @DecimalMax(value = "95", message = "El margen mínimo debe ser menor o igual a 95 %")
        Double margenMinimoPct,

        /** Redondeo hacia arriba del precio recomendado (p. ej. 100 COP). 0 = sin redondeo. */
        @PositiveOrZero(message = "El redondeo no puede ser negativo")
        Double pasoRedondeo
) {
}
