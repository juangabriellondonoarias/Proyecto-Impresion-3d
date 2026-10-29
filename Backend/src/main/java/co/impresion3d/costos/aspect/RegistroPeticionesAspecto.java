package co.impresion3d.costos.aspect;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Registra en el log cada llamada a un controlador y cuánto tardó.
 * Las que pasan de un segundo se marcan como lentas.
 */
@Aspect
@Component
public class RegistroPeticionesAspecto {

    private static final Logger log = LoggerFactory.getLogger(RegistroPeticionesAspecto.class);
    private static final long UMBRAL_LENTO_MS = 1000;

    @Around("within(co.impresion3d.costos.controller..*)")
    public Object registrar(ProceedingJoinPoint punto) throws Throwable {
        String metodo = punto.getSignature().getDeclaringType().getSimpleName() + "." + punto.getSignature().getName();
        long inicio = System.currentTimeMillis();
        try {
            return punto.proceed();
        } finally {
            long duracion = System.currentTimeMillis() - inicio;
            if (duracion > UMBRAL_LENTO_MS) {
                log.warn("{} tardó {} ms (lento)", metodo, duracion);
            } else {
                log.debug("{} tardó {} ms", metodo, duracion);
            }
        }
    }
}
