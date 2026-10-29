# Costos 3D

Aplicación web para calcular costos, precios de venta y rentabilidad de un negocio de impresión 3D. Moneda: pesos colombianos (COP).

- **Backend**: Java 21 + Spring Boot 4 + JPA. Base de datos **MySQL** (base `costos3d`), que puedes ver y administrar con MySQL Workbench.
- **Frontend**: Angular 21 (TypeScript) con menú lateral y diseño adaptable a celular.

## Requisitos

- Java 21 (JDK)
- Node.js 20 o superior y npm
- MySQL Server 8 (y MySQL Workbench para ver los datos)
- No hace falta instalar Maven: el proyecto trae `mvnw` (Maven Wrapper).

## Configurar MySQL (solo la primera vez)

1. Copia `Backend/.env.example` como `Backend/.env` y escribe tu usuario y contraseña de MySQL (`DB_USERNAME`, `DB_PASSWORD`). El `.env` **no se sube a git**.
2. Arranca el backend: la base `costos3d` y sus tablas se crean solas, junto con los datos de demostración.
3. Opcional: para no usar `root`, ejecuta `Backend/mysql/crear-base.sql` en MySQL Workbench (crea el usuario `costos3d`) y pon ese usuario en el `.env`.

Para ver los datos en Workbench, crea una conexión a `127.0.0.1`, puerto `3306`, con el mismo usuario del `.env`.

### Variables del `.env`

| Variable | Para qué | Por defecto |
|---|---|---|
| `DB_HOST` | Servidor MySQL | `localhost` |
| `DB_PORT` | Puerto de MySQL | `3306` |
| `DB_NAME` | Nombre de la base | `costos3d` |
| `DB_USERNAME` | Usuario de MySQL | `root` |
| `DB_PASSWORD` | Contraseña de MySQL | (vacía) |
| `SERVER_PORT` | Puerto del backend. Si lo cambias, cambia también `Frontend/proxy.conf.json` | `8081` |

Una variable de entorno del sistema con el mismo nombre tiene prioridad sobre el `.env` (útil en un servidor).

## Cómo arrancar en tu computador

Abre **dos terminales**.

**Terminal 1: backend** (queda en http://localhost:8081)

```bash
cd Backend
./mvnw spring-boot:run
```

En Windows PowerShell/CMD usa `mvnw.cmd spring-boot:run`.

**Terminal 2: frontend** (queda en http://localhost:4200)

```bash
cd Frontend
npm install
npm start
```

Abre http://localhost:4200. El frontend redirige las llamadas `/api` al backend mediante `proxy.conf.json`.

## Pruebas

```bash
cd Backend
./mvnw test
```

Las pruebas usan una base H2 en memoria (modo MySQL) y ejecutan las mismas migraciones de Flyway: no necesitan MySQL ni tocan tus datos.

`CalculadoraPrecioServicioTest` verifica las fórmulas con un ejemplo calculado a mano (los pasos están escritos en el comentario de la clase).

## Estructura del backend

Las carpetas siguen la convención en inglés; clases, campos, métodos, tablas y la API están en español.

```
Backend/src/main/java/co/impresion3d/costos
├── aspect       RegistroPeticionesAspecto (tiempo de cada petición en el log)
├── config       CorsConfiguracion
├── controller   *Controlador: endpoints REST
├── documento    (reservado) PDF generado en el servidor
├── dto          Solicitud*, Resultado*, Resumen*, ErrorApi
├── entity       Configuracion, Filamento, Producto, Cotizacion, ItemCotizacion
├── enums        EstadoCotizacion
├── exception    ManejadorGlobalExcepciones y excepciones propias
├── mapper       *Mapeador: de DTO a entidad
├── repository   *Repositorio (consultas con @Query)
├── security     (reservado) inicio de sesión cuando haya usuarios
├── service      *Servicio: reglas de negocio y fórmulas
└── CostosAplicacion.java
Backend/src/main/resources
├── db/migration   V1__esquema_inicial.sql, V2__datos_demostracion.sql (Flyway)
├── documento, static, templates
├── application.yaml
└── logback-spring.xml   logs en consola y en Backend/logs/costos.log
```

### API

| Método | Ruta | Para qué |
|---|---|---|
| GET, PUT | `/api/configuracion` | Configuración del negocio |
| GET, POST, PUT, DELETE | `/api/filamentos[/{id}]` | Filamentos e inventario |
| GET, POST, PUT, DELETE | `/api/productos[/{id}]` | Catálogo de productos |
| POST | `/api/precios/calcular` | Calculadora de precios |
| GET, POST, PUT, DELETE | `/api/cotizaciones[/{id}]` | Cotizaciones |
| PUT | `/api/cotizaciones/{id}/estado` | Cambiar estado / registrar venta |
| POST | `/api/cotizaciones/vista-previa` | Totales sin guardar |
| POST | `/api/cotizaciones/precio-item` | Costo y precio de un producto para una cantidad |
| POST | `/api/descuentos/comparar` | Descuentos por mayor para un producto y una cantidad |
| GET | `/api/analisis?meses=12` | Indicadores de rentabilidad del periodo |
| GET | `/api/exportar/excel?desde=&hasta=` | Excel de ventas y cotizaciones (fechas opcionales) |
| GET, POST | `/api/respaldos` | Estado de las copias de seguridad / hacer una copia ahora |

### Cambios en la base de datos

Las tablas las crea **Flyway**, no Hibernate. Para cambiar la estructura, crea un archivo nuevo en `db/migration` (`V3__descripcion.sql`, `V4__...`). Nunca edites uno que ya se ejecutó: Flyway lo detecta y no arranca.

## Fórmulas principales

| Concepto | Fórmula |
|---|---|
| Gramos del lote | (peso + desperdicio por unidad) × cantidad + purga del lote |
| Material | gramos del lote / 1000 × precio por kg |
| Horas de máquina | tiempo real del lote, o tiempo por unidad × cantidad si no se indica |
| Eléctrico | potencia (W) / 1000 × horas de máquina × precio kWh |
| Máquina | horas de máquina × (precio impresora / vida útil en horas + mantenimiento por hora) |
| Mano de obra | (preparación + posprocesado por unidad × cantidad + posprocesado por lote) / 60 × valor hora |
| Otros | diseño + empaque × cantidad + envío + otros |
| Costo por unidad | costo total del pedido / cantidad |
| Precio recomendado | costo por unidad / (1 − margen), redondeado hacia arriba |
| Margen real | (precio − costo por unidad) / precio |
| Precio mínimo rentable | costo por unidad / (1 − margen mínimo) |
| Descuento máximo | 1 − precio mínimo / precio recomendado |

Los costos fijos del pedido (preparación, diseño, envío, otros) se cuentan una sola vez y se reparten entre las unidades.

## Cotizaciones y ventas

- Cada línea de una cotización guarda una copia del costo y el precio unitario. Si después cambias el producto o el filamento, la cotización no se altera.
- El costo y el precio de un producto del catálogo se calculan para la cantidad pedida. Diseño, envío y otros costos del pedido se cobran una sola vez, como línea aparte: costo / (1 − margen).
- El descuento se aplica sobre el total bruto. El descuento máximo es el que deja el margen real justo en el margen mínimo de Configuración.
- Una cotización **no** es una venta. Solo cuenta como venta cuando usas «Registrar venta»: ahí se descuenta el filamento del inventario y la cotización queda bloqueada. «Anular venta» devuelve el filamento.
- PDF: la vista «Ver PDF» muestra el documento para el cliente, sin costos ni márgenes. El botón «Imprimir o guardar PDF» abre el diálogo de impresión del navegador; elige «Guardar como PDF».

`ResumenCotizacionTest` y `CotizacionApiTest` verifican los totales con un ejemplo calculado a mano y el flujo de venta con inventario.

## Descuentos por mayor

- El **precio de lista** es el precio recomendado para la cantidad de referencia (normalmente 1 unidad), con los costos del pedido incluidos.
- Al pedir más unidades, la preparación, el diseño, el envío y otros costos del pedido se reparten entre más piezas. El material, el tiempo de máquina y el posprocesado por pieza **no** bajan.
- Cada descuento se aplica sobre el precio de lista. Se bloquea si deja el margen real por debajo del margen mínimo.
- **Descuento máximo admisible** = 1 − precio mínimo / precio de lista, donde precio mínimo = costo por unidad / (1 − margen mínimo).

`DescuentoApiTest` lo verifica con un ejemplo calculado a mano.

## Análisis de rentabilidad

- **Ventas**: solo las cotizaciones en estado «Venta registrada», por fecha de venta dentro del periodo.
- **Cotizaciones abiertas** (borrador, enviada, aceptada): ingresos posibles. Se muestran aparte y nunca se suman a las ventas.
- **Por producto**: el descuento de cada venta se reparte entre sus líneas según su valor, y los costos del pedido van en una línea aparte. Así la suma de los productos coincide con el total vendido.
- **Tasa de cierre**: ventas / (ventas + rechazadas) entre las cotizaciones emitidas en el periodo.

`AnalisisServicioTest` lo verifica con cinco cotizaciones calculadas a mano.

## Exportar a Excel

En **Cotizaciones y ventas → Exportar a Excel**. El archivo trae cuatro hojas:

| Hoja | Contenido |
|---|---|
| Resumen | Ventas del periodo (cantidad, ingresos, costos, ganancia, margen, descuentos) y cotizaciones abiertas aparte |
| Ventas | Una fila por venta, filtradas por **fecha de venta**, con fila de totales (fórmulas SUMA) |
| Cotizaciones | Las que no son ventas, filtradas por **fecha de emisión**, con su estado |
| Detalle | Una fila por producto de cada venta y cotización, más la línea de diseño, preparación y envío |

Los valores son números y fechas reales de Excel, así que se pueden sumar, filtrar y hacer tablas dinámicas.

## Copias de seguridad

- El backend guarda una copia completa de MySQL (`mysqldump`) **todos los días a las 9:00 p. m.** y otra **al encender el backend** si ese día todavía no hay una (por si el computador estaba apagado a esa hora).
- Quedan en `Backend/respaldos/costos3d-AAAA-MM-DD_HHMMSS.sql`. Se conservan las últimas 30. La carpeta está en `.gitignore`.
- En **Configuración → Copias de seguridad** se ven las copias y se puede hacer una al instante.
- La contraseña se pasa a `mysqldump` en un archivo temporal que se borra al terminar: no aparece en la línea de comandos ni en la copia.
- Variables del `.env`: `RESPALDO_MYSQLDUMP` (ruta de mysqldump.exe), `RESPALDO_CONSERVAR`, `RESPALDO_CRON` (hora) y `RESPALDO_HABILITADO`.

**Restaurar una copia** (reemplaza los datos actuales): en MySQL Workbench, **Server → Data Import → Import from Self-Contained File**, elige el `.sql` y pulsa **Start Import**. O en una terminal:

```bash
mysql -u root -p < Backend/respaldos/costos3d-2026-10-29_210000.sql
```

**Recomendado:** copia de vez en cuando la carpeta `respaldos` a otro lugar (USB, Google Drive, OneDrive). Si el disco del computador falla, las copias locales se pierden con él.

## Datos de demostración

La primera vez que arranca con la base vacía, el backend crea una configuración y algunos filamentos y productos marcados como **demo**. Son valores ilustrativos, no precios reales: reemplázalos por los tuyos o elimínalos.

Para empezar de cero, detén el backend y en Workbench ejecuta `DROP DATABASE costos3d;`. Al arrancar, Flyway la crea de nuevo con los datos de demostración.

Copia de seguridad: en Workbench, menú **Server → Data Export**, elige `costos3d` y exporta a un archivo `.sql`.

## Despliegue

1. Compila el frontend: `cd Frontend && npm run build`. Queda en `Frontend/dist/costos-3d/browser/`.
2. Empaqueta el backend: `cd Backend && ./mvnw package`. Queda en `Backend/target/costos-0.0.1-SNAPSHOT.jar`.
3. En el servidor, ejecuta `java -jar costos-0.0.1-SNAPSHOT.jar` y sirve la carpeta del frontend con Nginx (o similar), redirigiendo `/api` al puerto 8081.
4. En el servidor, crea un `.env` junto al `.jar` (o define las mismas variables de entorno) para apuntar a su MySQL.

## Estado

| Sección | Estado |
|---|---|
| Panel principal | Funciona (catálogo, inventario y ventas registradas) |
| Calculadora de precios | Funciona |
| Productos | Funciona (crear, editar, eliminar) |
| Filamentos e inventario | Funciona (crear, editar, eliminar) |
| Configuración | Funciona |
| Cotizaciones y ventas | Funciona (varias líneas, descuento, rentabilidad, PDF, registro de ventas) |
| Descuentos por mayor | Funciona (comparación, margen mínimo, escala por cantidad) |
| Análisis de rentabilidad | Funciona (ventas vs. cotizaciones, ventas por mes, productos, descuentos) |
