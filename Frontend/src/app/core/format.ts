import { Pipe, PipeTransform } from '@angular/core';

const cop = new Intl.NumberFormat('es-CO', { style: 'currency', currency: 'COP', maximumFractionDigits: 0 });
const num = new Intl.NumberFormat('es-CO', { maximumFractionDigits: 1 });

/** 24600 -> "$ 24.600" */
@Pipe({ name: 'cop' })
export class CopPipe implements PipeTransform {
  transform(value: number | null | undefined): string {
    return value == null || !Number.isFinite(value) ? '—' : cop.format(value);
  }
}

/** 40.1626 -> "40,2 %" */
@Pipe({ name: 'pct' })
export class PctPipe implements PipeTransform {
  transform(value: number | null | undefined): string {
    return value == null || !Number.isFinite(value) ? '—' : `${num.format(value)} %`;
  }
}

/** Número con separadores colombianos y un decimal como máximo. */
@Pipe({ name: 'num' })
export class NumPipe implements PipeTransform {
  transform(value: number | null | undefined): string {
    return value == null || !Number.isFinite(value) ? '—' : num.format(value);
  }
}

/** 2.5 -> "2 h 30 min" */
@Pipe({ name: 'hours' })
export class HoursPipe implements PipeTransform {
  transform(value: number | null | undefined): string {
    if (value == null || !Number.isFinite(value)) return '—';
    const totalMin = Math.round(value * 60);
    const h = Math.floor(totalMin / 60);
    const m = totalMin % 60;
    return h === 0 ? `${m} min` : m === 0 ? `${h} h` : `${h} h ${m} min`;
  }
}

const longDate = new Intl.DateTimeFormat('es-CO', { day: 'numeric', month: 'long', year: 'numeric' });

/** Fecha ISO del backend a fecha local, sin desfase por zona horaria. */
export function parseIsoDate(iso: string): Date {
  const [y, m, d] = iso.split('-').map(Number);
  return new Date(y, m - 1, d);
}

/** Fecha de hoy como "2026-10-29". */
export function todayIso(): string {
  const d = new Date();
  return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}`;
}

/** "2026-10-29" -> "29 de octubre de 2026" */
@Pipe({ name: 'fecha' })
export class FechaPipe implements PipeTransform {
  transform(value: string | null | undefined): string {
    return value ? longDate.format(parseIsoDate(value)) : '—';
  }
}
