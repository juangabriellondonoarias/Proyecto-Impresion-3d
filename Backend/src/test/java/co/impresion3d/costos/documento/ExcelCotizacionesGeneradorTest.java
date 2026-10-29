package co.impresion3d.costos.documento;

import co.impresion3d.costos.entity.Cotizacion;
import co.impresion3d.costos.entity.ItemCotizacion;
import co.impresion3d.costos.enums.EstadoCotizacion;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.DateUtil;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Venta: 10 × P1 (costo 6.000, precio 10.000) + envío 15.000 cobrado a 25.000, descuento 10 %
 *   -> bruto 125.000; descuento 12.500; total 112.500; costo 75.000; ganancia 37.500
 * Cotización enviada: 3 × P1, sin descuento -> total 30.000
 */
class ExcelCotizacionesGeneradorTest {

    private static Cotizacion cotizacion(long id, EstadoCotizacion estado, LocalDate venta, double envio,
                                         double descuento, int cantidad) {
        ItemCotizacion item = new ItemCotizacion();
        item.setDescripcion("P1");
        item.setCantidad(cantidad);
        item.setCostoUnitario(6000);
        item.setPrecioUnitario(10000);
        Cotizacion c = new Cotizacion();
        c.setId(id);
        c.setNumero("COT-%04d".formatted(id));
        c.setNombreCliente("Cliente " + id);
        c.setEstado(estado);
        c.setFechaEmision(LocalDate.of(2026, 10, 10));
        c.setDiasVigencia(15);
        c.setFechaVenta(venta);
        c.setMargenPct(40);
        c.setMargenMinimoPct(20);
        c.setCostoEnvio(envio);
        c.setDescuentoPct(descuento);
        c.setItems(List.of(item));
        return c;
    }

    private XSSFWorkbook generar() throws IOException {
        Cotizacion venta = cotizacion(1, EstadoCotizacion.VENDIDA, LocalDate.of(2026, 10, 15), 15000, 10, 10);
        Cotizacion abierta = cotizacion(2, EstadoCotizacion.ENVIADA, null, 0, 0, 3);
        byte[] archivo = new ExcelCotizacionesGenerador().generar(List.of(venta), List.of(abierta),
                LocalDate.of(2026, 10, 1), LocalDate.of(2026, 10, 31), "Mi taller 3D");
        return new XSSFWorkbook(new ByteArrayInputStream(archivo));
    }

    @Test
    void tieneLasCuatroHojas() throws IOException {
        try (XSSFWorkbook libro = generar()) {
            assertEquals(4, libro.getNumberOfSheets());
            assertEquals("Resumen", libro.getSheetName(0));
            assertEquals("Ventas", libro.getSheetName(1));
            assertEquals("Cotizaciones", libro.getSheetName(2));
            assertEquals("Detalle", libro.getSheetName(3));
        }
    }

    @Test
    void laVentaTieneNumerosYFechasReales() throws IOException {
        try (XSSFWorkbook libro = generar()) {
            Row fila = libro.getSheet("Ventas").getRow(1);
            assertEquals("COT-0001", fila.getCell(0).getStringCellValue());
            assertTrue(DateUtil.isCellDateFormatted(fila.getCell(4)));
            assertEquals(LocalDate.of(2026, 10, 15), fila.getCell(4).getLocalDateTimeCellValue().toLocalDate());
            assertEquals(125000, fila.getCell(6).getNumericCellValue(), 1e-6); // subtotal
            assertEquals(0.10, fila.getCell(7).getNumericCellValue(), 1e-9);  // descuento %
            assertEquals(12500, fila.getCell(8).getNumericCellValue(), 1e-6);
            assertEquals(112500, fila.getCell(9).getNumericCellValue(), 1e-6); // total
            assertEquals(75000, fila.getCell(10).getNumericCellValue(), 1e-6); // costo
            assertEquals(37500, fila.getCell(11).getNumericCellValue(), 1e-6); // ganancia

            Row totales = libro.getSheet("Ventas").getRow(2);
            assertEquals("Total", totales.getCell(0).getStringCellValue());
            assertEquals(CellType.FORMULA, totales.getCell(9).getCellType());
            assertEquals("SUM(J2:J2)", totales.getCell(9).getCellFormula());
        }
    }

    @Test
    void lasCotizacionesNoSeMezclanConLasVentas() throws IOException {
        try (XSSFWorkbook libro = generar()) {
            Sheet cotizaciones = libro.getSheet("Cotizaciones");
            assertEquals(1, cotizaciones.getLastRowNum());
            assertEquals("COT-0002", cotizaciones.getRow(1).getCell(0).getStringCellValue());
            assertEquals("Enviada", cotizaciones.getRow(1).getCell(3).getStringCellValue());
            assertEquals(30000, cotizaciones.getRow(1).getCell(8).getNumericCellValue(), 1e-6);

            Sheet resumen = libro.getSheet("Resumen");
            assertEquals(1, resumen.getRow(4).getCell(1).getNumericCellValue(), 1e-9);      // ventas
            assertEquals(112500, resumen.getRow(5).getCell(1).getNumericCellValue(), 1e-6); // ingresos
            assertEquals(30000, resumen.getRow(13).getCell(1).getNumericCellValue(), 1e-6); // valor posible
        }
    }

    @Test
    void elDetalleIncluyeLaLineaDeCostosDelPedido() throws IOException {
        try (XSSFWorkbook libro = generar()) {
            Sheet detalle = libro.getSheet("Detalle");
            // venta: P1 + línea del pedido; cotización: P1
            assertEquals(3, detalle.getLastRowNum());
            assertEquals(ExcelCotizacionesGenerador.LINEA_PEDIDO, detalle.getRow(2).getCell(4).getStringCellValue());
            assertEquals(25000, detalle.getRow(2).getCell(9).getNumericCellValue(), 1e-6);
            assertEquals("Cotización", detalle.getRow(3).getCell(1).getStringCellValue());
        }
    }
}
