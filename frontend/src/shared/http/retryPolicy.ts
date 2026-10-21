import type { ErrorSolicitud } from './httpErrors'

export type PoliticaReintentos = {
  maxIntentos: number
  esperaBaseMs: number
  esperaMaximaMs: number
}

export const politicaPredeterminada: PoliticaReintentos = {
  maxIntentos: 3,
  esperaBaseMs: 500,
  esperaMaximaMs: 10000,
}

export function leerRetryAfter(valor: string | null, ahora = Date.now()): number | null {
  if (!valor?.trim()) return null
  const texto = valor.trim()
  if (/^\d+$/.test(texto)) {
    const milisegundos = Number(texto) * 1000
    return Number.isSafeInteger(milisegundos) ? milisegundos : null
  }
  // HTTP-date utiliza fecha con día y zona GMT; evita interpretar números negativos como fechas.
  if (!/^[A-Za-z]{3}, \d{2} [A-Za-z]{3} \d{4} \d{2}:\d{2}:\d{2} GMT$/.test(texto)) return null
  const fecha = Date.parse(texto)
  return Number.isFinite(fecha) ? Math.max(0, fecha - ahora) : null
}

export function calcularEspera(intento: number, politica: PoliticaReintentos, servidorMs: number | null): number {
  const exponencial = Math.min(politica.esperaMaximaMs, politica.esperaBaseMs * 2 ** (intento - 1))
  return Math.max(exponencial, servidorMs ?? 0)
}

export function puedeReintentar(error: ErrorSolicitud, intento: number, maxIntentos: number, segura: boolean): boolean {
  if (!segura || intento >= maxIntentos) return false
  return ['limite', 'timeout', 'conexion'].includes(error.tipo) ||
    (error.tipo === 'servidor' && [500, 502, 503, 504].includes(error.codigoHttp ?? 0))
}
