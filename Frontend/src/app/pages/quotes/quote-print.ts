import { Component, OnInit, inject, input, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { forkJoin } from 'rxjs';
import { ApiServicio, leerErrorApi } from '../../core/api.service';
import { CopPipe, FechaPipe, PctPipe } from '../../core/format';
import { Cotizacion, Configuracion } from '../../core/models';

/**
 * Documento para el cliente. Se imprime o se guarda como PDF con el diálogo de impresión del navegador.
 * No muestra costos, márgenes ni notas internas.
 */
@Component({
  selector: 'app-quote-print',
  imports: [RouterLink, CopPipe, PctPipe, FechaPipe],
  templateUrl: './quote-print.html',
  styleUrl: './quote-print.css',
})
export class QuotePrint implements OnInit {
  private api = inject(ApiServicio);

  readonly id = input<string>();
  readonly quote = signal<Cotizacion | null>(null);
  readonly settings = signal<Configuracion | null>(null);
  readonly error = signal<string | null>(null);

  ngOnInit(): void {
    forkJoin({ quote: this.api.obtenerCotizacion(Number(this.id())), settings: this.api.obtenerConfiguracion() }).subscribe({
      next: ({ quote, settings }) => {
        this.quote.set(quote);
        this.settings.set(settings);
        // El nombre del archivo PDF sale del título de la página.
        document.title = `${quote.numero} ${quote.nombreCliente}`;
      },
      error: (err) => this.error.set(leerErrorApi(err).mensaje),
    });
  }

  print(): void {
    window.print();
  }
}
