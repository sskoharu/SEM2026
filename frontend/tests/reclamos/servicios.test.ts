import { afterEach, describe, expect, it, vi } from 'vitest'
import { analizarReclamo } from '../../src/modules/reclamos/services/reclamosService'
import { generarSimulacion, incorporarRespuestas } from '../../src/modules/reclamos/utils/simularAnalisis'
import { limpiarMetricas, obtenerMetricas } from '../../src/shared/metrics/metricasService'
import { reclamoValido } from './datos'

afterEach(() => { vi.useRealTimers(); limpiarMetricas() })

describe('Servicios y simulación local sin backend', () => {
  it('conserva hechos y preguntas según campos faltantes', () => {
    const analisis = generarSimulacion(reclamoValido)
    expect(analisis.preguntas.map(p => p.id)).toEqual(['fecha', 'valor', 'contacto', 'detalle'])
    expect(analisis.hechos).toContain('El consumidor identifica a Proveedor de prueba como proveedor.')
  })
  it('respeta datos opcionales y comprobante', () => {
    const analisis = generarSimulacion({ ...reclamoValido, fecha: '2026-10-01', valor: '10', comprobante: true, tipoComprobante: 'nota', numeroComprobante: '123', descripcion: 'Detalle '.repeat(20) })
    expect(analisis.preguntas.map(p => p.id)).toEqual(['contacto'])
    expect(analisis.hechos.at(-1)).toContain('nota de venta, número 123')
  })
  it('incorpora respuestas sin mutar el análisis y permite preguntas sin contestar', () => {
    const analisis = generarSimulacion(reclamoValido)
    const resultado = incorporarRespuestas(analisis, { contacto: '  Sin solución  ', fecha: ' ' })
    expect(resultado.hechos.at(-1)).toContain('Respuesta: Sin solución')
    expect(resultado.preguntas.map(p => p.id)).toContain('fecha')
    expect(analisis.preguntas).toHaveLength(4)
  })
  it('mide la simulación y deja tokens no disponibles', async () => {
    vi.useFakeTimers()
    const solicitud = analizarReclamo(reclamoValido)
    await vi.advanceTimersByTimeAsync(850)
    expect(await solicitud).toEqual(generarSimulacion(reclamoValido))
    expect(obtenerMetricas()[0]).toMatchObject({ estado: 'exito', codigoHttp: null, intentos: 1, tokensEntrada: null, tokensSalida: null, tokensTotal: null })
  })
  it('cancela la simulación y registra cancelación', async () => {
    const controlador = new AbortController()
    const solicitud = analizarReclamo(reclamoValido, controlador.signal)
    const rechazo = expect(solicitud).rejects.toMatchObject({ tipo: 'cancelacion' })
    controlador.abort()
    await rechazo
    expect(obtenerMetricas()[0].estado).toBe('cancelada')
  })
  it('modo API informa integración pendiente y no llama a fetch', async () => {
    const fetchMock = vi.spyOn(globalThis, 'fetch')
    await expect(analizarReclamo(reclamoValido, undefined, { modo: 'api' })).rejects.toMatchObject({ tipo: 'integracion' })
    expect(fetchMock).not.toHaveBeenCalled()
    fetchMock.mockRestore()
  })
  it('permite inyectar un adaptador sin imponer contrato HTTP', async () => {
    const analizar = vi.fn().mockResolvedValue(generarSimulacion(reclamoValido))
    await analizarReclamo(reclamoValido, undefined, { modo: 'api', analizar })
    expect(analizar).toHaveBeenCalledWith(reclamoValido, undefined)
  })
})
