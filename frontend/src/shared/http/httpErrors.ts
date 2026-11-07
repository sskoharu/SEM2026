export type TipoError =
  | 'limite' | 'timeout' | 'vacia' | 'invalida' | 'conexion'
  | 'servidor' | 'http' | 'cancelacion'

export type ErrorSolicitud = {
  tipo: TipoError
  mensaje: string
  codigoHttp: number | null
  esperaServidorMs: number | null
  intentos: number
}

const mensajes: Record<TipoError, string> = {
  limite: 'Se alcanzó el límite de solicitudes. Intente nuevamente más tarde.',
  timeout: 'La solicitud superó el tiempo de espera. Sus datos se conservan; puede volver a intentarlo.',
  vacia: 'El servidor devolvió una respuesta vacía. Puede volver a intentarlo.',
  invalida: 'La respuesta del servidor no tiene el formato esperado. Intente nuevamente.',
  conexion: 'No se pudo conectar con el servidor. Revise su conexión e intente nuevamente.',
  servidor: 'El servidor no está disponible en este momento. Intente nuevamente más tarde.',
  http: 'No se pudo completar la solicitud. Revise la información e intente nuevamente.',
  cancelacion: 'La solicitud fue cancelada.',
}

export function crearError(
  tipo: TipoError,
  codigoHttp: number | null = null,
  esperaServidorMs: number | null = null,
  mensajePersonalizado?: string,
): ErrorSolicitud {
  return { tipo, mensaje: mensajePersonalizado ?? mensajes[tipo], codigoHttp, esperaServidorMs, intentos: 0 }
}

export function esErrorSolicitud(error: unknown): error is ErrorSolicitud {
  if (typeof error !== 'object' || error === null) return false
  return 'tipo' in error && typeof error.tipo === 'string' &&
    Object.hasOwn(mensajes, error.tipo) && 'mensaje' in error &&
    typeof error.mensaje === 'string' && 'intentos' in error
}

export function normalizarError(error: unknown): ErrorSolicitud {
  return esErrorSolicitud(error) ? error : crearError('conexion')
}
