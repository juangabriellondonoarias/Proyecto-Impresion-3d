package co.impresion3d.costos.documento;

import co.impresion3d.costos.dto.ResumenCotizacion;
import co.impresion3d.costos.entity.Cotizacion;
import co.impresion3d.costos.entity.ItemCotizacion;
import co.impresion3d.costos.enums.EstadoCotizacion;
import org.apache.poi.ss.usermodel.BorderStyle;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * Genera el Excel (.xlsx) de ventas y cotizaciones para la contabilidad.
 * Hojas: Resumen, Ventas, Cotizaciones y Detalle (una fila por producto).
 * Los valores se guardan como números y fechas reales, no como texto, para poder sumar y filtrar.
 */
@Component
public class ExcelCotizacionesGenerador {

    public static final String LINEA_PEDIDO = "Diseño, preparación y envío";

    private static final Map<EstadoCotizacion, String> ESTADOS = Map.of(
            EstadoCotizacion.BORRADOR, "Borrador",
            EstadoCotizacion.ENVIADA, "Enviada",
            EstadoCotizacion.ACEPTADA, "Aceptada",
            EstadoCotizacion.RECHAZADA, "Rechazada",
            EstadoCotizacion.VENDIDA, "Venta registrada");

    /** Estilos del libro; se crean una vez por archivo. */
    private record Estilos(CellStyle encabezado, CellStyle titulo, CellStyle dinero, CellStyle porcentaje,
                           CellStyle fecha, CellStyle entero, CellStyle total, CellStyle totalDinero) {
    }

    /**
     * @param ventas       cotizaciones vendidas en el periodo (por fecha de venta)
     * @param cotizaciones cotizaciones no vendidas emitidas en el periodo
     */
    public byte[] generar(List<Cotizacion> ventas, List<Cotizacion> cotizaciones, LocalDate desde, LocalDate hasta,
                          String nombreNegocio) {
        try (XSSFWorkbook libro = new XSSFWorkbook(); ByteArrayOutputStream salida = new ByteArrayOutputStream()) {
            Estilos e = estilos(libro);
            hojaResumen(libro, e, ventas, cotizaciones, desde, hasta, nombreNegocio);
            hojaVentas(libro, e, ventas);
            hojaCotizaciones(libro, e, cotizaciones);
            hojaDetalle(libro, e, ventas, cotizaciones);
            libro.write(salida);
            return salida.toByteArray();
        } catch (IOException ex) {
            throw new UncheckedIOException("No se pudo generar el Excel", ex);
        }
    }

    private void hojaResumen(XSSFWorkbook libro, Estilos e, List<Cotizacion> ventas, List<Cotizacion> cotizaciones,
                             LocalDate desde, LocalDate hasta, String nombreNegocio) {
        Sheet hoja = libro.createSheet("Resumen");
        hoja.setColumnWidth(0, 34 * 256);
        hoja.setColumnWidth(1, 20 * 256);

        Row r0 = hoja.createRow(0);
        texto(r0, 0, (nombreNegocio == null || nombreNegocio.isBlank() ? "Costos 3D" : nombreNegocio)
                + " · Ventas y cotizaciones", e.titulo());
        Row r1 = hoja.createRow(1);
        texto(r1, 0, "Periodo", null);
        texto(r1, 1, desde == null && hasta == null ? "Todo el historial"
                : "%s a %s".formatted(desde == null ? "inicio" : desde, hasta == null ? "hoy" : hasta), null);

        double ingresos = suma(ventas, ResumenCotizacion::total);
        double costos = suma(ventas, ResumenCotizacion::costoTotal);
        List<Cotizacion> abiertas = cotizaciones.stream()
                .filter(c -> c.getEstado() != EstadoCotizacion.RECHAZADA).toList();

        int fila = 3;
        Row encabezado = hoja.createRow(fila++);
        texto(encabezado, 0, "Ventas registradas", e.encabezado());
        texto(encabezado, 1, "", e.encabezado());
        fila = filaValor(hoja, fila, "Cantidad de ventas", ventas.size(), e.entero());
        fila = filaValor(hoja, fila, "Ingresos", ingresos, e.dinero());
        fila = filaValor(hoja, fila, "Costos", costos, e.dinero());
        fila = filaValor(hoja, fila, "Ganancia", ingresos - costos, e.dinero());
        fila = filaValor(hoja, fila, "Margen", ingresos > 0 ? (ingresos - costos) / ingresos : 0, e.porcentaje());
        fila = filaValor(hoja, fila, "Descuentos otorgados", suma(ventas, ResumenCotizacion::montoDescuento), e.dinero());

        fila++;
        Row encabezado2 = hoja.createRow(fila++);
        texto(encabezado2, 0, "Cotizaciones abiertas (no son ventas)", e.encabezado());
        texto(encabezado2, 1, "", e.encabezado());
        fila = filaValor(hoja, fila, "Cantidad", abiertas.size(), e.entero());
        fila = filaValor(hoja, fila, "Valor posible", suma(abiertas, ResumenCotizacion::total), e.dinero());
        filaValor(hoja, fila, "Ganancia estimada", suma(abiertas, ResumenCotizacion::ganancia), e.dinero());
    }

    private void hojaVentas(XSSFWorkbook libro, Estilos e, List<Cotizacion> ventas) {
        Sheet hoja = libro.createSheet("Ventas");
        String[] columnas = {"Número", "Cliente", "Contacto", "Fecha emisión", "Fecha venta", "Unidades",
                "Subtotal", "Descuento %", "Descuento", "Total", "Costo", "Ganancia", "Margen"};
        int[] anchos = {12, 28, 24, 14, 14, 10, 15, 12, 14, 15, 15, 15, 10};
        encabezados(hoja, e, columnas, anchos);

        int fila = 1;
        for (Cotizacion c : ventas) {
            ResumenCotizacion r = c.getResumen();
            Row row = hoja.createRow(fila++);
            texto(row, 0, c.getNumero(), null);
            texto(row, 1, c.getNombreCliente(), null);
            texto(row, 2, c.getContactoCliente(), null);
            fecha(row, 3, c.getFechaEmision(), e.fecha());
            fecha(row, 4, c.getFechaVenta(), e.fecha());
            numero(row, 5, r.unidades(), e.entero());
            numero(row, 6, r.totalBruto(), e.dinero());
            numero(row, 7, r.descuentoPct() / 100, e.porcentaje());
            numero(row, 8, r.montoDescuento(), e.dinero());
            numero(row, 9, r.total(), e.dinero());
            numero(row, 10, r.costoTotal(), e.dinero());
            numero(row, 11, r.ganancia(), e.dinero());
            numero(row, 12, r.margenPct() / 100, e.porcentaje());
        }
        filaTotales(hoja, e, fila, new int[]{5, 6, 8, 9, 10, 11}, 5);
        finalizar(hoja, columnas.length, fila);
    }

    private void hojaCotizaciones(XSSFWorkbook libro, Estilos e, List<Cotizacion> cotizaciones) {
        Sheet hoja = libro.createSheet("Cotizaciones");
        String[] columnas = {"Número", "Cliente", "Contacto", "Estado", "Fecha emisión", "Válida hasta",
                "Unidades", "Descuento %", "Total", "Costo", "Ganancia", "Margen"};
        int[] anchos = {12, 28, 24, 16, 14, 14, 10, 12, 15, 15, 15, 10};
        encabezados(hoja, e, columnas, anchos);

        int fila = 1;
        for (Cotizacion c : cotizaciones) {
            ResumenCotizacion r = c.getResumen();
            Row row = hoja.createRow(fila++);
            texto(row, 0, c.getNumero(), null);
            texto(row, 1, c.getNombreCliente(), null);
            texto(row, 2, c.getContactoCliente(), null);
            texto(row, 3, ESTADOS.get(c.getEstado()), null);
            fecha(row, 4, c.getFechaEmision(), e.fecha());
            fecha(row, 5, c.getFechaVencimiento(), e.fecha());
            numero(row, 6, r.unidades(), e.entero());
            numero(row, 7, r.descuentoPct() / 100, e.porcentaje());
            numero(row, 8, r.total(), e.dinero());
            numero(row, 9, r.costoTotal(), e.dinero());
            numero(row, 10, r.ganancia(), e.dinero());
            numero(row, 11, r.margenPct() / 100, e.porcentaje());
        }
        finalizar(hoja, columnas.length, fila);
    }

    /** Una fila por producto de cada venta y cotización, más la línea de costos del pedido. */
    private void hojaDetalle(XSSFWorkbook libro, Estilos e, List<Cotizacion> ventas, List<Cotizacion> cotizaciones) {
        Sheet hoja = libro.createSheet("Detalle");
        String[] columnas = {"Número", "Tipo", "Estado", "Cliente", "Producto", "Cantidad",
                "Costo unitario", "Precio unitario", "Costo línea", "Precio línea"};
        int[] anchos = {12, 12, 16, 26, 40, 10, 15, 15, 15, 15};
        encabezados(hoja, e, columnas, anchos);

        int fila = 1;
        for (List<Cotizacion> grupo : List.of(ventas, cotizaciones)) {
            for (Cotizacion c : grupo) {
                String tipo = c.getEstado().esVenta() ? "Venta" : "Cotización";
                for (ItemCotizacion item : c.getItems()) {
                    fila = filaDetalle(hoja, e, fila, c, tipo, item.getDescripcion(), item.getCantidad(),
                            item.getCostoUnitario(), item.getPrecioUnitario());
                }
                ResumenCotizacion r = c.getResumen();
                if (r.precioFijo() > 0 || r.costoFijo() > 0) {
                    fila = filaDetalle(hoja, e, fila, c, tipo, LINEA_PEDIDO, 1, r.costoFijo(), r.precioFijo());
                }
            }
        }
        finalizar(hoja, columnas.length, fila);
    }

    private int filaDetalle(Sheet hoja, Estilos e, int fila, Cotizacion c, String tipo, String producto,
                            int cantidad, double costoUnitario, double precioUnitario) {
        Row row = hoja.createRow(fila);
        texto(row, 0, c.getNumero(), null);
        texto(row, 1, tipo, null);
        texto(row, 2, ESTADOS.get(c.getEstado()), null);
        texto(row, 3, c.getNombreCliente(), null);
        texto(row, 4, producto, null);
        numero(row, 5, cantidad, e.entero());
        numero(row, 6, costoUnitario, e.dinero());
        numero(row, 7, precioUnitario, e.dinero());
        numero(row, 8, costoUnitario * cantidad, e.dinero());
        numero(row, 9, precioUnitario * cantidad, e.dinero());
        return fila + 1;
    }

    // ---------- utilidades ----------

    private static Estilos estilos(XSSFWorkbook libro) {
        short formatoDinero = libro.createDataFormat().getFormat("\"$\" #,##0");
        short formatoPorcentaje = libro.createDataFormat().getFormat("0.0%");
        short formatoFecha = libro.createDataFormat().getFormat("dd/mm/yyyy");
        short formatoEntero = libro.createDataFormat().getFormat("#,##0");

        Font negrita = libro.createFont();
        negrita.setBold(true);
        Font tituloFuente = libro.createFont();
        tituloFuente.setBold(true);
        tituloFuente.setFontHeightInPoints((short) 14);

        CellStyle encabezado = libro.createCellStyle();
        encabezado.setFont(negrita);
        encabezado.setFillForegroundColor(IndexedColors.LIGHT_ORANGE.getIndex());
        encabezado.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        encabezado.setBorderBottom(BorderStyle.THIN);

        CellStyle titulo = libro.createCellStyle();
        titulo.setFont(tituloFuente);

        CellStyle dinero = libro.createCellStyle();
        dinero.setDataFormat(formatoDinero);
        CellStyle porcentaje = libro.createCellStyle();
        porcentaje.setDataFormat(formatoPorcentaje);
        CellStyle fecha = libro.createCellStyle();
        fecha.setDataFormat(formatoFecha);
        CellStyle entero = libro.createCellStyle();
        entero.setDataFormat(formatoEntero);

        CellStyle total = libro.createCellStyle();
        total.setFont(negrita);
        total.setBorderTop(BorderStyle.THIN);
        total.setDataFormat(formatoEntero);
        CellStyle totalDinero = libro.createCellStyle();
        totalDinero.cloneStyleFrom(total);
        totalDinero.setDataFormat(formatoDinero);

        return new Estilos(encabezado, titulo, dinero, porcentaje, fecha, entero, total, totalDinero);
    }

    private static void encabezados(Sheet hoja, Estilos e, String[] columnas, int[] anchos) {
        Row row = hoja.createRow(0);
        for (int i = 0; i < columnas.length; i++) {
            texto(row, i, columnas[i], e.encabezado());
            hoja.setColumnWidth(i, anchos[i] * 256);
        }
        hoja.createFreezePane(0, 1);
    }

    /** Fila «Total» con fórmulas SUMA, para que el total se actualice si se filtra o edita en Excel. */
    private static void filaTotales(Sheet hoja, Estilos e, int fila, int[] columnas, int columnaEntera) {
        if (fila <= 1) {
            return;
        }
        Row row = hoja.createRow(fila);
        texto(row, 0, "Total", e.total());
        for (int col : columnas) {
            Cell celda = row.createCell(col);
            String letra = org.apache.poi.ss.util.CellReference.convertNumToColString(col);
            celda.setCellFormula("SUM(%s2:%s%d)".formatted(letra, letra, fila));
            celda.setCellStyle(col == columnaEntera ? e.total() : e.totalDinero());
        }
    }

    private static void finalizar(Sheet hoja, int columnas, int filas) {
        if (filas > 1) {
            hoja.setAutoFilter(new CellRangeAddress(0, filas - 1, 0, columnas - 1));
        }
    }

    private static int filaValor(Sheet hoja, int fila, String etiqueta, double valor, CellStyle estilo) {
        Row row = hoja.createRow(fila);
        texto(row, 0, etiqueta, null);
        numero(row, 1, valor, estilo);
        return fila + 1;
    }

    private static void texto(Row row, int col, String valor, CellStyle estilo) {
        Cell celda = row.createCell(col);
        celda.setCellValue(valor == null ? "" : valor);
        if (estilo != null) {
            celda.setCellStyle(estilo);
        }
    }

    private static void numero(Row row, int col, double valor, CellStyle estilo) {
        Cell celda = row.createCell(col);
        celda.setCellValue(valor);
        celda.setCellStyle(estilo);
    }

    private static void fecha(Row row, int col, LocalDate valor, CellStyle estilo) {
        Cell celda = row.createCell(col);
        if (valor != null) {
            celda.setCellValue(valor);
            celda.setCellStyle(estilo);
        }
    }

    private static double suma(List<Cotizacion> lista, java.util.function.ToDoubleFunction<ResumenCotizacion> campo) {
        return lista.stream().map(Cotizacion::getResumen).mapToDouble(campo).sum();
    }
}
