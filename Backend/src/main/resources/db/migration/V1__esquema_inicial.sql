-- Esquema inicial de Costos 3D.
-- Flyway ejecuta cada archivo V<n>__*.sql una sola vez y en orden.
-- Para cambiar la estructura, NO edites este archivo: crea uno nuevo (V3__..., V4__...).

CREATE TABLE configuracion (
    id                       BIGINT        NOT NULL PRIMARY KEY,
    nombre_negocio           VARCHAR(255),
    contacto_negocio         VARCHAR(500),
    condiciones_por_defecto  VARCHAR(2000),
    dias_vigencia_cotizacion INT,
    precio_kwh               DOUBLE,
    valor_hora_mano_obra     DOUBLE,
    potencia_impresora_w     DOUBLE,
    precio_impresora         DOUBLE,
    vida_util_impresora_horas DOUBLE,
    mantenimiento_por_hora   DOUBLE,
    margen_por_defecto_pct   DOUBLE,
    margen_minimo_pct        DOUBLE,
    paso_redondeo            DOUBLE
);

CREATE TABLE filamento (
    id                BIGINT        NOT NULL AUTO_INCREMENT PRIMARY KEY,
    material          VARCHAR(100)  NOT NULL,
    color             VARCHAR(100)  NOT NULL,
    marca             VARCHAR(100),
    precio_compra     DOUBLE        NOT NULL,
    peso_inicial_g    DOUBLE        NOT NULL,
    peso_disponible_g DOUBLE        NOT NULL,
    notas             VARCHAR(1000),
    demo              BOOLEAN       NOT NULL DEFAULT FALSE
);

CREATE TABLE producto (
    id                              BIGINT        NOT NULL AUTO_INCREMENT PRIMARY KEY,
    nombre                          VARCHAR(255)  NOT NULL,
    referencia                      VARCHAR(100),
    filamento_id                    BIGINT,
    peso_g                          DOUBLE,
    desperdicio_por_unidad_g        DOUBLE,
    tiempo_impresion_min_por_unidad DOUBLE,
    posproceso_min_por_unidad       DOUBLE,
    empaque_por_unidad              DOUBLE,
    notas                           VARCHAR(1000),
    demo                            BOOLEAN       NOT NULL DEFAULT FALSE,
    CONSTRAINT fk_producto_filamento FOREIGN KEY (filamento_id) REFERENCES filamento (id)
);

CREATE TABLE cotizacion (
    id                BIGINT        NOT NULL AUTO_INCREMENT PRIMARY KEY,
    numero            VARCHAR(20),
    nombre_cliente    VARCHAR(255),
    contacto_cliente  VARCHAR(255),
    fecha_emision     DATE,
    dias_vigencia     INT           NOT NULL,
    margen_pct        DOUBLE        NOT NULL,
    margen_minimo_pct DOUBLE        NOT NULL,
    descuento_pct     DOUBLE        NOT NULL,
    costo_diseno      DOUBLE        NOT NULL,
    costo_envio       DOUBLE        NOT NULL,
    otros_costos      DOUBLE        NOT NULL,
    condiciones       VARCHAR(2000),
    notas             VARCHAR(2000),
    estado            VARCHAR(20)   NOT NULL,
    fecha_venta       DATE
);

-- producto_id y filamento_id son copias de referencia (sin llave foránea):
-- la cotización se conserva aunque después se borre el producto o el filamento.
CREATE TABLE item_cotizacion (
    id                         BIGINT        NOT NULL AUTO_INCREMENT PRIMARY KEY,
    cotizacion_id              BIGINT,
    posicion                   INT,
    producto_id                BIGINT,
    filamento_id               BIGINT,
    descripcion                VARCHAR(500),
    cantidad                   INT           NOT NULL,
    costo_unitario             DOUBLE        NOT NULL,
    precio_unitario            DOUBLE        NOT NULL,
    gramos_material_por_unidad DOUBLE        NOT NULL,
    CONSTRAINT fk_item_cotizacion FOREIGN KEY (cotizacion_id) REFERENCES cotizacion (id)
);
