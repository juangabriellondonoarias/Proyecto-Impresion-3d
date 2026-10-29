package co.impresion3d.costos.dto;

import java.util.List;

/** Resultado de la calculadora. Dinero en COP, porcentajes de 0 a 100. */
public record ResultadoPrecio(
        int cantidad,

        double gramosMaterialPorUnidad,
        double gramosMaterialLote,
        double costoMaterialPorUnidad,
        double costoMaterialLote,

        double horasMaquina,
        double horasHumanas,

        double costoElectrico,
        double costoManoObra,
        double costoMaquina,

        double costoDiseno,
        double costoEmpaque,
        double costoEnvio,
        double otrosCostos,
        double totalOtrosCostos,

        double costoTotal,
        double costoUnitario,

        double precioUnitarioRecomendado,
        double precioLoteRecomendado,
        double gananciaPorUnidad,
        double gananciaLote,
        double margenRealPct,

        double margenMinimoPct,
        double precioUnitarioMinimo,
        double descuentoMaximoPct,

        double descuentoPct,
        double precioUnitarioFinal,
        double precioLoteFinal,
        double gananciaFinalPorUnidad,
        double gananciaFinalLote,
        double margenFinalPct,
        boolean descuentoExcedeMaximo,

        List<String> advertencias
) {
}
