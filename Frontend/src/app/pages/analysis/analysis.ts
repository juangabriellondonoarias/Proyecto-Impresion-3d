import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { ApiServicio, leerErrorApi } from '../../core/api.service';
import { CopPipe, FechaPipe, PctPipe } from '../../core/format';
import { ETIQUETA_ESTADO, ResultadoAnalisis } from '../../core/models';

/** Geometría del gráfico de ventas por mes (unidades del viewBox del SVG). */
const ANCHO = 720;
const ALTO = 260;
const MARGEN = { arriba: 16, derecha: 12, abajo: 32, izquierda: 64 };

const compacto = new Intl.NumberFormat('es-CO', { notation: 'compact', maximumFractionDigits: 1 });

interface BarraMes {
  x: number;
  ancho: number;
  etiqueta: string;
  ingresos: string;
  ganancia: string;
  hitX: number;
  hitAncho: number;
}

@Component({
  selector: 'app-analysis',
  imports: [RouterLink, CopPipe, PctPipe, FechaPipe],
  templateUrl: './analysis.html',
  styleUrl: './analysis.css',
})
export class Analysis implements OnInit {
  private api = inject(ApiServicio);

  readonly etiquetaEstado = ETIQUETA_ESTADO;
  readonly periodos = [3, 6, 12, 24];
  readonly meses = signal(12);
  readonly datos = signal<ResultadoAnalisis | null>(null);
  readonly error = signal<string | null>(null);
  readonly cargando = signal(false);
  readonly mesActivo = signal<number | null>(null);
  readonly verTabla = signal(false);

  readonly ancho = ANCHO;
  readonly alto = ALTO;
  readonly margen = MARGEN;

  /** Escala vertical del gráfico mensual: incluye 0 y pérdidas si las hay. */
  private escala = computed(() => {
    const meses = this.datos()?.ventasPorMes ?? [];
    const maximo = Math.max(0, ...meses.map((m) => Math.max(m.ingresos, m.ganancia)));
    const minimo = Math.min(0, ...meses.map((m) => m.ganancia));
    const paso = pasoBonito((maximo - minimo) / 4 || 1);
    const tope = Math.ceil(maximo / paso) * paso || paso;
    const piso = Math.floor(minimo / paso) * paso;
    const altoUtil = ALTO - MARGEN.arriba - MARGEN.abajo;
    const y = (v: number) => MARGEN.arriba + ((tope - v) / (tope - piso)) * altoUtil;
    const marcas: { y: number; texto: string }[] = [];
    for (let v = piso; v <= tope + 1e-6; v += paso) marcas.push({ y: y(v), texto: compacto.format(v) });
    return { y, marcas, cero: y(0) };
  });

  readonly marcasY = computed(() => this.escala().marcas);
  readonly lineaCero = computed(() => this.escala().cero);

  readonly barras = computed<BarraMes[]>(() => {
    const meses = this.datos()?.ventasPorMes ?? [];
    if (!meses.length) return [];
    const { y, cero } = this.escala();
    const anchoUtil = ANCHO - MARGEN.izquierda - MARGEN.derecha;
    const grupo = anchoUtil / meses.length;
    const barra = Math.max(4, Math.min(18, grupo * 0.32));
    return meses.map((m, i) => {
      const centro = MARGEN.izquierda + grupo * i + grupo / 2;
      return {
        x: centro,
        ancho: barra,
        // «nov 2025» -> «nov 25», para que las etiquetas no se encimen
        etiqueta: m.etiqueta.replace(/ \d{2}(\d{2})$/, ' $1'),
        ingresos: trazoBarra(centro - barra - 1, barra, y(m.ingresos), cero),
        ganancia: trazoBarra(centro + 1, barra, y(m.ganancia), cero),
        hitX: MARGEN.izquierda + grupo * i,
        hitAncho: grupo,
      };
    });
  });

  /** Mostrar una etiqueta de mes de cada N para que no se encimen. */
  readonly saltoEtiquetas = computed(() => Math.ceil((this.datos()?.ventasPorMes.length ?? 1) / 12));

  readonly mesSeleccionado = computed(() => {
    const i = this.mesActivo();
    return i == null ? null : (this.datos()?.ventasPorMes[i] ?? null);
  });

  readonly hayVentas = computed(() => (this.datos()?.ventas.cantidad ?? 0) > 0);

  /** Reparto del dinero vendido: costo de productos, costos del pedido y ganancia. */
  readonly composicion = computed(() => {
    const v = this.datos()?.ventas;
    if (!v || v.ingresos <= 0) return [];
    const partes = [
      { nombre: 'Costo de productos', valor: v.costoProductos, clase: 's1' },
      { nombre: 'Costos del pedido', valor: v.costoPedido, clase: 's2' },
      { nombre: 'Ganancia', valor: Math.max(0, v.ganancia), clase: 's3' },
    ];
    const total = partes.reduce((s, p) => s + p.valor, 0) || 1;
    return partes.map((p) => ({ ...p, pct: (p.valor / total) * 100 }));
  });

  readonly gananciaMaxima = computed(() =>
    Math.max(1, ...(this.datos()?.productos.map((p) => Math.abs(p.ganancia)) ?? [1])),
  );

  ngOnInit(): void {
    this.cargar();
  }

  cambiarPeriodo(meses: number): void {
    this.meses.set(meses);
    this.mesActivo.set(null);
    this.cargar();
  }

  cargar(): void {
    this.cargando.set(true);
    this.api.analizar(this.meses()).subscribe({
      next: (d) => {
        this.datos.set(d);
        this.error.set(null);
        this.cargando.set(false);
      },
      error: (err) => {
        this.error.set(leerErrorApi(err).mensaje);
        this.cargando.set(false);
      },
    });
  }

  abs(valor: number): number {
    return Math.abs(valor);
  }

  /** Posición horizontal del tooltip, en % del ancho del gráfico. */
  posicionTooltip(i: number): number {
    const b = this.barras()[i];
    return b ? (b.x / ANCHO) * 100 : 0;
  }
}

/** Paso de 1, 2 o 5 × 10^n más cercano para las líneas de la grilla. */
function pasoBonito(bruto: number): number {
  const potencia = 10 ** Math.floor(Math.log10(bruto));
  const n = bruto / potencia;
  return (n <= 1 ? 1 : n <= 2 ? 2 : n <= 5 ? 5 : 10) * potencia;
}

/** Barra con el extremo de datos redondeado (4 px) y la base plana sobre el eje. */
function trazoBarra(x: number, ancho: number, yValor: number, yCero: number): string {
  const alto = Math.abs(yCero - yValor);
  if (alto < 0.5) return '';
  const r = Math.min(4, ancho / 2, alto);
  if (yValor < yCero) {
    return `M${x},${yCero} V${yValor + r} Q${x},${yValor} ${x + r},${yValor} H${x + ancho - r} Q${x + ancho},${yValor} ${x + ancho},${yValor + r} V${yCero} Z`;
  }
  return `M${x},${yCero} V${yValor - r} Q${x},${yValor} ${x + r},${yValor} H${x + ancho - r} Q${x + ancho},${yValor} ${x + ancho},${yValor - r} V${yCero} Z`;
}
