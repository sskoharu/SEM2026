import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { solicitarJson } from '../../src/shared/http/httpClient'
import type { OpcionesHttp } from '../../src/shared/http/httpClient'
import { limpiarMetricas, obtenerMetricas } from '../../src/shared/metrics/metricasService'

type RespuestaPrueba = { texto: string; uso?: { entrada: number; salida: number; total: number } }
const opciones: OpcionesHttp<RespuestaPrueba> = {
  operacion: 'verificacion-http',
  validar: (datos): datos is RespuestaPrueba => typeof datos === 'object' && datos !== null && 'texto' in datos && typeof datos.texto === 'string',
  reintentos: { maxIntentos: 3, esperaBaseMs: 10, esperaMaximaMs: 100 },
}
const correcta = () => new Response(JSON.stringify({ texto: 'Resultado de prueba' }), { status: 200 })
const url = 'https://servidor.example.test/prueba'

beforeEach(() => limpiarMetricas())
afterEach(() => { vi.useRealTimers(); vi.unstubAllGlobals() })

// Todas las respuestas HTTP de este archivo utilizan mocks; no se consulta un backend.
describe('Cliente HTTP con fetch simulado', () => {
  it('conserva el error HTTP aunque falle el descarte del cuerpo', async () => {
    vi.stubGlobal('fetch', vi.fn().mockResolvedValue({
      ok: false, status: 403, headers: new Headers(),
      body: { cancel: () => Promise.reject(new Error('Cuerpo interrumpido')) },
    }))
    await expect(solicitarJson(url, opciones)).rejects.toMatchObject({ tipo: 'http', codigoHttp: 403, intentos: 1 })
  })
  it('aísla solicitudes simultáneas y sus métricas', async () => {
    vi.stubGlobal('fetch', vi.fn().mockImplementation(async () => correcta()))
    await Promise.all([solicitarJson(url, opciones), solicitarJson(url, opciones)])
    const registros = obtenerMetricas()
    expect(registros).toHaveLength(2)
    expect(new Set(registros.map(r => r.idSolicitud)).size).toBe(2)
    expect(registros.every(r => r.estado === 'exito' && r.intentos === 1)).toBe(true)
  })
  it('devuelve JSON validado y registra ausencia de tokens', async () => {
    vi.stubGlobal('fetch', vi.fn().mockResolvedValue(correcta()))
    expect(await solicitarJson(url, opciones)).toEqual({ texto: 'Resultado de prueba' })
    expect(obtenerMetricas()[0]).toMatchObject({ codigoHttp: 200, estado: 'exito', intentos: 1, tokensEntrada: null, tokensSalida: null, tokensTotal: null })
  })
  it('429 agotado limita intentos y registra esperas exponenciales', async () => {
    vi.useFakeTimers()
    const tiempos: number[] = []
    vi.stubGlobal('fetch', vi.fn(() => {
      tiempos.push(Date.now())
      return Promise.resolve(new Response('', { status: 429 }))
    }))
    const solicitud = solicitarJson(url, opciones)
    const rechazo = expect(solicitud).rejects.toMatchObject({ tipo: 'limite', codigoHttp: 429, intentos: 3 })
    await vi.advanceTimersByTimeAsync(40)
    await rechazo
    expect(tiempos.map(t => t - tiempos[0])).toEqual([0, 10, 30])
    expect(obtenerMetricas()[0]).toMatchObject({ estado: 'error', codigoHttp: 429, intentos: 3 })
  })
  it('respeta Retry-After antes de recuperar una solicitud', async () => {
    vi.useFakeTimers()
    const fetchMock = vi.fn().mockResolvedValueOnce(new Response('', { status: 429, headers: { 'Retry-After': '1' } })).mockResolvedValueOnce(correcta())
    vi.stubGlobal('fetch', fetchMock)
    const solicitud = solicitarJson(url, opciones)
    await vi.advanceTimersByTimeAsync(999)
    expect(fetchMock).toHaveBeenCalledTimes(1)
    await vi.advanceTimersByTimeAsync(1)
    await solicitud
    expect(fetchMock).toHaveBeenCalledTimes(2)
    expect(obtenerMetricas()[0].intentos).toBe(2)
  })
  it('POST no se reintenta sin garantía de idempotencia', async () => {
    const fetchMock = vi.fn().mockResolvedValue(new Response('', { status: 429 }))
    vi.stubGlobal('fetch', fetchMock)
    await expect(solicitarJson(url, { ...opciones, metodo: 'POST' })).rejects.toMatchObject({ intentos: 1 })
    expect(fetchMock).toHaveBeenCalledTimes(1)
  })
  it('POST con garantía explícita conserva la clave entre intentos', async () => {
    vi.useFakeTimers()
    const fetchMock = vi.fn().mockResolvedValueOnce(new Response('', { status: 503 })).mockResolvedValueOnce(correcta())
    vi.stubGlobal('fetch', fetchMock)
    const solicitud = solicitarJson(url, { ...opciones, metodo: 'POST', cuerpo: '{}', idempotencia: { clave: 'prueba-idempotente', garantizadaPorServidor: true } })
    await vi.advanceTimersByTimeAsync(10)
    await solicitud
    for (const llamada of fetchMock.mock.calls) {
      expect((llamada[1].headers as Headers).get('Idempotency-Key')).toBe('prueba-idempotente')
    }
  })
  it.each([500, 502, 503, 504])('controla servidor HTTP %s', async codigo => {
    vi.stubGlobal('fetch', vi.fn().mockResolvedValue(new Response('', { status: codigo })))
    await expect(solicitarJson(url, { ...opciones, reintentos: { maxIntentos: 1 } })).rejects.toMatchObject({ tipo: 'servidor', codigoHttp: codigo })
    expect(obtenerMetricas()[0].estado).toBe('error')
  })
  it.each([400, 401, 403, 404])('controla error %s sin reintentar', async codigo => {
    const fetchMock = vi.fn().mockResolvedValue(new Response('', { status: codigo }))
    vi.stubGlobal('fetch', fetchMock)
    await expect(solicitarJson(url, opciones)).rejects.toMatchObject({ tipo: 'http', codigoHttp: codigo, intentos: 1 })
    expect(fetchMock).toHaveBeenCalledTimes(1)
  })
  it.each([['', 'vacia'], ['  ', 'vacia'], ['no es JSON', 'invalida'], ['null', 'invalida'], ['{}', 'invalida']])('controla cuerpo %s como %s', async (cuerpo, tipo) => {
    vi.stubGlobal('fetch', vi.fn().mockResolvedValue(new Response(cuerpo)))
    await expect(solicitarJson(url, opciones)).rejects.toMatchObject({ tipo, codigoHttp: 200 })
    expect(obtenerMetricas()[0]).toMatchObject({ codigoHttp: 200, intentos: 1, estado: 'error' })
  })
  it('controla error de conexión y no almacena el mensaje externo', async () => {
    vi.stubGlobal('fetch', vi.fn().mockRejectedValue(new TypeError('correo-personal@example.test')))
    await expect(solicitarJson(url, { ...opciones, reintentos: { maxIntentos: 1 } })).rejects.toMatchObject({ tipo: 'conexion' })
    expect(JSON.stringify(obtenerMetricas())).not.toContain('correo-personal')
  })
  it('timeout aborta la solicitud y permite recuperar con otra', async () => {
    vi.useFakeTimers()
    let senal: AbortSignal | undefined
    vi.stubGlobal('fetch', vi.fn((_url, config) => new Promise((_resolver, rechazar) => {
      senal = config.signal
      senal?.addEventListener('abort', () => rechazar(new DOMException('Abortado', 'AbortError')))
    })))
    const solicitud = solicitarJson(url, { ...opciones, timeoutMs: 20, reintentos: { maxIntentos: 1 } })
    const rechazo = expect(solicitud).rejects.toMatchObject({ tipo: 'timeout' })
    await vi.advanceTimersByTimeAsync(20)
    await rechazo
    expect(senal?.aborted).toBe(true)
    vi.stubGlobal('fetch', vi.fn().mockResolvedValue(correcta()))
    await expect(solicitarJson(url, opciones)).resolves.toHaveProperty('texto')
  })
  it('el timeout también cubre la lectura del cuerpo', async () => {
    vi.useFakeTimers()
    vi.stubGlobal('fetch', vi.fn((_url, config) => Promise.resolve({
      ok: true, status: 200,
      text: () => new Promise((_resolver, rechazar) => config.signal.addEventListener('abort', () => rechazar(new DOMException('Abortado', 'AbortError')))),
    })))
    const solicitud = solicitarJson(url, { ...opciones, timeoutMs: 20, reintentos: { maxIntentos: 1 } })
    const rechazo = expect(solicitud).rejects.toMatchObject({ tipo: 'timeout' })
    await vi.advanceTimersByTimeAsync(20)
    await rechazo
  })
  it('cancela durante fetch sin reintentar', async () => {
    const controlador = new AbortController()
    const fetchMock = vi.fn((_url, config) => new Promise((_resolver, rechazar) => {
      config.signal.addEventListener('abort', () => rechazar(new DOMException('Abortado', 'AbortError')))
    }))
    vi.stubGlobal('fetch', fetchMock)
    const solicitud = solicitarJson(url, { ...opciones, senal: controlador.signal })
    const rechazo = expect(solicitud).rejects.toMatchObject({ tipo: 'cancelacion', intentos: 1 })
    controlador.abort()
    await rechazo
    expect(fetchMock).toHaveBeenCalledTimes(1)
    expect(obtenerMetricas()[0].estado).toBe('cancelada')
  })
  it('cancela durante la espera de reintento', async () => {
    vi.useFakeTimers()
    const controlador = new AbortController()
    vi.stubGlobal('fetch', vi.fn().mockResolvedValue(new Response('', { status: 429 })))
    const solicitud = solicitarJson(url, { ...opciones, senal: controlador.signal })
    const rechazo = expect(solicitud).rejects.toMatchObject({ tipo: 'cancelacion', intentos: 1 })
    await vi.advanceTimersByTimeAsync(1)
    controlador.abort()
    await rechazo
    expect(obtenerMetricas()[0]).toMatchObject({ estado: 'cancelada', intentos: 1 })
  })
  it('una señal ya cancelada no inicia fetch', async () => {
    const controlador = new AbortController()
    controlador.abort()
    const fetchMock = vi.fn()
    vi.stubGlobal('fetch', fetchMock)
    await expect(solicitarJson(url, { ...opciones, senal: controlador.signal })).rejects.toMatchObject({ tipo: 'cancelacion', intentos: 0 })
    expect(fetchMock).not.toHaveBeenCalled()
  })
  it('lee tokens de un fixture de prueba sin imponer contrato al backend', async () => {
    vi.stubGlobal('fetch', vi.fn().mockResolvedValue(new Response(JSON.stringify({ texto: 'Prueba', uso: { entrada: 7, salida: 3, total: 10 } }))))
    await solicitarJson(url, { ...opciones, leerTokens: datos => ({ entrada: datos.uso?.entrada ?? null, salida: datos.uso?.salida ?? null, total: datos.uso?.total ?? null }) })
    expect(obtenerMetricas()[0]).toMatchObject({ tokensEntrada: 7, tokensSalida: 3, tokensTotal: 10 })
  })
  it('valida configuración antes de enviar', async () => {
    const fetchMock = vi.fn()
    vi.stubGlobal('fetch', fetchMock)
    await expect(solicitarJson(url, { ...opciones, timeoutMs: -1 })).rejects.toMatchObject({ tipo: 'invalida', intentos: 0 })
    expect(fetchMock).not.toHaveBeenCalled()
  })
  it('una excepción del validador se controla como formato inválido sin reintento', async () => {
    const fetchMock = vi.fn().mockResolvedValue(correcta())
    vi.stubGlobal('fetch', fetchMock)
    await expect(solicitarJson(url, {
      ...opciones, validar: (_datos): _datos is RespuestaPrueba => {
        void _datos
        throw new Error('Fallo del validador de prueba')
      }
    })).rejects.toMatchObject({ tipo: 'invalida', codigoHttp: 200, intentos: 1 })
    expect(fetchMock).toHaveBeenCalledTimes(1)
  })
  it('un extractor de tokens ausente o fallido no invalida el resultado', async () => {
    vi.stubGlobal('fetch', vi.fn().mockResolvedValue(correcta()))
    await expect(solicitarJson(url, { ...opciones, leerTokens: () => { throw new Error('Uso ausente') } })).resolves.toHaveProperty('texto')
    expect(obtenerMetricas()[0]).toMatchObject({ estado: 'exito', tokensTotal: null })
  })

})
