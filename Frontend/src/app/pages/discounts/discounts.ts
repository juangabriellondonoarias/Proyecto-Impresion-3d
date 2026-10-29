import { Component, DestroyRef, OnInit, computed, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { Subject, catchError, debounceTime, forkJoin, of, switchMap } from 'rxjs';
import { ApiServicio, leerErrorApi } from '../../core/api.service';
import { CopPipe, NumPipe, PctPipe } from '../../core/format';
import { Producto, ResultadoDescuentos, SolicitudDescuentos } from '../../core/models';

@Component({
  selector: 'app-discounts',
  imports: [ReactiveFormsModule, RouterLink, CopPipe, PctPipe, NumPipe],
  templateUrl: './discounts.html',
  styleUrl: './discounts.css',
})
export class Discounts implements OnInit {
  private api = inject(ApiServicio);
  private fb = inject(FormBuilder).nonNullable;
  private destroyRef = inject(DestroyRef);

  readonly productos = signal<Producto[]>([]);
  readonly resultado = signal<ResultadoDescuentos | null>(null);
  readonly error = signal<string | null>(null);
  readonly erroresCampo = signal<Record<string, string>>({});
  readonly cargando = signal(false);
  readonly descuentos = signal<number[]>([0, 5, 10, 15, 20, 25, 30]);

  readonly form = this.fb.group({
    productoId: this.fb.control<number | null>(null, Validators.required),
    cantidad: this.fb.control<number | null>(10, [Validators.required, Validators.min(1)]),
    cantidadReferencia: this.fb.control<number | null>(1, [Validators.required, Validators.min(1)]),
    margenPct: this.fb.control<number | null>(null, [Validators.min(0), Validators.max(95)]),
    margenMinimoPct: this.fb.control<number | null>(null, [Validators.min(0), Validators.max(95)]),
    tiempoPreparacionMin: this.fb.control<number | null>(0, Validators.min(0)),
    costoDiseno: this.fb.control<number | null>(0, Validators.min(0)),
    costoEnvio: this.fb.control<number | null>(0, Validators.min(0)),
    otrosCostos: this.fb.control<number | null>(0, Validators.min(0)),
  });

  /** Mayor descuento máximo de la escala, para dibujar las barras a la misma escala. */
  readonly maximoEscala = computed(() => Math.max(1, ...(this.resultado()?.escala.map((e) => e.descuentoMaximoPct) ?? [1])));

  private comparar$ = new Subject<void>();

  ngOnInit(): void {
    this.comparar$
      .pipe(
        debounceTime(300),
        switchMap(() => {
          this.cargando.set(true);
          return this.api.compararDescuentos(this.solicitud()).pipe(
            catchError((err) => {
              const e = leerErrorApi(err);
              this.error.set(e.mensaje);
              this.erroresCampo.set(e.errores);
              return of(null);
            }),
          );
        }),
        takeUntilDestroyed(this.destroyRef),
      )
      .subscribe((r) => {
        this.cargando.set(false);
        if (r) {
          this.resultado.set(r);
          this.error.set(null);
          this.erroresCampo.set({});
        }
      });

    this.form.valueChanges.pipe(takeUntilDestroyed(this.destroyRef)).subscribe(() => this.recalcular());

    forkJoin({ configuracion: this.api.obtenerConfiguracion(), productos: this.api.listarProductos() }).subscribe({
      next: ({ configuracion, productos }) => {
        this.productos.set(productos);
        this.form.patchValue({
          margenPct: configuracion.margenPorDefectoPct,
          margenMinimoPct: configuracion.margenMinimoPct,
          productoId: productos[0]?.id ?? null,
        });
      },
      error: (err) => this.error.set(leerErrorApi(err).mensaje),
    });
  }

  agregarDescuento(campo: HTMLInputElement): void {
    const valor = Number(campo.value.replace(',', '.'));
    if (campo.value.trim() === '' || !Number.isFinite(valor) || valor < 0 || valor > 100) {
      this.error.set('El descuento debe estar entre 0 y 100 %.');
      return;
    }
    if (this.descuentos().length >= 12) {
      this.error.set('Puedes comparar como máximo 12 descuentos.');
      return;
    }
    this.descuentos.update((lista) => [...new Set([...lista, valor])].sort((a, b) => a - b));
    campo.value = '';
    this.error.set(null);
    this.recalcular();
  }

  quitarDescuento(valor: number): void {
    this.descuentos.update((lista) => lista.filter((d) => d !== valor));
    this.recalcular();
  }

  invalido(clave: string): boolean {
    const c = this.form.get(clave);
    return !!(c && c.invalid && c.touched) || !!this.erroresCampo()[clave];
  }

  private recalcular(): void {
    if (this.form.valid && this.descuentos().length) this.comparar$.next();
  }

  private solicitud(): SolicitudDescuentos {
    return { ...this.form.getRawValue(), descuentos: this.descuentos() };
  }
}
