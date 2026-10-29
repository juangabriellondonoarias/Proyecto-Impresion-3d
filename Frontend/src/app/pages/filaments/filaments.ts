import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { ApiServicio, leerErrorApi } from '../../core/api.service';
import { CopPipe, NumPipe, PctPipe } from '../../core/format';
import { Filamento } from '../../core/models';

@Component({
  selector: 'app-filaments',
  imports: [ReactiveFormsModule, CopPipe, NumPipe, PctPipe],
  templateUrl: './filaments.html',
  styleUrl: './filaments.css',
})
export class Filaments implements OnInit {
  private api = inject(ApiServicio);
  private fb = inject(FormBuilder).nonNullable;

  readonly materials = ['PLA', 'PETG', 'ABS', 'ASA', 'TPU', 'PLA Silk', 'PLA Mate'];

  readonly filaments = signal<Filamento[]>([]);
  readonly editingId = signal<number | null | undefined>(undefined); // undefined = formulario cerrado
  readonly error = signal<string | null>(null);
  readonly formError = signal<string | null>(null);
  readonly serverErrors = signal<Record<string, string>>({});
  readonly saving = signal(false);

  readonly form = this.fb.group({
    material: ['PLA', Validators.required],
    color: ['', Validators.required],
    marca: [''],
    precioCompra: this.fb.control<number | null>(null, [Validators.required, Validators.min(0)]),
    pesoInicialG: this.fb.control<number | null>(1000, [Validators.required, Validators.min(1)]),
    pesoDisponibleG: this.fb.control<number | null>(1000, [Validators.required, Validators.min(0)]),
    notas: [''],
  });

  private formValue = toSignal(this.form.valueChanges, { initialValue: this.form.getRawValue() });

  readonly previewCostPerKg = computed(() => {
    const v = this.formValue();
    return v.pesoInicialG && v.pesoInicialG > 0 && v.precioCompra != null ? (v.precioCompra / v.pesoInicialG) * 1000 : null;
  });

  readonly inventoryValue = computed(() =>
    this.filaments().reduce((sum, f) => sum + (f.pesoDisponibleG / 1000) * (f.costoPorKg ?? 0), 0),
  );

  readonly totalKg = computed(() => this.filaments().reduce((sum, f) => sum + f.pesoDisponibleG, 0) / 1000);

  ngOnInit(): void {
    this.load();
  }

  load(): void {
    this.api.listarFilamentos().subscribe({
      next: (list) => {
        this.filaments.set(list);
        this.error.set(null);
      },
      error: (err) => this.error.set(leerErrorApi(err).mensaje),
    });
  }

  stockPct(f: Filamento): number {
    return f.pesoInicialG > 0 ? Math.min(100, (f.pesoDisponibleG / f.pesoInicialG) * 100) : 0;
  }

  openNew(): void {
    this.form.reset();
    this.clearErrors();
    this.editingId.set(null);
  }

  edit(f: Filamento): void {
    this.form.reset({
      material: f.material,
      color: f.color,
      marca: f.marca ?? '',
      precioCompra: f.precioCompra,
      pesoInicialG: f.pesoInicialG,
      pesoDisponibleG: f.pesoDisponibleG,
      notas: f.notas ?? '',
    });
    this.clearErrors();
    this.editingId.set(f.id ?? null);
  }

  cancel(): void {
    this.editingId.set(undefined);
  }

  save(): void {
    this.form.markAllAsTouched();
    const v = this.form.getRawValue();
    if (this.form.invalid) {
      this.formError.set('Revisa los campos marcados en rojo.');
      return;
    }
    if ((v.pesoDisponibleG ?? 0) > (v.pesoInicialG ?? 0)) {
      this.formError.set('El peso disponible no puede ser mayor que el peso inicial.');
      return;
    }
    this.saving.set(true);
    const body: Filamento = {
      id: this.editingId() ?? undefined,
      material: v.material.trim(),
      color: v.color.trim(),
      marca: v.marca.trim() || null,
      precioCompra: v.precioCompra ?? 0,
      pesoInicialG: v.pesoInicialG ?? 0,
      pesoDisponibleG: v.pesoDisponibleG ?? 0,
      notas: v.notas.trim() || null,
    };
    this.api.guardarFilamento(body).subscribe({
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

  remove(f: Filamento): void {
    if (!f.id || !confirm(`¿Eliminar el filamento ${f.etiqueta}? Esta acción no se puede deshacer.`)) return;
    this.api.eliminarFilamento(f.id).subscribe({
      next: () => {
        if (this.editingId() === f.id) this.editingId.set(undefined);
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
