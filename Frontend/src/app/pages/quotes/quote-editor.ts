import { Component, DestroyRef, OnInit, computed, inject, input, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { FormArray, FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { Subject, catchError, debounceTime, forkJoin, of, switchMap } from 'rxjs';
import { ApiServicio, leerErrorApi } from '../../core/api.service';
import { CopPipe, FechaPipe, PctPipe, todayIso } from '../../core/format';
import { Producto, ETIQUETA_ESTADO, Cotizacion, ItemCotizacion, SolicitudCotizacion, EstadoCotizacion, ResumenCotizacion, Configuracion } from '../../core/models';

@Component({
  selector: 'app-quote-editor',
  imports: [ReactiveFormsModule, RouterLink, CopPipe, PctPipe, FechaPipe],
  templateUrl: './quote-editor.html',
  styleUrl: './quote-editor.css',
})
export class QuoteEditor implements OnInit {
  private api = inject(ApiServicio);
  private fb = inject(FormBuilder).nonNullable;
  private router = inject(Router);
  private destroyRef = inject(DestroyRef);

  /** Parámetro :id de la ruta. Vacío en «nueva». */
  readonly id = input<string>();

  readonly statusLabel = ETIQUETA_ESTADO;
  /** Estados que se eligen a mano; VENDIDA solo con «Registrar venta». */
  readonly manualStatuses: EstadoCotizacion[] = ['BORRADOR', 'ENVIADA', 'ACEPTADA', 'RECHAZADA'];
  readonly settings = signal<Configuracion | null>(null);
  readonly products = signal<Producto[]>([]);
  readonly quote = signal<Cotizacion | null>(null);
  readonly resumen = signal<ResumenCotizacion | null>(null);
  readonly mensaje = signal<{ ok: boolean; text: string } | null>(null);
  readonly serverErrors = signal<Record<string, string>>({});
  readonly busy = signal(false);
  readonly pricingRows = signal(0);
  readonly soldDate = signal(todayIso());

  readonly form = this.fb.group({
    nombreCliente: ['', Validators.required],
    contactoCliente: [''],
    fechaEmision: [todayIso(), Validators.required],
    diasVigencia: this.fb.control<number | null>(15, Validators.min(0)),
    margenPct: this.fb.control<number | null>(40, [Validators.required, Validators.min(0), Validators.max(95)]),
    descuentoPct: this.fb.control<number | null>(0, [Validators.min(0), Validators.max(100)]),
    costoDiseno: this.fb.control<number | null>(0, Validators.min(0)),
    costoEnvio: this.fb.control<number | null>(0, Validators.min(0)),
    otrosCostos: this.fb.control<number | null>(0, Validators.min(0)),
    condiciones: [''],
    notas: [''],
    items: this.fb.array<FormGroup>([]),
  });

  /** Para no pisar una descripción que el usuario escribió a mano. */
  private autoDescription = new WeakMap<FormGroup, string>();
  private preview$ = new Subject<void>();

  readonly isSale = computed(() => this.quote()?.estado === 'VENDIDA');
  readonly isNew = computed(() => !this.quote());

  get items(): FormArray<FormGroup> {
    return this.form.controls.items;
  }

  ngOnInit(): void {
    this.preview$
      .pipe(
        debounceTime(300),
        switchMap(() => this.api.vistaPreviaCotizacion(this.buildRequest()).pipe(catchError(() => of(null)))),
        takeUntilDestroyed(this.destroyRef),
      )
      .subscribe((s) => s && this.resumen.set(s));

    this.form.valueChanges.pipe(takeUntilDestroyed(this.destroyRef)).subscribe(() => this.preview$.next());

    const id = Number(this.id());
    forkJoin({
      settings: this.api.obtenerConfiguracion(),
      products: this.api.listarProductos(),
      quote: id ? this.api.obtenerCotizacion(id) : of(null),
    }).subscribe({
      next: ({ settings, products, quote }) => {
        this.settings.set(settings);
        this.products.set(products);
        if (quote) {
          this.loadQuote(quote);
        } else {
          this.form.patchValue({
            diasVigencia: settings.diasVigenciaCotizacion ?? 15,
            margenPct: settings.margenPorDefectoPct,
            condiciones: settings.condicionesPorDefecto ?? '',
          });
          this.addItem();
        }
        this.form.markAsPristine();
        this.preview$.next();
      },
      error: (err) => this.mensaje.set({ ok: false, text: leerErrorApi(err).mensaje }),
    });
  }

  addItem(item?: ItemCotizacion): void {
    const group = this.fb.group({
      productoId: this.fb.control<number | null>(item?.productoId ?? null),
      filamentoId: this.fb.control<number | null>(item?.filamentoId ?? null),
      descripcion: [item?.descripcion ?? '', Validators.required],
      cantidad: this.fb.control<number | null>(item?.cantidad ?? 1, [Validators.required, Validators.min(1)]),
      costoUnitario: this.fb.control<number | null>(item?.costoUnitario ?? 0, [Validators.required, Validators.min(0)]),
      precioUnitario: this.fb.control<number | null>(item?.precioUnitario ?? 0, [Validators.required, Validators.min(0)]),
      gramosMaterialPorUnidad: this.fb.control<number>(item?.gramosMaterialPorUnidad ?? 0),
    });
    if (item) this.autoDescription.set(group, item.descripcion);

    // Elegir producto o cambiar la cantidad recalcula costo y precio desde el catálogo.
    group.controls.productoId.valueChanges.pipe(takeUntilDestroyed(this.destroyRef)).subscribe(() => this.priceRow(group));
    group.controls.cantidad.valueChanges
      .pipe(debounceTime(400), takeUntilDestroyed(this.destroyRef))
      .subscribe(() => group.controls.productoId.value && this.priceRow(group));

    this.items.push(group);
  }

  removeItem(index: number): void {
    this.items.removeAt(index);
    this.form.markAsDirty();
  }

  /** Vuelve a calcular todas las líneas del catálogo con el margen actual. */
  repriceAll(): void {
    for (const group of this.items.controls) {
      if (group.value.productoId) this.priceRow(group);
    }
  }

  lineTotal(group: FormGroup): number {
    return (group.value.cantidad ?? 0) * (group.value.precioUnitario ?? 0);
  }

  lineMargin(group: FormGroup): number | null {
    const price = group.value.precioUnitario ?? 0;
    return price > 0 ? ((price - (group.value.costoUnitario ?? 0)) / price) * 100 : null;
  }

  save(): void {
    this.form.markAllAsTouched();
    if (!this.items.length) {
      this.mensaje.set({ ok: false, text: 'Agrega al menos un producto.' });
      return;
    }
    if (this.form.invalid) {
      this.mensaje.set({ ok: false, text: 'Revisa los campos marcados en rojo.' });
      return;
    }
    this.busy.set(true);
    this.api.guardarCotizacion(this.quote()?.id, this.buildRequest()).subscribe({
      next: (saved) => {
        const wasNew = this.isNew();
        this.busy.set(false);
        this.quote.set(saved);
        this.serverErrors.set({});
        this.form.markAsPristine();
        this.mensaje.set({ ok: true, text: `Cotización ${saved.numero} guardada.` });
        if (wasNew) this.router.navigate(['/cotizaciones', saved.id], { replaceUrl: true });
      },
      error: (err) => this.fail(err),
    });
  }

  setStatus(estado: EstadoCotizacion): void {
    const q = this.quote();
    if (!q) return;
    if (this.form.dirty) {
      this.mensaje.set({ ok: false, text: 'Guarda los cambios antes de cambiar el estado.' });
      return;
    }
    if (q.estado === 'VENDIDA' && !confirm('¿Anular la venta? El filamento descontado vuelve al inventario.')) return;
    this.busy.set(true);
    this.api.cambiarEstadoCotizacion(q.id, estado, estado === 'VENDIDA' ? this.soldDate() : undefined).subscribe({
      next: (updated) => {
        this.busy.set(false);
        this.loadQuote(updated);
        this.mensaje.set({
          ok: true,
          text: estado === 'VENDIDA' ? 'Venta registrada. Se descontó el filamento del inventario.' : `Estado: ${this.statusLabel[estado]}.`,
        });
      },
      error: (err) => this.fail(err),
    });
  }

  remove(): void {
    const q = this.quote();
    if (!q || !confirm(`¿Eliminar la cotización ${q.numero}? Esta acción no se puede deshacer.`)) return;
    this.api.eliminarCotizacion(q.id).subscribe({
      next: () => this.router.navigate(['/cotizaciones']),
      error: (err) => this.fail(err),
    });
  }

  invalid(control: { invalid: boolean; touched: boolean } | null, key?: string): boolean {
    return !!(control && control.invalid && control.touched) || (!!key && !!this.serverErrors()[key]);
  }

  private priceRow(group: FormGroup): void {
    const productId = group.value.productoId as number | null;
    const quantity = Number(group.value.cantidad);
    if (!productId) {
      group.patchValue({ filamentoId: null, gramosMaterialPorUnidad: 0 });
      return;
    }
    if (!(quantity >= 1)) return;
    this.pricingRows.update((n) => n + 1);
    this.api.precioItem(productId, quantity, this.form.controls.margenPct.value ?? 0).subscribe({
      next: (p) => {
        this.pricingRows.update((n) => n - 1);
        const current = group.value.descripcion as string;
        const keepDescription = current && current !== this.autoDescription.get(group);
        this.autoDescription.set(group, p.descripcion);
        group.patchValue({
          filamentoId: p.filamentoId,
          descripcion: keepDescription ? current : p.descripcion,
          costoUnitario: p.costoUnitario,
          precioUnitario: p.precioUnitario,
          gramosMaterialPorUnidad: p.gramosMaterialPorUnidad,
        });
      },
      error: (err) => {
        this.pricingRows.update((n) => n - 1);
        this.mensaje.set({ ok: false, text: leerErrorApi(err).mensaje });
      },
    });
  }

  private loadQuote(q: Cotizacion): void {
    this.quote.set(q);
    this.resumen.set(q.resumen);
    this.items.clear();
    q.items.forEach((item) => this.addItem(item));
    this.form.reset(
      {
        nombreCliente: q.nombreCliente,
        contactoCliente: q.contactoCliente ?? '',
        fechaEmision: q.fechaEmision,
        diasVigencia: q.diasVigencia,
        margenPct: q.margenPct,
        descuentoPct: q.descuentoPct,
        costoDiseno: q.costoDiseno,
        costoEnvio: q.costoEnvio,
        otrosCostos: q.otrosCostos,
        condiciones: q.condiciones ?? '',
        notas: q.notas ?? '',
      },
      { emitEvent: false },
    );
    // Una venta registrada no se edita: hay que anularla primero.
    if (q.estado === 'VENDIDA') this.form.disable({ emitEvent: false });
    else this.form.enable({ emitEvent: false });
  }

  private buildRequest(): SolicitudCotizacion {
    const v = this.form.getRawValue();
    return {
      ...v,
      contactoCliente: v.contactoCliente || null,
      condiciones: v.condiciones || null,
      notas: v.notas || null,
      items: v.items.map((i) => ({ ...i }) as ItemCotizacion),
    };
  }

  private fail(err: unknown): void {
    this.busy.set(false);
    const e = leerErrorApi(err);
    this.serverErrors.set(e.errores);
    this.mensaje.set({ ok: false, text: e.mensaje });
  }
}
