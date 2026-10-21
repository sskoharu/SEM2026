import type { MetricaSolicitud, Operacion, Tokens } from './metricas.types'

const registros: MetricaSolicitud[] = []

export function iniciarMedicion(operacion: Operacion) {
  return {
    idSolicitud: crypto.randomUUID(),
    fechaHora: new Date().toISOString(),
    operacion,
    inicio: performance.now(),
  }
}

function tokenValido(valor: unknown): number | null {
  return typeof valor === 'number' && Number.isSafeInteger(valor) && valor >= 0 ? valor : null
}

export function finalizarMedicion(
  medicion: ReturnType<typeof iniciarMedicion>,
  resultado: Pick<MetricaSolicitud, 'estado' | 'codigoHttp' | 'intentos'>,
  tokens?: Tokens,
): MetricaSolicitud {
  const registro: MetricaSolicitud = {
    idSolicitud: medicion.idSolicitud,
    fechaHora: medicion.fechaHora,
    operacion: medicion.operacion,
    duracionMs: Math.max(0, performance.now() - medicion.inicio),
    estado: resultado.estado,
    codigoHttp: resultado.codigoHttp,
    intentos: resultado.intentos,
    tokensEntrada: tokenValido(tokens?.entrada),
    tokensSalida: tokenValido(tokens?.salida),
    tokensTotal: tokenValido(tokens?.total),
  }
  registros.push(registro)
  if (registros.length > 100) registros.shift()
  return { ...registro }
}

export function obtenerMetricas(): MetricaSolicitud[] {
  return registros.map(registro => ({ ...registro }))
}

export function limpiarMetricas(): void {
  registros.length = 0
}
