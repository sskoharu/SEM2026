import { crearError, esErrorSolicitud, normalizarError } from './httpErrors'
import { calcularEspera, leerRetryAfter, politicaPredeterminada, puedeReintentar } from './retryPolicy'
import type { PoliticaReintentos } from './retryPolicy'
import { esperar } from './esperar'
import { finalizarMedicion, iniciarMedicion } from '../metrics/metricasService'
import type { Operacion, Tokens } from '../metrics/metricas.types'

export type OpcionesHttp<T> = {
  operacion: Operacion
  metodo?: 'GET' | 'HEAD' | 'OPTIONS' | 'POST' | 'PUT' | 'PATCH' | 'DELETE'
  cabeceras?: HeadersInit
  cuerpo?: BodyInit
  senal?: AbortSignal
  timeoutMs?: number
  reintentos?: Partial<PoliticaReintentos>
  idempotencia?: { clave: string; garantizadaPorServidor: true }
  validar: (datos: unknown) => datos is T
  leerTokens?: (datos: T) => Tokens
  alRecibir?: (datos: T, cabeceras: Headers) => void
}

function validarOpciones<T>(opciones: OpcionesHttp<T>, politica: PoliticaReintentos, timeout: number) {
  if (!Number.isSafeInteger(politica.maxIntentos) || politica.maxIntentos < 1 || politica.maxIntentos > 10 ||
    !Number.isFinite(politica.esperaBaseMs) || politica.esperaBaseMs < 0 ||
    !Number.isFinite(politica.esperaMaximaMs) || politica.esperaMaximaMs < politica.esperaBaseMs ||
    !Number.isFinite(timeout) || timeout <= 0 || timeout > 2147483647 ||
    (opciones.idempotencia && !opciones.idempotencia.clave.trim())) {
    throw crearError('invalida')
  }
}

async function ejecutarIntento<T>(url: string, opciones: OpcionesHttp<T>, timeout: number): Promise<{ datos: T; codigo: number }> {
  const controlador = new AbortController()
  let agotado = false
  let codigoHttp: number | null = null
  const cancelar = () => controlador.abort()
  opciones.senal?.addEventListener('abort', cancelar, { once: true })
  if (opciones.senal?.aborted) cancelar()
  const temporizador = setTimeout(() => {
    agotado = true
    controlador.abort()
  }, timeout)
  try {
    if (controlador.signal.aborted) throw crearError('cancelacion')
    const cabeceras = new Headers(opciones.cabeceras)
    if (opciones.idempotencia) cabeceras.set('Idempotency-Key', opciones.idempotencia.clave)
    const respuesta = await fetch(url, {
      method: opciones.metodo ?? 'GET',
      headers: cabeceras,
      body: opciones.cuerpo,
      signal: controlador.signal,
    })
    codigoHttp = respuesta.status
    if (!respuesta.ok) {
      const tipo = respuesta.status === 429 ? 'limite' : respuesta.status >= 500 ? 'servidor' : 'http'
      const espera = leerRetryAfter(respuesta.headers.get('Retry-After'))
      // No se conservan cuerpos de error: podrían incluir datos personales.
      try {
        await respuesta.body?.cancel()
      } catch {
        // Descartar el cuerpo no debe ocultar el estado recibido del servidor.
      }
      throw crearError(tipo, respuesta.status, espera)
    }
    const texto = await respuesta.text()
    if (controlador.signal.aborted) throw crearError('cancelacion', respuesta.status)
    if (!texto.trim()) throw crearError('vacia', respuesta.status)
    let datos: unknown
    try {
      datos = JSON.parse(texto)
    } catch {
      throw crearError('invalida', respuesta.status)
    }
    try {
      if (!opciones.validar(datos)) throw crearError('invalida', respuesta.status)
    } catch {
      throw crearError('invalida', respuesta.status)
    }
    opciones.alRecibir?.(datos, respuesta.headers)
    return { datos, codigo: respuesta.status }
  } catch (error) {
    if (opciones.senal?.aborted) throw crearError('cancelacion', codigoHttp)
    if (agotado) throw crearError('timeout', codigoHttp)
    const controlado = normalizarError(error)
    throw { ...controlado, codigoHttp: controlado.codigoHttp ?? codigoHttp }
  } finally {
    clearTimeout(temporizador)
    opciones.senal?.removeEventListener('abort', cancelar)
  }
}

export async function solicitarJson<T>(url: string, opciones: OpcionesHttp<T>): Promise<T> {
  const medicion = iniciarMedicion(opciones.operacion)
  const politica = { ...politicaPredeterminada, ...opciones.reintentos }
  const timeout = opciones.timeoutMs ?? 60000
  const metodo = opciones.metodo ?? 'GET'
  const segura = ['GET', 'HEAD', 'OPTIONS'].includes(metodo) ||
    opciones.idempotencia?.garantizadaPorServidor === true
  let intentos = 0
  let codigoHttp: number | null = null
  try {
    validarOpciones(opciones, politica, timeout)
    while (intentos < politica.maxIntentos) {
      if (opciones.senal?.aborted) throw crearError('cancelacion')
      intentos += 1
      try {
        const resultado = await ejecutarIntento(url, opciones, timeout)
        codigoHttp = resultado.codigo
        let tokens: Tokens | undefined
        try {
          tokens = opciones.leerTokens?.(resultado.datos)
        } catch {
          // Una métrica opcional ausente no invalida el resultado de la operación.
          tokens = undefined
        }
        finalizarMedicion(medicion, { estado: 'exito', codigoHttp, intentos }, tokens)
        return resultado.datos
      } catch (error) {
        const controlado = normalizarError(error)
        codigoHttp = controlado.codigoHttp
        if (!puedeReintentar(controlado, intentos, politica.maxIntentos, segura)) throw controlado
        await esperar(calcularEspera(intentos, politica, controlado.esperaServidorMs), opciones.senal)
      }
    }
    throw crearError('conexion')
  } catch (error) {
    const controlado = esErrorSolicitud(error) ? error : normalizarError(error)
    const estado = controlado.tipo === 'cancelacion' ? 'cancelada' : 'error'
    finalizarMedicion(medicion, { estado, codigoHttp, intentos })
    throw { ...controlado, codigoHttp: controlado.codigoHttp ?? codigoHttp, intentos }
  }
}
