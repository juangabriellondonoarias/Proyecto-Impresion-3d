import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { ApiServicio, leerErrorApi } from '../../core/api.service';
import { CopPipe } from '../../core/format';
import { Configuracion } from '../../core/models';

@Component({
  selector: 'app-settings',
  imports: [ReactiveFormsModule, CopPipe],
  templateUrl: './settings.html',
})
export class SettingsPage implements OnInit {
  private api = inject(ApiServicio);
  private fb = inject(FormBuilder).nonNullable;

  readonly mensaje = signal<{ ok: boolean; text: string } | null>(null);
  readonly serverErrors = signal<Record<string, string>>({});
  readonly saving = signal(false);

  private num = (validators = [Validators.required, Validators.min(0)]) => this.fb.control<number | null>(null, validators);

  readonly form = this.fb.group({
    nombreNegocio: [''],
    contactoNegocio: [''],
    diasVigenciaCotizacion: this.num([Validators.min(0)]),
    condicionesPorDefecto: [''],
    precioKwh: this.num(),
    valorHoraManoObra: this.num(),
    potenciaImpresoraW: this.num(),
    precioImpresora: this.num(),
    vidaUtilImpresoraHoras: this.num(),
    mantenimientoPorHora: this.num(),
    margenPorDefectoPct: this.num([Validators.required, Validators.min(0), Validators.max(95)]),
    margenMinimoPct: this.num([Validators.required, Validators.min(0), Validators.max(95)]),
    pasoRedondeo: this.num(),
  });

  private value = toSignal(this.form.valueChanges, { initialValue: this.form.getRawValue() });

  readonly depreciationPerHour = computed(() => {
    const v = this.value();
    return v.vidaUtilImpresoraHoras && v.vidaUtilImpresoraHoras > 0 ? (v.precioImpresora ?? 0) / v.vidaUtilImpresoraHoras : 0;
  });

  readonly machineRate = computed(() => this.depreciationPerHour() + (this.value().mantenimientoPorHora ?? 0));

  ngOnInit(): void {
    this.api.obtenerConfiguracion().subscribe({
      next: (s) =>
        this.form.reset({
          ...s,
          nombreNegocio: s.nombreNegocio ?? '',
          contactoNegocio: s.contactoNegocio ?? '',
          condicionesPorDefecto: s.condicionesPorDefecto ?? '',
        }),
      error: (err) => this.mensaje.set({ ok: false, text: leerErrorApi(err).mensaje }),
    });
  }

  save(): void {
    this.form.markAllAsTouched();
    if (this.form.invalid) {
      this.mensaje.set({ ok: false, text: 'Revisa los campos marcados en rojo.' });
      return;
    }
    const v = this.form.getRawValue();
    if ((v.margenMinimoPct ?? 0) > (v.margenPorDefectoPct ?? 0)) {
      this.mensaje.set({ ok: false, text: 'El margen mínimo no puede ser mayor que el margen por defecto.' });
      return;
    }
    this.saving.set(true);
    this.api.guardarConfiguracion(v as Configuracion).subscribe({
      next: () => {
        this.saving.set(false);
        this.serverErrors.set({});
        this.mensaje.set({ ok: true, text: 'Configuración guardada.' });
      },
      error: (err) => {
        this.saving.set(false);
        const e = leerErrorApi(err);
        this.serverErrors.set(e.errores);
        this.mensaje.set({ ok: false, text: e.mensaje });
      },
    });
  }

  invalid(key: string): boolean {
    const c = this.form.get(key);
    return !!(c && c.invalid && c.touched) || !!this.serverErrors()[key];
  }
}
