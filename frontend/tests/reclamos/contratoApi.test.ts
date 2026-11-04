import { afterEach, describe, expect, it, vi } from 'vitest'
import { analizarConApi, esRespuestaSesion, seleccionarAccionConApi } from '../../src/modules/reclamos/services/reclamosApi'
import { forgetSessions } from '../../src/modules/reclamos/services/sessionAccess'
import { reclamoValido } from './datos'

afterEach(() => {
  vi.unstubAllGlobals()
  forgetSessions()
})

describe('Contrato y autorización React–Spring', () => {
  it('rechaza listas, fuentes y opciones de forma incorrecta', () => {
    expect(esRespuestaSesion({ sessionId: 'id', pendingParametersToAsk: 'not-a-list' })).toBe(false)
    expect(esRespuestaSesion({ sessionId: 'id', legalSources: [{}] })).toBe(false)
    expect(esRespuestaSesion({ sessionId: 'id', actionRouteOptions: { options: [{ optionId: 1 }] } })).toBe(false)
  })

  it('conserva el token fuera del JSON y extrae campos desde la opción seleccionada', async () => {
    const id = '11111111-1111-1111-1111-111111111111'
    const token = 'a'.repeat(43)
    const fetchMock = vi.fn()
      .mockResolvedValueOnce(new Response(JSON.stringify({ sessionId: id, layer1State: 'SOLICITANDO_ACLARACION', clarificationMessage: 'Datos faltantes' }), {
        headers: { 'X-Claim-Token': token },
      }))
      .mockResolvedValueOnce(new Response(JSON.stringify({
        sessionId: id, layer2State: 'DONE', layer3State: 'WAITING_FOR_PROCEDURAL_INPUT',
        selectedOptionId: 'choice', actionRouteOptions: { options: [{
          optionId: 'choice', title: 'Opción de prueba', userInputsRequired: [{ parameterName: 'field', questionForUser: 'Dato' }],
        }] },
      })))
    vi.stubGlobal('fetch', fetchMock)
    await analizarConApi(reclamoValido)
    const result = await seleccionarAccionConApi(id, 'choice')
    expect(new Headers(fetchMock.mock.calls[1][1].headers).get('X-Claim-Token')).toBe(token)
    expect(result.camposFormulario[0].parameterName).toBe('field')
    expect(result.opcionSeleccionada).toBe('Opción de prueba')
    expect(fetchMock.mock.calls[1][0]).toContain('/select-action/' + id)
  })
})
