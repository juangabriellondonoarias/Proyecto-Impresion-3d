import { Routes } from '@angular/router';

export const routes: Routes = [
  { path: '', title: 'Panel principal · Costos 3D', loadComponent: () => import('./pages/dashboard/dashboard').then((m) => m.Dashboard) },
  { path: 'calculadora', title: 'Calculadora · Costos 3D', loadComponent: () => import('./pages/calculator/calculator').then((m) => m.Calculator) },
  { path: 'productos', title: 'Productos · Costos 3D', loadComponent: () => import('./pages/products/products').then((m) => m.Products) },
  { path: 'filamentos', title: 'Filamentos · Costos 3D', loadComponent: () => import('./pages/filaments/filaments').then((m) => m.Filaments) },
  { path: 'cotizaciones', title: 'Cotizaciones · Costos 3D', loadComponent: () => import('./pages/quotes/quotes').then((m) => m.Quotes) },
  { path: 'cotizaciones/nueva', title: 'Nueva cotización · Costos 3D', loadComponent: () => import('./pages/quotes/quote-editor').then((m) => m.QuoteEditor) },
  { path: 'cotizaciones/:id', title: 'Cotización · Costos 3D', loadComponent: () => import('./pages/quotes/quote-editor').then((m) => m.QuoteEditor) },
  { path: 'cotizaciones/:id/imprimir', title: 'Cotización PDF · Costos 3D', loadComponent: () => import('./pages/quotes/quote-print').then((m) => m.QuotePrint) },
  { path: 'descuentos', title: 'Descuentos por mayor · Costos 3D', loadComponent: () => import('./pages/discounts/discounts').then((m) => m.Discounts) },
  { path: 'rentabilidad', title: 'Análisis de rentabilidad · Costos 3D', loadComponent: () => import('./pages/analysis/analysis').then((m) => m.Analysis) },
  { path: 'configuracion', title: 'Configuración · Costos 3D', loadComponent: () => import('./pages/settings/settings').then((m) => m.SettingsPage) },
  { path: '**', redirectTo: '' },
];
