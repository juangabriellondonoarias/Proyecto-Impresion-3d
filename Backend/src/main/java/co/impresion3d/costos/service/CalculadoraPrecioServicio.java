package co.impresion3d.costos.service;

import co.impresion3d.costos.dto.ResultadoPrecio;
import co.impresion3d.costos.dto.SolicitudPrecio;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * Motor de precios. Todas las fórmulas viven aquí para que la calculadora, las cotizaciones
 * y los descuentos por mayor usen exactamente el mismo cálculo.
 *
 * Margen sobre ventas: margen = ganancia / precio, por lo tanto precio = costo / (1 - margen).
 * Los costos fijos del pedido (preparación, diseño, envío, otros) se suman al costo total
 * y se reparten entre las unidades del lote; no se cuentan dos veces.
 */
@Service
public class CalculadoraPrecioServicio {

    public ResultadoPrecio calcular(SolicitudPrecio s) {
        List<String> advertencias = new ArrayList<>();
        int cantidad = s.cantidad() == null || s.cantidad() < 1 ? 1 : s.cantidad();

        // Material: peso + desperdicio por unidad, más purgas del lote completo.
        double gramosPorUnidad = n(s.pesoPiezaG()) + n(s.desperdicioPorUnidadG());
        double gramosLote = gramosPorUnidad * cantidad + n(s.desperdicioPorLoteG());
        double costoMaterialLote = gramosLote / 1000.0 * n(s.precioFilamentoKg());

        // Tiempo de máquina: el tiempo real del lote manda si se indica.
        double minutosMaquina = n(s.tiempoImpresionLoteMin()) > 0
                ? n(s.tiempoImpresionLoteMin())
                : n(s.tiempoImpresionPorUnidadMin()) * cantidad;
        double horasMaquina = minutosMaquina / 60.0;
        if (minutosMaquina == 0) {
            advertencias.add("Sin tiempo de impresión: no se calcula costo eléctrico ni de máquina.");
        }

        double costoElectrico = n(s.potenciaImpresoraW()) / 1000.0 * horasMaquina * n(s.precioKwh());
        double costoMaquina = horasMaquina * n(s.costoMaquinaPorHora());

        // Trabajo humano: preparación del pedido + posprocesado por unidad y por lote.
        double minutosHumanos = n(s.tiempoPreparacionMin())
                + n(s.posprocesoPorUnidadMin()) * cantidad
                + n(s.posprocesoPorLoteMin());
        double horasHumanas = minutosHumanos / 60.0;
        double costoManoObra = horasHumanas * n(s.valorHoraManoObra());

        double costoDiseno = n(s.costoDiseno());
        double costoEmpaque = n(s.empaquePorUnidad()) * cantidad;
        double costoEnvio = n(s.costoEnvio());
        double otrosCostos = n(s.otrosCostos());
        double totalOtros = costoDiseno + costoEmpaque + costoEnvio + otrosCostos;

        double costoTotal = costoMaterialLote + costoElectrico + costoManoObra + costoMaquina + totalOtros;
        double costoUnitario = costoTotal / cantidad;

        double margenObjetivo = limitarMargen(n(s.margenObjetivoPct())) / 100.0;
        double margenMinimo = limitarMargen(n(s.margenMinimoPct())) / 100.0;
        double paso = n(s.pasoRedondeo());

        double recomendado = redondearArriba(costoUnitario / (1 - margenObjetivo), paso);
        double gananciaUnidad = recomendado - costoUnitario;
        double minimo = redondearArriba(costoUnitario / (1 - margenMinimo), paso);
        if (margenMinimo > margenObjetivo) {
            advertencias.add("El margen deseado es menor que el margen mínimo configurado.");
        }

        double descuentoMaximo = recomendado > 0
                ? Math.max(0, Math.floor((1 - minimo / recomendado) * 1000) / 10.0)
                : 0;

        double descuento = Math.min(100, Math.max(0, n(s.descuentoPct())));
        double precioFinal = Math.round(recomendado * (1 - descuento / 100.0));
        double gananciaFinalUnidad = precioFinal - costoUnitario;
        boolean excede = descuento > descuentoMaximo;
        if (excede) {
            advertencias.add(precioFinal < costoUnitario
                    ? "Con este descuento vendes por debajo del costo."
                    : "El descuento supera el máximo permitido por el margen mínimo.");
        }
        if (costoTotal == 0) {
            advertencias.add("Todos los costos están en 0: revisa los datos.");
        }

        return new ResultadoPrecio(
                cantidad,
                gramosPorUnidad, gramosLote,
                costoMaterialLote / cantidad, costoMaterialLote,
                horasMaquina, horasHumanas,
                costoElectrico, costoManoObra, costoMaquina,
                costoDiseno, costoEmpaque, costoEnvio, otrosCostos, totalOtros,
                costoTotal, costoUnitario,
                recomendado, recomendado * cantidad,
                gananciaUnidad, gananciaUnidad * cantidad,
                margenPct(gananciaUnidad, recomendado),
                margenMinimo * 100, minimo, descuentoMaximo,
                descuento, precioFinal, precioFinal * cantidad,
                gananciaFinalUnidad, gananciaFinalUnidad * cantidad,
                margenPct(gananciaFinalUnidad, precioFinal),
                excede,
                advertencias);
    }

    private static double n(Double valor) {
        return valor == null || valor.isNaN() || valor < 0 ? 0 : valor;
    }

    /** Un margen de 100 % haría infinito el precio; se limita a 95 % como la validación. */
    private static double limitarMargen(double pct) {
        return Math.min(95, Math.max(0, pct));
    }

    private static double redondearArriba(double valor, double paso) {
        if (paso <= 0) {
            return Math.round(valor);
        }
        // Se resta un épsilon para que 18400.0000001 no salte a 18500.
        return Math.ceil(valor / paso - 1e-9) * paso;
    }

    private static double margenPct(double ganancia, double precio) {
        return precio > 0 ? ganancia / precio * 100 : 0;
    }
}
