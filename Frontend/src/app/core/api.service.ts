import { HttpClient, HttpErrorResponse } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import {
  Configuracion,
  Cotizacion,
  ErrorApi,
  EstadoCotizacion,
  Filamento,
  PrecioItem,
  Producto,
  ResultadoAnalisis,
  ResultadoDescuentos,
  ResultadoPrecio,
  ResumenCotizacion,
  SolicitudCotizacion,
  SolicitudDescuentos,
  SolicitudPrecio,
  SolicitudProducto,
} from './models';

/** Llamadas a la API del backend (/api/...). */
@Injectable({ providedIn: 'root' })
export class ApiServicio {
  private http = inject(HttpClient);

  obtenerConfiguracion(): Observable<Configuracion> {
    return this.http.get<Configuracion>('/api/configuracion');
  }

  guardarConfiguracion(configuracion: Configuracion): Observable<Configuracion> {
    return this.http.put<Configuracion>('/api/configuracion', configuracion);
  }

  listarFilamentos(): Observable<Filamento[]> {
    return this.http.get<Filamento[]>('/api/filamentos');
  }

  guardarFilamento(filamento: Filamento): Observable<Filamento> {
    return filamento.id
      ? this.http.put<Filamento>(`/api/filamentos/${filamento.id}`, filamento)
      : this.http.post<Filamento>('/api/filamentos', filamento);
  }

  eliminarFilamento(id: number): Observable<void> {
    return this.http.delete<void>(`/api/filamentos/${id}`);
  }

  listarProductos(): Observable<Producto[]> {
    return this.http.get<Producto[]>('/api/productos');
  }

  guardarProducto(id: number | undefined, producto: SolicitudProducto): Observable<Producto> {
    return id
      ? this.http.put<Producto>(`/api/productos/${id}`, producto)
      : this.http.post<Producto>('/api/productos', producto);
  }

  eliminarProducto(id: number): Observable<void> {
    return this.http.delete<void>(`/api/productos/${id}`);
  }

  calcularPrecio(solicitud: SolicitudPrecio): Observable<ResultadoPrecio> {
    return this.http.post<ResultadoPrecio>('/api/precios/calcular', solicitud);
  }

  listarCotizaciones(): Observable<Cotizacion[]> {
    return this.http.get<Cotizacion[]>('/api/cotizaciones');
  }

  obtenerCotizacion(id: number): Observable<Cotizacion> {
    return this.http.get<Cotizacion>(`/api/cotizaciones/${id}`);
  }

  guardarCotizacion(id: number | undefined, cotizacion: SolicitudCotizacion): Observable<Cotizacion> {
    return id
      ? this.http.put<Cotizacion>(`/api/cotizaciones/${id}`, cotizacion)
      : this.http.post<Cotizacion>('/api/cotizaciones', cotizacion);
  }

  eliminarCotizacion(id: number): Observable<void> {
    return this.http.delete<void>(`/api/cotizaciones/${id}`);
  }

  cambiarEstadoCotizacion(id: number, estado: EstadoCotizacion, fechaVenta?: string): Observable<Cotizacion> {
    return this.http.put<Cotizacion>(`/api/cotizaciones/${id}/estado`, { estado, fechaVenta: fechaVenta ?? null });
  }

  vistaPreviaCotizacion(cotizacion: SolicitudCotizacion): Observable<ResumenCotizacion> {
    return this.http.post<ResumenCotizacion>('/api/cotizaciones/vista-previa', cotizacion);
  }

  precioItem(productoId: number, cantidad: number, margenPct: number): Observable<PrecioItem> {
    return this.http.post<PrecioItem>('/api/cotizaciones/precio-item', { productoId, cantidad, margenPct });
  }

  compararDescuentos(solicitud: SolicitudDescuentos): Observable<ResultadoDescuentos> {
    return this.http.post<ResultadoDescuentos>('/api/descuentos/comparar', solicitud);
  }

  analizar(meses: number): Observable<ResultadoAnalisis> {
    return this.http.get<ResultadoAnalisis>('/api/analisis', { params: { meses } });
  }
}

/** Extrae el mensaje y los errores por campo que devuelve el backend. */
export function leerErrorApi(err: unknown): ErrorApi {
  if (err instanceof HttpErrorResponse) {
    if (err.status === 0) {
      return { mensaje: 'No hay conexión con el servidor. ¿Está encendido el backend?', errores: {} };
    }
    const cuerpo = err.error as Partial<ErrorApi> | null;
    return {
      mensaje: cuerpo?.mensaje ?? `Error ${err.status} del servidor.`,
      errores: cuerpo?.errores ?? {},
    };
  }
  return { mensaje: 'Ocurrió un error inesperado.', errores: {} };
}
