import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { forkJoin, of, switchMap } from 'rxjs';
import { ApiServicio, leerErrorApi } from '../../core/api.service';
import { CopPipe, FechaPipe, PctPipe } from '../../core/format';
import { Filamento, ResultadoPrecio, Producto, Cotizacion, Configuracion } from '../../core/models';

interface ProductPrice {
  product: Producto;
  result: ResultadoPrecio;
}

@Component({
  selector: 'app-dashboard',
  imports: [RouterLink, CopPipe, PctPipe, FechaPipe],
  templateUrl: './dashboard.html',
  styleUrl: './dashboard.css',
})
export class Dashboard implements OnInit {
  private api = inject(ApiServicio);

  readonly settings = signal<Configuracion | null>(null);
  readonly filaments = signal<Filamento[]>([]);
  readonly products = signal<Producto[]>([]);
  readonly prices = signal<ProductPrice[]>([]);
  readonly quotes = signal<Cotizacion[]>([]);

  // Ventas reales (registradas) y cotizaciones se cuentan por separado.
  readonly sales = computed(() => this.quotes().filter((q) => q.estado === 'VENDIDA'));
  readonly openQuotes = computed(() => this.quotes().filter((q) => q.estado !== 'VENDIDA' && q.estado !== 'RECHAZADA'));
  readonly salesTotal = computed(() => this.sales().reduce((s, q) => s + q.resumen.total, 0));
  readonly salesProfit = computed(() => this.sales().reduce((s, q) => s + q.resumen.ganancia, 0));
  readonly openTotal = computed(() => this.openQuotes().reduce((s, q) => s + q.resumen.total, 0));
  readonly recentSales = computed(() => this.sales().slice(0, 5));
  readonly error = signal<string | null>(null);

  readonly inventoryValue = computed(() =>
    this.filaments().reduce((sum, f) => sum + (f.pesoDisponibleG / 1000) * (f.costoPorKg ?? 0), 0),
  );

  readonly lowStock = computed(() => this.filaments().filter((f) => f.pesoInicialG > 0 && f.pesoDisponibleG / f.pesoInicialG < 0.2));

  readonly hasDemo = computed(() => this.filaments().some((f) => f.demo) || this.products().some((p) => p.demo));

  ngOnInit(): void {
    this.api.listarCotizaciones().subscribe({
      next: (list) => this.quotes.set(list),
      error: (err) => this.error.set(leerErrorApi(err).mensaje),
    });
    forkJoin({ settings: this.api.obtenerConfiguracion(), filaments: this.api.listarFilamentos(), products: this.api.listarProductos() })
      .pipe(
        switchMap((data) => {
          this.settings.set(data.settings);
          this.filaments.set(data.filaments);
          this.products.set(data.products);
          const priced = data.products.filter((p) => p.filamento);
          if (!priced.length) return of([] as ProductPrice[]);
          // Precio de referencia: 1 unidad con la configuración por defecto, sin costos de pedido.
          return forkJoin(
            priced.map((p) =>
              this.api
                .calcularPrecio({
                  nombreProducto: p.nombre,
                  referencia: p.referencia ?? '',
                  material: p.filamento!.material,
                  color: p.filamento!.color,
                  precioFilamentoKg: p.filamento!.costoPorKg ?? 0,
                  pesoPiezaG: p.pesoG,
                  desperdicioPorUnidadG: p.desperdicioPorUnidadG,
                  desperdicioPorLoteG: 0,
                  cantidad: 1,
                  tiempoImpresionPorUnidadMin: p.tiempoImpresionMinPorUnidad,
                  tiempoImpresionLoteMin: 0,
                  tiempoPreparacionMin: 0,
                  posprocesoPorUnidadMin: p.posprocesoMinPorUnidad,
                  posprocesoPorLoteMin: 0,
                  potenciaImpresoraW: data.settings.potenciaImpresoraW,
                  precioKwh: data.settings.precioKwh,
                  valorHoraManoObra: data.settings.valorHoraManoObra,
                  costoMaquinaPorHora: data.settings.costoMaquinaPorHora ?? 0,
                  costoDiseno: 0,
                  empaquePorUnidad: p.empaquePorUnidad,
                  costoEnvio: 0,
                  otrosCostos: 0,
                  margenObjetivoPct: data.settings.margenPorDefectoPct,
                  descuentoPct: 0,
                  margenMinimoPct: data.settings.margenMinimoPct,
                  pasoRedondeo: data.settings.pasoRedondeo,
                })
                .pipe(switchMap((result) => of({ product: p, result }))),
            ),
          );
        }),
      )
      .subscribe({
        next: (prices) => this.prices.set(prices),
        error: (err) => this.error.set(leerErrorApi(err).mensaje),
      });
  }
}
