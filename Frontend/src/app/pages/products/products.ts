import { Component, OnInit, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { forkJoin } from 'rxjs';
import { ApiServicio, leerErrorApi } from '../../core/api.service';
import { CopPipe, HoursPipe, NumPipe } from '../../core/format';
import { Filamento, Producto, SolicitudProducto } from '../../core/models';

@Component({
  selector: 'app-products',
  imports: [ReactiveFormsModule, RouterLink, CopPipe, NumPipe, HoursPipe],
  templateUrl: './products.html',
  styleUrl: './products.css',
})
export class Products implements OnInit {
  private api = inject(ApiServicio);
  private fb = inject(FormBuilder).nonNullable;

  readonly products = signal<Producto[]>([]);
  readonly filaments = signal<Filamento[]>([]);
  readonly editingId = signal<number | null | undefined>(undefined); // undefined = formulario cerrado
  readonly error = signal<string | null>(null);
  readonly formError = signal<string | null>(null);
  readonly serverErrors = signal<Record<string, string>>({});
  readonly saving = signal(false);

  readonly form = this.fb.group({
    nombre: ['', Validators.required],
    referencia: [''],
    filamentoId: this.fb.control<number | null>(null),
    pesoG: this.fb.control<number | null>(null, [Validators.required, Validators.min(0)]),
    desperdicioPorUnidadG: this.fb.control<number | null>(0, Validators.min(0)),
    tiempoImpresionMinPorUnidad: this.fb.control<number | null>(null, Validators.min(0)),
    posprocesoMinPorUnidad: this.fb.control<number | null>(0, Validators.min(0)),
    empaquePorUnidad: this.fb.control<number | null>(0, Validators.min(0)),
    notas: [''],
  });

  ngOnInit(): void {
    this.load();
  }

  load(): void {
    forkJoin({ products: this.api.listarProductos(), filaments: this.api.listarFilamentos() }).subscribe({
      next: ({ products, filaments }) => {
        this.products.set(products);
        this.filaments.set(filaments);
        this.error.set(null);
      },
      error: (err) => this.error.set(leerErrorApi(err).mensaje),
    });
  }

  /** Costo del material de una unidad (pieza + desperdicio), sin purgas de lote. */
  materialCost(p: Producto): number | null {
    if (!p.filamento?.costoPorKg) return null;
    return ((p.pesoG + (p.desperdicioPorUnidadG ?? 0)) / 1000) * p.filamento.costoPorKg;
  }

  openNew(): void {
    this.form.reset();
    this.clearErrors();
    this.editingId.set(null);
  }

  edit(p: Producto): void {
    this.form.reset({
      nombre: p.nombre,
      referencia: p.referencia ?? '',
      filamentoId: p.filamento?.id ?? null,
      pesoG: p.pesoG,
      desperdicioPorUnidadG: p.desperdicioPorUnidadG ?? 0,
      tiempoImpresionMinPorUnidad: p.tiempoImpresionMinPorUnidad,
      posprocesoMinPorUnidad: p.posprocesoMinPorUnidad ?? 0,
      empaquePorUnidad: p.empaquePorUnidad ?? 0,
      notas: p.notas ?? '',
    });
    this.clearErrors();
    this.editingId.set(p.id ?? null);
  }

  cancel(): void {
    this.editingId.set(undefined);
  }

  save(): void {
    this.form.markAllAsTouched();
    if (this.form.invalid) {
      this.formError.set('Revisa los campos marcados en rojo.');
      return;
    }
    const v = this.form.getRawValue();
    const body: SolicitudProducto = {
      nombre: v.nombre.trim(),
      referencia: v.referencia.trim() || null,
      filamentoId: v.filamentoId,
      pesoG: v.pesoG ?? 0,
      desperdicioPorUnidadG: v.desperdicioPorUnidadG,
      tiempoImpresionMinPorUnidad: v.tiempoImpresionMinPorUnidad,
      posprocesoMinPorUnidad: v.posprocesoMinPorUnidad,
      empaquePorUnidad: v.empaquePorUnidad,
      notas: v.notas.trim() || null,
    };
    this.saving.set(true);
    this.api.guardarProducto(this.editingId() ?? undefined, body).subscribe({
      next: () => {
        this.saving.set(false);
        this.editingId.set(undefined);
        this.load();
      },
      error: (err) => {
        this.saving.set(false);
        const e = leerErrorApi(err);
        this.formError.set(e.mensaje);
        this.serverErrors.set(e.errores);
      },
    });
  }

  remove(p: Producto): void {
    if (!p.id || !confirm(`¿Eliminar el producto «${p.nombre}»? Esta acción no se puede deshacer.`)) return;
    this.api.eliminarProducto(p.id).subscribe({
      next: () => {
        if (this.editingId() === p.id) this.editingId.set(undefined);
        this.load();
      },
      error: (err) => this.error.set(leerErrorApi(err).mensaje),
    });
  }

  invalid(key: string): boolean {
    const c = this.form.get(key);
    return !!(c && c.invalid && c.touched) || !!this.serverErrors()[key];
  }

  private clearErrors(): void {
    this.formError.set(null);
    this.serverErrors.set({});
  }
}
