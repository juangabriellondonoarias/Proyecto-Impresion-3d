package co.impresion3d.costos.exception;

import co.impresion3d.costos.dto.ErrorApi;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.util.LinkedHashMap;
import java.util.Map;

/** Convierte los errores en respuestas JSON con mensajes en español para el frontend. */
@RestControllerAdvice
public class ManejadorGlobalExcepciones {

    private static final Logger log = LoggerFactory.getLogger(ManejadorGlobalExcepciones.class);

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorApi> validacion(MethodArgumentNotValidException ex) {
        Map<String, String> errores = new LinkedHashMap<>();
        for (FieldError error : ex.getBindingResult().getFieldErrors()) {
            errores.putIfAbsent(error.getField(), error.getDefaultMessage());
        }
        return ResponseEntity.badRequest().body(new ErrorApi("Revisa los datos marcados.", errores));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorApi> formatoInvalido(HttpMessageNotReadableException ex) {
        return ResponseEntity.badRequest()
                .body(new ErrorApi("Los datos enviados no tienen un formato válido.", Map.of()));
    }

    @ExceptionHandler(RecursoNoEncontradoExcepcion.class)
    public ResponseEntity<ErrorApi> noEncontrado(RecursoNoEncontradoExcepcion ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new ErrorApi(ex.getMessage(), Map.of()));
    }

    @ExceptionHandler(ConflictoExcepcion.class)
    public ResponseEntity<ErrorApi> conflicto(ConflictoExcepcion ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(new ErrorApi(ex.getMessage(), Map.of()));
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ErrorApi> rutaInexistente(NoResourceFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(new ErrorApi("La ruta " + ex.getResourcePath() + " no existe.", Map.of()));
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ErrorApi> metodoNoPermitido(HttpRequestMethodNotSupportedException ex) {
        return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED)
                .body(new ErrorApi("Esta ruta no acepta " + ex.getMethod() + ".", Map.of()));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorApi> inesperado(Exception ex) {
        log.error("Error inesperado", ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ErrorApi("Ocurrió un error inesperado en el servidor.", Map.of()));
    }
}
