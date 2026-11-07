export type Operacion = 'analisis-inicial' | 'analisis-complementario' | 'verificacion-http' |
  'guardar-reclamo' | 'listar-reclamos-guardados' | 'obtener-reclamo-guardado' | 'reanudar-reclamo'
export type Tokens = {
  entrada: number | null
  salida: number | null
  total: number | null
}
export type MetricaSolicitud = {
  idSolicitud: string
  fechaHora: string
  operacion: Operacion
  duracionMs: number
  estado: 'exito' | 'error' | 'cancelada'
  codigoHttp: number | null
  intentos: number
  tokensEntrada: number | null
  tokensSalida: number | null
  tokensTotal: number | null
}
