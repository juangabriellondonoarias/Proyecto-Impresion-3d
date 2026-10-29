package co.impresion3d.costos.dto;

import java.util.Map;

/** Respuesta de error para el frontend: mensaje general y errores por campo. */
public record ErrorApi(String mensaje, Map<String, String> errores) {
}
