-- Configuración inicial y datos de DEMOSTRACIÓN (marcados con demo = TRUE).
-- Son valores ilustrativos, no precios reales de mercado: reemplázalos por los tuyos.

INSERT INTO configuracion (id, nombre_negocio, contacto_negocio, condiciones_por_defecto, dias_vigencia_cotizacion,
                           precio_kwh, valor_hora_mano_obra, potencia_impresora_w, precio_impresora,
                           vida_util_impresora_horas, mantenimiento_por_hora, margen_por_defecto_pct,
                           margen_minimo_pct, paso_redondeo)
VALUES (1, 'Mi taller 3D', NULL,
        'Precios en pesos colombianos (COP).
Forma de pago: 50 % para iniciar la producción y 50 % contra entrega.
Tiempo de entrega: a convenir según la cantidad.
Los colores pueden variar levemente según el lote de filamento.',
        15, 900, 12000, 150, 2500000, 5000, 300, 40, 20, 100);

INSERT INTO filamento (material, color, marca, precio_compra, peso_inicial_g, peso_disponible_g, notas, demo)
VALUES ('PLA', 'Negro', 'Demo', 80000, 1000, 1000, 'Dato de demostración: reemplázalo por tu compra real.', TRUE),
       ('PETG', 'Transparente', 'Demo', 95000, 1000, 1000, 'Dato de demostración: reemplázalo por tu compra real.', TRUE),
       ('PLA', 'Blanco', 'Demo', 80000, 1000, 650, 'Dato de demostración: reemplázalo por tu compra real.', TRUE);

INSERT INTO producto (nombre, referencia, filamento_id, peso_g, desperdicio_por_unidad_g,
                      tiempo_impresion_min_por_unidad, posproceso_min_por_unidad, empaque_por_unidad, notas, demo)
SELECT 'Soporte para celular (demo)', 'DEMO-001', id, 45, 4, 150, 5, 1500, 'Producto de demostración.', TRUE
FROM filamento WHERE material = 'PLA' AND color = 'Negro' AND demo = TRUE;

INSERT INTO producto (nombre, referencia, filamento_id, peso_g, desperdicio_por_unidad_g,
                      tiempo_impresion_min_por_unidad, posproceso_min_por_unidad, empaque_por_unidad, notas, demo)
SELECT 'Maceta geométrica (demo)', 'DEMO-002', id, 120, 8, 300, 10, 2500, 'Producto de demostración.', TRUE
FROM filamento WHERE material = 'PETG' AND color = 'Transparente' AND demo = TRUE;
