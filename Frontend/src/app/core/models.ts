// Modelos de la API del backend (mismos nombres que los DTO y entidades de Java).

export interface Configuracion {
  id?: number;
  nombreNegocio: string;
  contactoNegocio: string | null;
  condicionesPorDefecto: string | null;
  diasVigenciaCotizacion: number | null;
  precioKwh: number;
  valorHoraManoObra: number;
  potenciaImpresoraW: number;
  precioImpresora: number;
  vidaUtilImpresoraHoras: number;
  mantenimientoPorHora: number;
  margenPorDefectoPct: number;
  margenMinimoPct: number;
  pasoRedondeo: number;
  /** Calculado por el backend: depreciación + mantenimiento por hora. */
  costoMaquinaPorHora?: number;
}

export interface Filamento {
  id?: number;
  material: string;
  color: string;
  marca: string | null;
  precioCompra: number;
  pesoInicialG: number;
  pesoDisponibleG: number;
  notas: string | null;
  demo?: boolean;
  costoPorKg?: number;
  etiqueta?: string;
}

export interface Producto {
  id?: number;
  nombre: string;
  referencia: string | null;
  filamento: Filamento | null;
  pesoG: number;
  desperdicioPorUnidadG: number | null;
  tiempoImpresionMinPorUnidad: number | null;
  posprocesoMinPorUnidad: number | null;
  empaquePorUnidad: number | null;
  notas: string | null;
  demo?: boolean;
}

export interface SolicitudProducto {
  nombre: string;
  referencia: string | null;
  filamentoId: number | null;
  pesoG: number;
  desperdicioPorUnidadG: number | null;
  tiempoImpresionMinPorUnidad: number | null;
  posprocesoMinPorUnidad: number | null;
  empaquePorUnidad: number | null;
  notas: string | null;
}

export interface SolicitudPrecio {
  nombreProducto: string;
  referencia: string;
  material: string;
  color: string;
  precioFilamentoKg: number | null;
  pesoPiezaG: number | null;
  desperdicioPorUnidadG: number | null;
  desperdicioPorLoteG: number | null;
  cantidad: number | null;
  tiempoImpresionPorUnidadMin: number | null;
  tiempoImpresionLoteMin: number | null;
  tiempoPreparacionMin: number | null;
  posprocesoPorUnidadMin: number | null;
  posprocesoPorLoteMin: number | null;
  potenciaImpresoraW: number | null;
  precioKwh: number | null;
  valorHoraManoObra: number | null;
  costoMaquinaPorHora: number | null;
  costoDiseno: number | null;
  empaquePorUnidad: number | null;
  costoEnvio: number | null;
  otrosCostos: number | null;
  margenObjetivoPct: number | null;
  descuentoPct: number | null;
  margenMinimoPct: number | null;
  pasoRedondeo: number | null;
}

export interface ResultadoPrecio {
  cantidad: number;
  gramosMaterialPorUnidad: number;
  gramosMaterialLote: number;
  costoMaterialPorUnidad: number;
  costoMaterialLote: number;
  horasMaquina: number;
  horasHumanas: number;
  costoElectrico: number;
  costoManoObra: number;
  costoMaquina: number;
  costoDiseno: number;
  costoEmpaque: number;
  costoEnvio: number;
  otrosCostos: number;
  totalOtrosCostos: number;
  costoTotal: number;
  costoUnitario: number;
  precioUnitarioRecomendado: number;
  precioLoteRecomendado: number;
  gananciaPorUnidad: number;
  gananciaLote: number;
  margenRealPct: number;
  margenMinimoPct: number;
  precioUnitarioMinimo: number;
  descuentoMaximoPct: number;
  descuentoPct: number;
  precioUnitarioFinal: number;
  precioLoteFinal: number;
  gananciaFinalPorUnidad: number;
  gananciaFinalLote: number;
  margenFinalPct: number;
  descuentoExcedeMaximo: boolean;
  advertencias: string[];
}

export interface ErrorApi {
  mensaje: string;
  errores: Record<string, string>;
}

export type EstadoCotizacion = 'BORRADOR' | 'ENVIADA' | 'ACEPTADA' | 'RECHAZADA' | 'VENDIDA';

export const ETIQUETA_ESTADO: Record<EstadoCotizacion, string> = {
  BORRADOR: 'Borrador',
  ENVIADA: 'Enviada',
  ACEPTADA: 'Aceptada',
  RECHAZADA: 'Rechazada',
  VENDIDA: 'Venta registrada',
};

export interface ItemCotizacion {
  id?: number;
  productoId: number | null;
  filamentoId: number | null;
  descripcion: string;
  cantidad: number;
  costoUnitario: number;
  precioUnitario: number;
  gramosMaterialPorUnidad: number;
  costoLinea?: number;
  precioLinea?: number;
}

export interface ResumenCotizacion {
  unidades: number;
  costoProductos: number;
  precioProductos: number;
  costoFijo: number;
  precioFijo: number;
  totalBruto: number;
  descuentoPct: number;
  montoDescuento: number;
  total: number;
  costoTotal: number;
  ganancia: number;
  margenPct: number;
  margenMinimoPct: number;
  descuentoMaximoPct: number;
  bajoMargenMinimo: boolean;
  advertencias: string[];
}

export interface Cotizacion {
  id: number;
  numero: string;
  nombreCliente: string;
  contactoCliente: string | null;
  fechaEmision: string;
  diasVigencia: number;
  fechaVencimiento: string | null;
  margenPct: number;
  margenMinimoPct: number;
  descuentoPct: number;
  costoDiseno: number;
  costoEnvio: number;
  otrosCostos: number;
  condiciones: string | null;
  notas: string | null;
  estado: EstadoCotizacion;
  fechaVenta: string | null;
  items: ItemCotizacion[];
  resumen: ResumenCotizacion;
}

export interface SolicitudCotizacion {
  nombreCliente: string;
  contactoCliente: string | null;
  fechaEmision: string;
  diasVigencia: number | null;
  margenPct: number | null;
  descuentoPct: number | null;
  costoDiseno: number | null;
  costoEnvio: number | null;
  otrosCostos: number | null;
  condiciones: string | null;
  notas: string | null;
  items: ItemCotizacion[];
}

export interface PrecioItem {
  productoId: number;
  filamentoId: number | null;
  descripcion: string;
  cantidad: number;
  costoUnitario: number;
  precioUnitario: number;
  gramosMaterialPorUnidad: number;
}

export interface SolicitudDescuentos {
  productoId: number | null;
  cantidad: number | null;
  cantidadReferencia: number | null;
  margenPct: number | null;
  margenMinimoPct: number | null;
  descuentos: number[];
  costoDiseno: number | null;
  costoEnvio: number | null;
  otrosCostos: number | null;
  tiempoPreparacionMin: number | null;
}

export interface OpcionDescuento {
  descuentoPct: number;
  precioUnitario: number;
  totalPedido: number;
  gananciaTotal: number;
  margenPct: number;
  ahorroCliente: number;
  permitido: boolean;
}

export interface EscalaCantidad {
  cantidad: number;
  costoUnitario: number;
  precioSugerido: number;
  precioMinimo: number;
  descuentoMaximoPct: number;
}

export interface ResultadoDescuentos {
  producto: string;
  cantidad: number;
  cantidadReferencia: number;
  margenPct: number;
  margenMinimoPct: number;
  precioLista: number;
  costoUnitario: number;
  precioMinimo: number;
  precioSugerido: number;
  descuentoSugeridoPct: number;
  descuentoMaximoPct: number;
  opciones: OpcionDescuento[];
  escala: EscalaCantidad[];
  advertencias: string[];
}

export interface ResultadoAnalisis {
  desde: string;
  hasta: string;
  ventas: {
    cantidad: number;
    ingresos: number;
    costos: number;
    costoProductos: number;
    costoPedido: number;
    ganancia: number;
    margenPct: number;
    ticketPromedio: number;
    descuentosOtorgados: number;
    descuentoPromedioPct: number;
  };
  abiertas: {
    cantidad: number;
    valor: number;
    gananciaEstimada: number;
    margenPct: number;
    descuentosOfrecidos: number;
    bajoMargenMinimo: number;
  };
  rechazadas: number;
  tasaCierrePct: number;
  ventasPorMes: { mes: string; etiqueta: string; ventas: number; ingresos: number; costos: number; ganancia: number }[];
  productos: { nombre: string; unidades: number; ingresos: number; costos: number; ganancia: number; margenPct: number }[];
  descuentos: {
    id: number;
    numero: string;
    cliente: string;
    esVenta: boolean;
    estado: EstadoCotizacion;
    fecha: string | null;
    descuentoPct: number;
    monto: number;
    margenPct: number;
    bajoMargenMinimo: boolean;
  }[];
}

export interface ArchivoRespaldo {
  nombre: string;
  tamanoBytes: number;
  fecha: string;
}

export interface EstadoRespaldos {
  habilitado: boolean;
  carpeta: string;
  horario: string;
  conservar: number;
  archivos: ArchivoRespaldo[];
}
