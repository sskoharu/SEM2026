import { afterEach, describe, expect, it, vi } from 'vitest'
import { analizarConApi, ejecutarCalculosConApi, enviarFormularioConApi, esRespuestaSesion, seleccionarAccionConApi } from '../../src/modules/reclamos/services/reclamosApi'
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

  it('acepta la respuesta resumida DONE de los endpoints de Capa 3', async () => {
    const id = '22222222-2222-2222-2222-222222222222'
    vi.stubGlobal('fetch', vi.fn().mockResolvedValue(
      new Response(JSON.stringify({
        status: 'DONE',
        message: 'Formulario validado y procesado exitosamente.',
        executionPlan: { action_title: 'Reposición' },
      }), { headers: { 'Content-Type': 'application/json' } }),
    ))

    const resultado = await enviarFormularioConApi(id, { providerAddress: 'Avenida Quito' })

    expect(resultado.estadoCapa3).toBe('DONE')
    expect(resultado.planEjecucion).toEqual({ action_title: 'Reposición' })
  })

  it('acepta un plan de Capa 4 sin tareas de cálculo', async () => {
    const id = '33333333-3333-3333-3333-333333333333'
    const fetchMock = vi.fn().mockResolvedValue(new Response(JSON.stringify({
      sessionId: id,
      status: 'CALCULATIONS_COMPLETED',
      message: 'Cálculos deterministas ejecutados exitosamente.',
      executionPlan: {
        action_title: 'Corrección de la publicidad engañosa',
        base_article: '7, 12 y 17',
      },
      nextPhase: 'DOCUMENT_GENERATION',
    }), { headers: { 'Content-Type': 'application/json' } }))
    vi.stubGlobal('fetch', fetchMock)

    const resultado = await ejecutarCalculosConApi(id)

    expect(resultado.executionPlan).toEqual({
      action_title: 'Corrección de la publicidad engañosa',
      base_article: '7, 12 y 17',
    })
  })
})
