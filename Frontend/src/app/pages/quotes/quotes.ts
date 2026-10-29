import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { ApiServicio, leerErrorApi } from '../../core/api.service';
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
}
