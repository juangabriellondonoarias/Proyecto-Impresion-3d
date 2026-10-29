package co.impresion3d.costos.service;

import co.impresion3d.costos.config.PropiedadesRespaldo;
import co.impresion3d.costos.dto.EstadoRespaldos;
import co.impresion3d.costos.exception.RespaldoExcepcion;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

/**
 * Copias de seguridad de la base MySQL con mysqldump.
 *
 * Cada copia es un archivo .sql con la estructura y los datos (incluye CREATE DATABASE), en
 * Backend/respaldos. Se hace una copia diaria a la hora configurada y otra al arrancar si todavía
 * no hay una de hoy (por si el computador estaba apagado a esa hora). Se conservan las últimas N.
 */
@Service
public class RespaldoServicio {

    private static final Logger log = LoggerFactory.getLogger(RespaldoServicio.class);
    static final ZoneId COLOMBIA = ZoneId.of("America/Bogota");
    private static final DateTimeFormatter SELLO = DateTimeFormatter.ofPattern("yyyy-MM-dd_HHmmss");
    private static final long LIMITE_MINUTOS = 10;

    private final PropiedadesRespaldo propiedades;
    private final String host;
    private final String puerto;
    private final String base;
    private final String usuario;
    private final String contrasena;
    private final Pattern patronArchivo;

    public RespaldoServicio(PropiedadesRespaldo propiedades,
                            @Value("${DB_HOST:localhost}") String host,
                            @Value("${DB_PORT:3306}") String puerto,
                            @Value("${DB_NAME:costos3d}") String base,
                            @Value("${spring.datasource.username:root}") String usuario,
                            @Value("${spring.datasource.password:}") String contrasena) {
        this.propiedades = propiedades;
        this.host = host;
        this.puerto = puerto;
        this.base = base;
        this.usuario = usuario;
        this.contrasena = contrasena;
        this.patronArchivo = Pattern.compile(Pattern.quote(base) + "-(\\d{4}-\\d{2}-\\d{2}_\\d{6})\\.sql");
    }

    /** Copia diaria a la hora configurada (por defecto 9:00 p. m., hora de Colombia). */
    @Scheduled(cron = "${aplicacion.respaldo.cron:0 0 21 * * *}", zone = "America/Bogota")
    public void respaldoProgramado() {
        if (!propiedades.habilitado()) {
            return;
        }
        try {
            EstadoRespaldos.Archivo archivo = respaldar();
            log.info("Copia de seguridad diaria creada: {}", archivo.nombre());
        } catch (RespaldoExcepcion ex) {
            log.error("La copia de seguridad diaria falló: {}", ex.getMessage());
        }
    }

    /** Al arrancar, si no hay copia de hoy, la hace en segundo plano para no demorar el inicio. */
    @EventListener(ApplicationReadyEvent.class)
    public void respaldoAlArrancar() {
        if (!propiedades.habilitado() || !propiedades.alArrancar() || hayRespaldoDe(LocalDate.now(COLOMBIA))) {
            return;
        }
        Thread.ofVirtual().name("respaldo-inicial").start(() -> {
            try {
                log.info("Copia de seguridad al arrancar creada: {}", respaldar().nombre());
            } catch (RespaldoExcepcion ex) {
                log.error("La copia de seguridad al arrancar falló: {}", ex.getMessage());
            }
        });
    }

    /** Hace una copia ahora. Solo una a la vez. */
    public synchronized EstadoRespaldos.Archivo respaldar() {
        Path carpeta = carpeta();
        String nombre = base + "-" + LocalDateTime.now(COLOMBIA).format(SELLO) + ".sql";
        Path destino = carpeta.resolve(nombre);
        Path temporal = carpeta.resolve(nombre + ".parcial");
        Path credenciales = null;
        try {
            Files.createDirectories(carpeta);
            credenciales = archivoCredenciales();
            ejecutarMysqldump(credenciales, temporal);
            // Se renombra al final: un .sql en la carpeta siempre es una copia completa.
            Files.move(temporal, destino, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            aplicarRetencion();
            return describir(destino);
        } catch (IOException ex) {
            throw new RespaldoExcepcion("No se pudo escribir la copia en " + carpeta + ": " + ex.getMessage(), ex);
        } finally {
            borrarSilencioso(temporal);
            borrarSilencioso(credenciales);
        }
    }

    public EstadoRespaldos estado() {
        return new EstadoRespaldos(propiedades.habilitado(), carpeta().toString(), describirHorario(propiedades.cron()),
                propiedades.conservar(), listar());
    }

    /** Copias existentes, la más reciente primero. */
    public List<EstadoRespaldos.Archivo> listar() {
        Path carpeta = carpeta();
        if (!Files.isDirectory(carpeta)) {
            return List.of();
        }
        try (Stream<Path> archivos = Files.list(carpeta)) {
            return archivos.filter(p -> patronArchivo.matcher(p.getFileName().toString()).matches())
                    .map(this::describir)
                    .sorted(Comparator.comparing(EstadoRespaldos.Archivo::fecha).reversed())
                    .toList();
        } catch (IOException ex) {
            throw new RespaldoExcepcion("No se pudo leer la carpeta " + carpeta, ex);
        }
    }

    /** Borra las copias más viejas que excedan el número a conservar. */
    void aplicarRetencion() {
        List<EstadoRespaldos.Archivo> archivos = listar();
        for (EstadoRespaldos.Archivo viejo : archivos.subList(Math.min(propiedades.conservar(), archivos.size()), archivos.size())) {
            borrarSilencioso(carpeta().resolve(viejo.nombre()));
            log.info("Copia antigua eliminada: {}", viejo.nombre());
        }
    }

    boolean hayRespaldoDe(LocalDate dia) {
        return listar().stream().anyMatch(a -> a.fecha().toLocalDate().equals(dia));
    }

    Path carpeta() {
        return Path.of(propiedades.carpeta()).toAbsolutePath().normalize();
    }

    private void ejecutarMysqldump(Path credenciales, Path salida) throws IOException {
        List<String> comando = new ArrayList<>(List.of(
                propiedades.mysqldump(),
                "--defaults-extra-file=" + credenciales,
                "--host=" + host,
                "--port=" + puerto,
                "--user=" + usuario,
                "--single-transaction",
                "--routines",
                "--triggers",
                "--no-tablespaces",
                "--default-character-set=utf8mb4",
                "--result-file=" + salida,
                "--databases", base));
        Path errores = Files.createTempFile("mysqldump-", ".log");
        try {
            Process proceso;
            try {
                proceso = new ProcessBuilder(comando).redirectErrorStream(true).redirectOutput(errores.toFile()).start();
            } catch (IOException ex) {
                throw new RespaldoExcepcion("No se encontró mysqldump en «" + propiedades.mysqldump()
                        + "». Revisa RESPALDO_MYSQLDUMP en el archivo .env.", ex);
            }
            boolean termino = proceso.waitFor(LIMITE_MINUTOS, TimeUnit.MINUTES);
            if (!termino) {
                proceso.destroyForcibly();
                throw new RespaldoExcepcion("mysqldump tardó más de " + LIMITE_MINUTOS + " minutos y se canceló.");
            }
            if (proceso.exitValue() != 0) {
                String detalle = Files.readString(errores, StandardCharsets.UTF_8).lines()
                        .filter(l -> !l.isBlank() && !l.contains("Using a password"))
                        .reduce((a, b) -> a + " " + b).orElse("sin detalle");
                throw new RespaldoExcepcion("mysqldump terminó con error " + proceso.exitValue() + ": " + detalle);
            }
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new RespaldoExcepcion("La copia de seguridad se interrumpió.", ex);
        } finally {
            borrarSilencioso(errores);
        }
    }

    /** Archivo temporal con la contraseña, solo legible por el usuario actual, para no pasarla en la línea de comandos. */
    private Path archivoCredenciales() throws IOException {
        Path archivo = Files.createTempFile("costos3d-", ".cnf");
        archivo.toFile().setReadable(false, false);
        archivo.toFile().setReadable(true, true);
        String escapada = contrasena.replace("\\", "\\\\").replace("\"", "\\\"");
        Files.writeString(archivo, "[client]\npassword=\"" + escapada + "\"\n", StandardCharsets.UTF_8);
        return archivo;
    }

    private EstadoRespaldos.Archivo describir(Path archivo) {
        Matcher m = patronArchivo.matcher(archivo.getFileName().toString());
        LocalDateTime fecha = m.matches() ? LocalDateTime.parse(m.group(1), SELLO) : LocalDateTime.MIN;
        try {
            return new EstadoRespaldos.Archivo(archivo.getFileName().toString(), Files.size(archivo), fecha);
        } catch (IOException ex) {
            return new EstadoRespaldos.Archivo(archivo.getFileName().toString(), 0, fecha);
        }
    }

    /** «0 0 21 * * *» -> «Todos los días a las 21:00». */
    static String describirHorario(String cron) {
        String[] partes = cron.trim().split("\\s+");
        if (partes.length == 6 && partes[3].equals("*") && partes[4].equals("*") && partes[5].equals("*")
                && partes[1].matches("\\d+") && partes[2].matches("\\d+")) {
            return "Todos los días a las %02d:%02d".formatted(Integer.parseInt(partes[2]), Integer.parseInt(partes[1]));
        }
        return "Según la expresión cron «" + cron + "»";
    }

    private static void borrarSilencioso(Path archivo) {
        if (archivo == null) {
            return;
        }
        try {
            Files.deleteIfExists(archivo);
        } catch (IOException ex) {
            log.warn("No se pudo borrar {}: {}", archivo, ex.getMessage());
        }
    }
}
