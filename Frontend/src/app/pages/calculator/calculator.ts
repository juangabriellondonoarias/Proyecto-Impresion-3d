import { NgTemplateOutlet } from '@angular/common';
import { Component, DestroyRef, ElementRef, OnInit, computed, inject, input, signal, viewChild } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Subject, debounceTime, forkJoin, switchMap, catchError, of } from 'rxjs';
import { ApiServicio, leerErrorApi } from '../../core/api.service';
import { CopPipe, HoursPipe, NumPipe, PctPipe } from '../../core/format';
import { Filamento, SolicitudPrecio, ResultadoPrecio, Producto, SolicitudProducto, Configuracion } from '../../core/models';

type NumKey = Exclude<keyof SolicitudPrecio, 'nombreProducto' | 'referencia' | 'material' | 'color' | 'margenMinimoPct' | 'pasoRedondeo'>;

interface FieldDef {
  key: NumKey;
  etiqueta: string;
  unit: string;
  hint?: string;
  step?: number;
}

@Component({
  selector: 'app-calculator',
  imports: [ReactiveFormsModule, NgTemplateOutlet, CopPipe, PctPipe, NumPipe, HoursPipe],
  templateUrl: './calculator.html',
  styleUrl: './calculator.css',
})
export class Calculator implements OnInit {
  private api = inject(ApiServicio);
  private fb = inject(FormBuilder).nonNullable;
  private destroyRef = inject(DestroyRef);

  /** ?producto=ID en la URL carga ese producto (lo usa la página de Productos). */
  readonly producto = input<string>();

  private resultsEl = viewChild<ElementRef<HTMLElement>>('results');

  readonly settings = signal<Configuracion | null>(null);
  readonly filaments = signal<Filamento[]>([]);
  readonly products = signal<Producto[]>([]);
  readonly result = signal<ResultadoPrecio | null>(null);
  readonly loading = signal(false);
  readonly loadError = signal<string | null>(null);
  readonly calcError = signal<string | null>(null);
  readonly serverErrors = signal<Record<string, string>>({});
  readonly saveMessage = signal<string | null>(null);
  /** Después del primer clic en «Calcular precio», los resultados se actualizan solos. */
  readonly live = signal(false);

  readonly form = this.fb.group({
    productoId: this.fb.control<number | null>(null),
    filamentoId: this.fb.control<number | null>(null),
    nombreProducto: [''],
    referencia: [''],
    material: [''],
    color: [''],
    cantidad: this.fb.control<number | null>(1, [Validators.required, Validators.min(1)]),
    precioFilamentoKg: this.fb.control<number | null>(null, [Validators.required, Validators.min(0)]),
    pesoPiezaG: this.fb.control<number | null>(null, [Validators.required, Validators.min(0)]),
    desperdicioPorUnidadG: this.fb.control<number | null>(0, Validators.min(0)),
    desperdicioPorLoteG: this.fb.control<number | null>(0, Validators.min(0)),
    tiempoImpresionPorUnidadMin: this.fb.control<number | null>(null, Validators.min(0)),
    tiempoImpresionLoteMin: this.fb.control<number | null>(null, Validators.min(0)),
    tiempoPreparacionMin: this.fb.control<number | null>(0, Validators.min(0)),
    posprocesoPorUnidadMin: this.fb.control<number | null>(0, Validators.min(0)),
    posprocesoPorLoteMin: this.fb.control<number | null>(0, Validators.min(0)),
    potenciaImpresoraW: this.fb.control<number | null>(null, Validators.min(0)),
    precioKwh: this.fb.control<number | null>(null, Validators.min(0)),
    valorHoraManoObra: this.fb.control<number | null>(null, Validators.min(0)),
    costoMaquinaPorHora: this.fb.control<number | null>(null, Validators.min(0)),
    costoDiseno: this.fb.control<number | null>(0, Validators.min(0)),
    empaquePorUnidad: this.fb.control<number | null>(0, Validators.min(0)),
    costoEnvio: this.fb.control<number | null>(0, Validators.min(0)),
    otrosCostos: this.fb.control<number | null>(0, Validators.min(0)),
    margenObjetivoPct: this.fb.control<number | null>(null, [Validators.required, Validators.min(0), Validators.max(95)]),
    descuentoPct: this.fb.control<number | null>(0, [Validators.min(0), Validators.max(100)]),
  });

  readonly materialFields: FieldDef[] = [
    { key: 'precioFilamentoKg', etiqueta: 'Precio del filamento', unit: 'COP/kg', step: 1000 },
    { key: 'pesoPiezaG', etiqueta: 'Peso de la pieza', unit: 'g', hint: 'Por unidad, según el laminador.' },
    { key: 'desperdicioPorUnidadG', etiqueta: 'Soportes y desperdicio', unit: 'g', hint: 'Por unidad.' },
    { key: 'desperdicioPorLoteG', etiqueta: 'Purgas del lote', unit: 'g', hint: 'Una vez por lote (torre de purga, línea inicial).' },
  ];

  readonly timeFields: FieldDef[] = [
    { key: 'tiempoImpresionPorUnidadMin', etiqueta: 'Impresión por unidad', unit: 'min' },
    { key: 'tiempoImpresionLoteMin', etiqueta: 'Tiempo real del lote', unit: 'min', hint: 'Opcional. Si lo llenas, reemplaza «por unidad × cantidad».' },
    { key: 'tiempoPreparacionMin', etiqueta: 'Preparación', unit: 'min', hint: 'Laminar, cargar filamento, nivelar. Una vez por pedido.' },
    { key: 'posprocesoPorUnidadMin', etiqueta: 'Posprocesado por unidad', unit: 'min', hint: 'Retirar soportes, lijar, pintar.' },
    { key: 'posprocesoPorLoteMin', etiqueta: 'Posprocesado por lote', unit: 'min', hint: 'Limpiar la cama, empacar todo el pedido.' },
  ];

  readonly machineFields: FieldDef[] = [
    { key: 'potenciaImpresoraW', etiqueta: 'Consumo de la impresora', unit: 'W', hint: 'Promedio real, no el máximo de la fuente.' },
    { key: 'precioKwh', etiqueta: 'Precio del kWh', unit: 'COP' },
    { key: 'costoMaquinaPorHora', etiqueta: 'Mantenimiento y depreciación', unit: 'COP/h', hint: 'Sale de Configuración.' },
    { key: 'valorHoraManoObra', etiqueta: 'Mano de obra', unit: 'COP/h' },
  ];

  readonly extraFields: FieldDef[] = [
    { key: 'costoDiseno', etiqueta: 'Diseño', unit: 'COP', hint: 'Por pedido.', step: 1000 },
    { key: 'empaquePorUnidad', etiqueta: 'Empaque', unit: 'COP', hint: 'Por unidad.', step: 100 },
    { key: 'costoEnvio', etiqueta: 'Envío', unit: 'COP', hint: 'Por pedido, si lo asumes tú.', step: 1000 },
    { key: 'otrosCostos', etiqueta: 'Otros costos', unit: 'COP', hint: 'Por pedido.', step: 1000 },
  ];

  readonly priceFields: FieldDef[] = [
    { key: 'margenObjetivoPct', etiqueta: 'Margen de ganancia deseado', unit: '%', hint: 'Sobre el precio de venta. Máximo 95 %.' },
    { key: 'descuentoPct', etiqueta: 'Descuento propuesto', unit: '%' },
  ];

  /** Partes del costo para la barra de composición. */
  readonly breakdown = computed(() => {
    const r = this.result();
    if (!r || r.costoTotal <= 0) return [];
    return [
      { etiqueta: 'Material', value: r.costoMaterialLote, cls: 'c1' },
      { etiqueta: 'Mano de obra', value: r.costoManoObra, cls: 'c2' },
      { etiqueta: 'Máquina', value: r.costoMaquina, cls: 'c3' },
      { etiqueta: 'Electricidad', value: r.costoElectrico, cls: 'c4' },
      { etiqueta: 'Otros', value: r.totalOtrosCostos, cls: 'c5' },
    ].map((p) => ({ ...p, pct: (p.value / r.costoTotal) * 100 }));
  });

  private calc$ = new Subject<void>();

  ngOnInit(): void {
    this.calc$
      .pipe(
        switchMap(() => {
          this.loading.set(true);
          return this.api.calcularPrecio(this.buildRequest()).pipe(
            catchError((err) => {
              const e = leerErrorApi(err);
              this.calcError.set(e.mensaje);
              this.serverErrors.set(e.errores);
              return of(null);
            }),
          );
        }),
        takeUntilDestroyed(this.destroyRef),
      )
      .subscribe((res) => {
        this.loading.set(false);
        if (res) {
          this.result.set(res);
          this.calcError.set(null);
          this.serverErrors.set({});
        }
      });

    this.form.valueChanges.pipe(debounceTime(350), takeUntilDestroyed(this.destroyRef)).subscribe(() => {
      if (this.live() && this.form.valid) this.calc$.next();
    });

    this.form.controls.productoId.valueChanges.pipe(takeUntilDestroyed(this.destroyRef)).subscribe((id) => {
      const p = this.products().find((x) => x.id === id);
      if (p) this.applyProduct(p);
    });

    this.form.controls.filamentoId.valueChanges.pipe(takeUntilDestroyed(this.destroyRef)).subscribe((id) => {
      const f = this.filaments().find((x) => x.id === id);
      if (f) this.applyFilament(f);
    });

    this.loadCatalog();
  }

  loadCatalog(): void {
    this.loadError.set(null);
    forkJoin({
      settings: this.api.obtenerConfiguracion(),
      filaments: this.api.listarFilamentos(),
      products: this.api.listarProductos(),
    }).subscribe({
      next: ({ settings, filaments, products }) => {
        this.settings.set(settings);
        this.filaments.set(filaments);
        this.products.set(products);
        this.applySettings(settings);
        const id = Number(this.producto());
        if (id) this.form.controls.productoId.setValue(id);
      },
      error: (err) => this.loadError.set(leerErrorApi(err).mensaje),
    });
  }

  calculate(): void {
    this.form.markAllAsTouched();
    if (this.form.invalid) {
      this.calcError.set('Revisa los campos marcados en rojo.');
      return;
    }
    this.live.set(true);
    this.calc$.next();
    // En celular los resultados quedan debajo del formulario.
    if (window.matchMedia('(max-width: 1100px)').matches) {
      setTimeout(() => this.resultsEl()?.nativeElement.scrollIntoView({ behavior: 'smooth', block: 'start' }), 150);
    }
  }

  reset(): void {
    this.form.reset();
    this.result.set(null);
    this.live.set(false);
    this.calcError.set(null);
    this.serverErrors.set({});
    this.saveMessage.set(null);
    const s = this.settings();
    if (s) this.applySettings(s);
  }

  saveAsProduct(): void {
    const v = this.form.getRawValue();
    if (!v.nombreProducto.trim()) {
      this.saveMessage.set('Escribe el nombre del producto para guardarlo.');
      return;
    }
    const body: SolicitudProducto = {
      nombre: v.nombreProducto,
      referencia: v.referencia || null,
      filamentoId: v.filamentoId,
      pesoG: v.pesoPiezaG ?? 0,
      desperdicioPorUnidadG: v.desperdicioPorUnidadG,
      tiempoImpresionMinPorUnidad: v.tiempoImpresionPorUnidadMin,
      posprocesoMinPorUnidad: v.posprocesoPorUnidadMin,
      empaquePorUnidad: v.empaquePorUnidad,
      notas: this.products().find((p) => p.id === v.productoId)?.notas ?? null,
    };
    this.api.guardarProducto(v.productoId ?? undefined, body).subscribe({
      next: (saved) => {
        this.saveMessage.set(v.productoId ? 'Producto actualizado en el catálogo.' : 'Producto guardado en el catálogo.');
        this.api.listarProductos().subscribe((list) => {
          this.products.set(list);
          this.form.controls.productoId.setValue(saved.id ?? null, { emitEvent: false });
        });
      },
      error: (err) => this.saveMessage.set(leerErrorApi(err).mensaje),
    });
  }

  isInvalid(key: string): boolean {
    const c = this.form.get(key);
    return !!(c && c.invalid && c.touched) || !!this.serverErrors()[key];
  }

  errorText(key: string): string | null {
    const server = this.serverErrors()[key];
    if (server) return server;
    const c = this.form.get(key);
    if (!c || !c.touched || !c.errors) return null;
    if (c.errors['required']) return 'Campo obligatorio.';
    if (c.errors['min']) return `Mínimo ${c.errors['min'].min}.`;
    if (c.errors['max']) return `Máximo ${c.errors['max'].max}.`;
    return 'Valor no válido.';
  }

  private applySettings(s: Configuracion): void {
    this.form.patchValue({
      potenciaImpresoraW: s.potenciaImpresoraW,
      precioKwh: s.precioKwh,
      valorHoraManoObra: s.valorHoraManoObra,
      costoMaquinaPorHora: Math.round(s.costoMaquinaPorHora ?? 0),
      margenObjetivoPct: s.margenPorDefectoPct,
    });
  }

  private applyFilament(f: Filamento): void {
    this.form.patchValue({
      material: f.material,
      color: f.color,
      precioFilamentoKg: Math.round(f.costoPorKg ?? 0),
    });
  }

  private applyProduct(p: Producto): void {
    this.form.patchValue({
      nombreProducto: p.nombre,
      referencia: p.referencia ?? '',
      pesoPiezaG: p.pesoG,
      desperdicioPorUnidadG: p.desperdicioPorUnidadG ?? 0,
      tiempoImpresionPorUnidadMin: p.tiempoImpresionMinPorUnidad,
      posprocesoPorUnidadMin: p.posprocesoMinPorUnidad ?? 0,
      empaquePorUnidad: p.empaquePorUnidad ?? 0,
    });
    if (p.filamento?.id) this.form.controls.filamentoId.setValue(p.filamento.id);
  }

  private buildRequest(): SolicitudPrecio {
    const { productoId, filamentoId, ...v } = this.form.getRawValue();
    const s = this.settings();
    return { ...v, margenMinimoPct: s?.margenMinimoPct ?? 0, pasoRedondeo: s?.pasoRedondeo ?? 0 };
  }
}
