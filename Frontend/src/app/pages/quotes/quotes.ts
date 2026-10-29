import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { ApiServicio, guardarArchivo, leerErrorApi } from '../../core/api.service';
import { CopPipe, FechaPipe, PctPipe } from '../../core/format';
import { ETIQUETA_ESTADO, Cotizacion } from '../../core/models';

type Tab = 'cotizaciones' | 'ventas';

@Component({
  selector: 'app-quotes',
  imports: [RouterLink, CopPipe, PctPipe, FechaPipe],
  templateUrl: './quotes.html',
  styleUrl: './quotes.css',
})
export class Quotes implements OnInit {
  private api = inject(ApiServicio);

  readonly statusLabel = ETIQUETA_ESTADO;
  readonly quotes = signal<Cotizacion[]>([]);
  readonly tab = signal<Tab>('cotizaciones');
  readonly error = signal<string | null>(null);
  readonly loaded = signal(false);
  readonly exportando = signal(false);
  readonly mensajeExportar = signal<{ ok: boolean; texto: string } | null>(null);

  readonly sales = computed(() => this.quotes().filter((q) => q.estado === 'VENDIDA'));
  readonly open = computed(() => this.quotes().filter((q) => q.estado !== 'VENDIDA'));
  /** Cotizaciones que aún pueden cerrarse (no rechazadas). */
  readonly pending = computed(() => this.open().filter((q) => q.estado !== 'RECHAZADA'));

  readonly visible = computed(() => (this.tab() === 'ventas' ? this.sales() : this.open()));

  readonly salesTotal = computed(() => this.sales().reduce((s, q) => s + q.resumen.total, 0));
  readonly salesProfit = computed(() => this.sales().reduce((s, q) => s + q.resumen.ganancia, 0));
  readonly pendingTotal = computed(() => this.pending().reduce((s, q) => s + q.resumen.total, 0));

  ngOnInit(): void {
    this.api.listarCotizaciones().subscribe({
      next: (list) => {
        this.quotes.set(list);
        this.loaded.set(true);
      },
      error: (err) => this.error.set(leerErrorApi(err).mensaje),
    });
  }

  /** Descarga el Excel del periodo. Sin fechas, todo el historial. */
  exportarExcel(desde: string, hasta: string): void {
    this.exportando.set(true);
    this.mensajeExportar.set(null);
    this.api.descargarExcel(desde || null, hasta || null).subscribe({
      next: (respuesta) => {
        guardarArchivo(respuesta, 'costos3d-ventas-y-cotizaciones.xlsx');
        this.exportando.set(false);
        this.mensajeExportar.set({ ok: true, texto: 'Excel descargado. Revisa tu carpeta de descargas.' });
      },
      error: async (err) => {
        this.exportando.set(false);
        // Con responseType «blob», el error del backend llega como archivo: se lee como texto.
        let texto = leerErrorApi(err).mensaje;
        if (err?.error instanceof Blob) {
          try {
            texto = JSON.parse(await err.error.text()).mensaje ?? texto;
          } catch {
            /* se deja el mensaje genérico */
          }
        }
        this.mensajeExportar.set({ ok: false, texto });
      },
    });
  }
}
