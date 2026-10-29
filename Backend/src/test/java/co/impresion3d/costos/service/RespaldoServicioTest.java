package co.impresion3d.costos.service;

import co.impresion3d.costos.config.PropiedadesRespaldo;
import co.impresion3d.costos.dto.EstadoRespaldos;
import co.impresion3d.costos.exception.RespaldoExcepcion;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RespaldoServicioTest {

    @TempDir
    Path carpeta;

    private RespaldoServicio servicio(String mysqldump, int conservar) {
        PropiedadesRespaldo p = new PropiedadesRespaldo(true, carpeta.toString(), mysqldump, conservar, null, false);
        return new RespaldoServicio(p, "localhost", "3306", "costos3d", "root", "clave\"con\\raros");
    }

    @Test
    void conservaSoloLasCopiasMasRecientesYNoTocaOtrosArchivos() throws IOException {
        for (String dia : List.of("2026-10-01", "2026-10-02", "2026-10-03", "2026-10-04", "2026-10-05")) {
            Files.writeString(carpeta.resolve("costos3d-" + dia + "_210000.sql"), "-- copia");
        }
        Files.writeString(carpeta.resolve("notas.txt"), "no es una copia");

        RespaldoServicio s = servicio("mysqldump", 3);
        s.aplicarRetencion();

        List<String> quedan = s.listar().stream().map(EstadoRespaldos.Archivo::nombre).toList();
        assertEquals(List.of("costos3d-2026-10-05_210000.sql", "costos3d-2026-10-04_210000.sql",
                "costos3d-2026-10-03_210000.sql"), quedan);
        assertTrue(Files.exists(carpeta.resolve("notas.txt")));
        assertTrue(s.hayRespaldoDe(LocalDate.of(2026, 10, 5)));
        assertFalse(s.hayRespaldoDe(LocalDate.of(2026, 10, 1)));
    }

    @Test
    void sinMysqldumpExplicaQueRevisarYNoDejaArchivosAMedias() throws IOException {
        RespaldoServicio s = servicio("C:/no/existe/mysqldump.exe", 30);
        RespaldoExcepcion error = assertThrows(RespaldoExcepcion.class, s::respaldar);
        assertTrue(error.getMessage().contains("RESPALDO_MYSQLDUMP"), error.getMessage());
        try (Stream<Path> archivos = Files.list(carpeta)) {
            assertEquals(0, archivos.count(), "no debe quedar ningún .parcial ni copia vacía");
        }
    }

    @Test
    void describeElHorario() {
        assertEquals("Todos los días a las 21:00", RespaldoServicio.describirHorario("0 0 21 * * *"));
        assertEquals("Todos los días a las 07:30", RespaldoServicio.describirHorario("0 30 7 * * *"));
        assertTrue(RespaldoServicio.describirHorario("0 0 21 * * MON").contains("cron"));
    }
}
