import { describe, expect, it } from 'vitest'
import { calcularEspera, leerRetryAfter, politicaPredeterminada } from '../../src/shared/http/retryPolicy'

describe('Política de reintentos', () => {
  it('acepta Retry-After cero sin eliminar el backoff local', () => {
    expect(leerRetryAfter('0')).toBe(0)
    expect(calcularEspera(1, politicaPredeterminada, 0)).toBe(500)
  })
  it('calcula espera exponencial con límite local', () => {
    expect([1, 2, 3, 8].map(i => calcularEspera(i, politicaPredeterminada, null))).toEqual([500, 1000, 2000, 10000])
  })
  it('respeta Retry-After incluso si supera el límite local', () => {
    expect(calcularEspera(1, politicaPredeterminada, 20000)).toBe(20000)
  })
  it('acepta segundos y fecha HTTP', () => {
    const ahora = Date.parse('2026-10-20T12:00:00Z')
    expect(leerRetryAfter('2', ahora)).toBe(2000)
    expect(leerRetryAfter('Tue, 20 Oct 2026 12:00:03 GMT', ahora)).toBe(3000)
    expect(leerRetryAfter('Tue, 20 Oct 2026 11:00:00 GMT', ahora)).toBe(0)
  })
  it.each([null, '', '-1', 'abc', '1.5', 'Infinity', '9999999999999999999999'])('ignora Retry-After inválido %s', valor => {
    expect(leerRetryAfter(valor)).toBeNull()
  })
})
