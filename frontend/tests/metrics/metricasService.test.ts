import { afterEach, describe, expect, it } from 'vitest'
import { finalizarMedicion, iniciarMedicion, limpiarMetricas, obtenerMetricas } from '../../src/shared/metrics/metricasService'

afterEach(limpiarMetricas)
describe('Registro estructurado de métricas', () => {
  it('mide duración real con reloj monotónico', async () => {
    const inicio = iniciarMedicion('analisis-inicial')
    await new Promise(resolver => setTimeout(resolver, 20))
    const metrica = finalizarMedicion(inicio, { estado: 'exito', codigoHttp: null, intentos: 1 })
    expect(metrica.duracionMs).toBeGreaterThanOrEqual(15)
    expect(Number.isNaN(Date.parse(metrica.fechaHora))).toBe(false)
    expect(metrica.idSolicitud).toMatch(/^[a-f0-9-]{36}$/)
  })
  it('no estima tokens ausentes ni suma valores parciales', () => {
    const metrica = finalizarMedicion(iniciarMedicion('verificacion-http'), { estado: 'error', codigoHttp: 500, intentos: 3 }, { entrada: 2, salida: null, total: null })
    expect(metrica).toMatchObject({ tokensEntrada: 2, tokensSalida: null, tokensTotal: null, estado: 'error', intentos: 3 })
  })
  it('descarta tokens negativos, fraccionarios o infinitos', () => {
    const metrica = finalizarMedicion(iniciarMedicion('verificacion-http'), { estado: 'exito', codigoHttp: 200, intentos: 1 }, { entrada: -1, salida: 1.5, total: Infinity })
    expect([metrica.tokensEntrada, metrica.tokensSalida, metrica.tokensTotal]).toEqual([null, null, null])
  })
  it('almacena solo campos técnicos, limita memoria y devuelve copias', () => {
    for (let i = 0; i < 105; i += 1) finalizarMedicion(iniciarMedicion('analisis-inicial'), { estado: 'exito', codigoHttp: null, intentos: 1 })
    const registros = obtenerMetricas()
    expect(registros).toHaveLength(100)
    expect(Object.keys(registros[0]).sort()).toEqual(['idSolicitud', 'fechaHora', 'operacion', 'duracionMs', 'estado', 'codigoHttp', 'intentos', 'tokensEntrada', 'tokensSalida', 'tokensTotal'].sort())
    registros[0].intentos = 99
    expect(obtenerMetricas()[0].intentos).toBe(1)
  })
})
