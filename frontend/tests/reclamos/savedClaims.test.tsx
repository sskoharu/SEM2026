// @vitest-environment jsdom
import { afterEach, describe, expect, it, vi } from 'vitest'
import { cleanup, render, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import {
  guardarReclamo,
  listarReclamosGuardados,
  obtenerIdCardLocal,
  obtenerReclamoGuardado,
  reanudarReclamo,
} from '../../src/modules/reclamos/services/savedClaimsService'
import { forgetSessions, rememberSession } from '../../src/modules/reclamos/services/sessionAccess'
import { GuardarReclamo } from '../../src/modules/reclamos/components/GuardarReclamo'
import { ReclamosGuardados } from '../../src/modules/reclamos/components/ReclamosGuardados'

afterEach(() => {
  cleanup()
  forgetSessions()
  vi.unstubAllGlobals()
  vi.restoreAllMocks()
  localStorage.clear()
})

describe('Endpoints /save, /saved y /resume en savedClaimsService', () => {
  it('guardarReclamo envía POST a /api/v1/claims/{sessionId}/save con el payload esperado', async () => {
    const sessionId = 'test-session-1234'
    const payload = {
      idCard: '8034867066076',
      fullName: 'Rubén Godoy Nazario',
      contactInfo: 'aliquip ipsum',
    }

    const fetchMock = vi.fn().mockResolvedValue(
      new Response(
        JSON.stringify({
          id: 'claim-saved-999',
          sessionId,
          idCard: payload.idCard,
          fullName: payload.fullName,
          status: 'SAVED',
        }),
        { headers: { 'Content-Type': 'application/json' } },
      ),
    )
    vi.stubGlobal('fetch', fetchMock)

    const resultado = await guardarReclamo(sessionId, payload)

    expect(fetchMock).toHaveBeenCalledTimes(1)
    const [url, opciones] = fetchMock.mock.calls[0]
    expect(url).toContain(`/api/v1/claims/${sessionId}/save`)
    expect(opciones.method).toBe('POST')
    expect(JSON.parse(opciones.body as string)).toEqual({
      idCard: '8034867066076',
      fullName: 'Rubén Godoy Nazario',
      contactInfo: 'aliquip ipsum',
    })
    expect(resultado.id).toBe('claim-saved-999')
    expect(obtenerIdCardLocal()).toBe('8034867066076')
  })

  it('listarReclamosGuardados envía GET a /api/v1/claims/saved con el parámetro idCard', async () => {
    const idCard = '1315779726'
    const listaFixture = [
      {
        id: 'saved-abc-1',
        sessionId: 'session-1',
        idCard: '1315779726',
        fullName: 'Rubén Godoy Nazario',
        contactInfo: '0999999999',
        status: 'EN_PROCESO',
        createdAt: '2026-11-03',
      },
    ]

    const fetchMock = vi.fn().mockResolvedValue(
      new Response(JSON.stringify(listaFixture), {
        headers: { 'Content-Type': 'application/json' },
      }),
    )
    vi.stubGlobal('fetch', fetchMock)

    const resultado = await listarReclamosGuardados(idCard)

    expect(fetchMock).toHaveBeenCalledTimes(1)
    const [url, opciones] = fetchMock.mock.calls[0]
    expect(url).toContain('/api/v1/claims/saved?')
    expect(url).toContain('idCard=1315779726')
    expect(opciones.method).toBe('GET')
    expect(resultado).toHaveLength(1)
    expect(resultado[0].id).toBe('saved-abc-1')
    expect(resultado[0].fullName).toBe('Rubén Godoy Nazario')
    expect(obtenerIdCardLocal()).toBe('1315779726')
  })

  it('obtenerReclamoGuardado envía GET a /api/v1/claims/saved/{id}?idCard=... con ruta y query params', async () => {
    const claimId = '45e3481c-2df9-4afa-98c0-ea9abcad111e'
    const idCard = '1315779726'
    const detalleFixture = {
      id: claimId,
      sessionId: 'sesion-45e',
      idCard,
      fullName: 'Rubén Godoy Nazario',
      contactInfo: 'aliquip ipsum',
      status: 'GUARDADO',
      createdAt: '2026-11-03T21:00:00Z',
    }

    const fetchMock = vi.fn().mockResolvedValue(
      new Response(JSON.stringify(detalleFixture), {
        headers: { 'Content-Type': 'application/json' },
      }),
    )
    vi.stubGlobal('fetch', fetchMock)

    const resultado = await obtenerReclamoGuardado(claimId, idCard)

    expect(fetchMock).toHaveBeenCalledTimes(1)
    const [url, opciones] = fetchMock.mock.calls[0]
    expect(url).toContain(`/api/v1/claims/saved/${claimId}`)
    expect(url).toContain('idCard=1315779726')
    expect(opciones.method).toBe('GET')
    expect(resultado.id).toBe(claimId)
    expect(resultado.idCard).toBe(idCard)
    expect(resultado.fullName).toBe('Rubén Godoy Nazario')
  })

  it('guardarReclamo incluye X-Claim-Token si la sesión fue autenticada', async () => {
    const sessionId = 'session-con-token-111'
    const token = 'c'.repeat(45)
    rememberSession(sessionId, token)

    const fetchMock = vi.fn().mockResolvedValue(
      new Response(
        JSON.stringify({
          sessionId,
          status: 'GUARDADO',
        }),
        { headers: { 'Content-Type': 'application/json' } },
      ),
    )
    vi.stubGlobal('fetch', fetchMock)

    await guardarReclamo(sessionId, {
      idCard: '8034867066076',
      fullName: 'Rubén Godoy Nazario',
      contactInfo: 'aliquip ipsum',
    })

    const [, opciones] = fetchMock.mock.calls[0]
    expect(new Headers(opciones.headers).get('X-Claim-Token')).toBe(token)
  })

  it('reanudarReclamo envía POST a /api/v1/claims/saved/{id}/resume con { idCard }', async () => {
    const claimId = 'claim-saved-456'
    const idCard = '1315779726'
    const newSessionToken = 'd'.repeat(45)

    const sesionBackend = {
      sessionId: 'resumed-session-789',
      layer1State: 'DONE',
      layer2State: 'WAITING_FOR_USER',
      currentParameterBeingAsked: 'purchaseDate',
      currentQuestionForUser: '¿En qué fecha adquirió el bien?',
      pendingParametersToAsk: ['purchaseDate'],
      collectedParameters: {
        product: 'Refrigeradora',
      },
    }

    const fetchMock = vi.fn().mockResolvedValue(
      new Response(JSON.stringify(sesionBackend), {
        headers: {
          'Content-Type': 'application/json',
          'X-Claim-Token': newSessionToken,
        },
      }),
    )
    vi.stubGlobal('fetch', fetchMock)

    const resultado = await reanudarReclamo(claimId, idCard)

    expect(fetchMock).toHaveBeenCalledTimes(1)
    const [url, opciones] = fetchMock.mock.calls[0]
    expect(url).toContain(`/api/v1/claims/saved/${claimId}/resume`)
    expect(opciones.method).toBe('POST')
    expect(JSON.parse(opciones.body as string)).toEqual({
      idCard: '1315779726',
    })

    expect(resultado.sessionId).toBe('resumed-session-789')
    expect(resultado.analisis.estadoCapa2).toBe('WAITING_FOR_USER')
    expect(resultado.analisis.preguntas[0].texto).toBe('¿En qué fecha adquirió el bien?')
    expect(obtenerIdCardLocal()).toBe('1315779726')
  })
})

describe('Componente GuardarReclamo', () => {
  it('solicita identificación, nombre completo e información de contacto y envía el formulario', async () => {
    const sessionId = 'sesion-test-999'
    const fetchMock = vi.fn().mockResolvedValue(
      new Response(
        JSON.stringify({
          id: 'rec-guardado-101',
          sessionId,
          status: 'SAVED',
        }),
        { headers: { 'Content-Type': 'application/json' } },
      ),
    )
    vi.stubGlobal('fetch', fetchMock)
    const alGuardar = vi.fn()
    const user = userEvent.setup()

    render(
      <GuardarReclamo
        sessionId={sessionId}
        onGuardadoExitoso={alGuardar}
      />,
    )

    // Intenta enviar vacío
    await user.click(screen.getByRole('button', { name: 'Guardar reclamo' }))
    expect(
      screen.getByText(/Debe completar su identificación, nombre completo y datos de contacto/),
    ).toBeTruthy()
    expect(fetchMock).not.toHaveBeenCalled()

    // Llena los tres campos requeridos
    await user.type(screen.getByLabelText(/Identificación o cédula para guardar/), '8034867066076')
    await user.type(screen.getByLabelText(/Nombre completo para guardar/), 'Rubén Godoy Nazario')
    await user.type(screen.getByLabelText(/Información de contacto para guardar/), 'aliquip ipsum')

    await user.click(screen.getByRole('button', { name: 'Guardar reclamo' }))

    await waitFor(() => {
      expect(screen.getByText(/Reclamo guardado exitosamente/)).toBeTruthy()
    })

    expect(screen.getByText('rec-guardado-101')).toBeTruthy()
    expect(fetchMock).toHaveBeenCalledTimes(1)
    const [url, opciones] = fetchMock.mock.calls[0]
    expect(url).toContain(`/api/v1/claims/${sessionId}/save`)
    expect(JSON.parse(opciones.body as string)).toEqual({
      idCard: '8034867066076',
      fullName: 'Rubén Godoy Nazario',
      contactInfo: 'aliquip ipsum',
    })
    expect(alGuardar).toHaveBeenCalledTimes(1)
  })
})

describe('Componente ReclamosGuardados', () => {
  it('permite consultar reclamos con GET /api/v1/claims/saved?idCard=... y reanudar desde la lista', async () => {
    const idCard = '1315779726'
    const onReanudar = vi.fn()
    const fetchMock = vi.fn()
      // 1. Respuesta de GET /saved?idCard=1315779726
      .mockResolvedValueOnce(
        new Response(
          JSON.stringify([
            {
              id: 'reclamo-item-777',
              sessionId: 'session-777',
              idCard,
              fullName: 'Rubén Godoy Nazario',
              contactInfo: 'ruben@example.com',
              status: 'PENDIENTE',
            },
          ]),
          { headers: { 'Content-Type': 'application/json' } },
        ),
      )
      // 2. Respuesta de POST /saved/reclamo-item-777/resume
      .mockResolvedValueOnce(
        new Response(
          JSON.stringify({
            sessionId: 'session-777',
            layer1State: 'DONE',
            layer2State: 'DONE',
            collectedParameters: {},
          }),
          { headers: { 'Content-Type': 'application/json' } },
        ),
      )

    vi.stubGlobal('fetch', fetchMock)
    const user = userEvent.setup()

    render(<ReclamosGuardados onReanudar={onReanudar} />)

    // El tab por defecto es historial por cédula
    const inputCedula = screen.getByLabelText('Identificación para consultar reclamos')
    await user.clear(inputCedula)
    await user.type(inputCedula, idCard)

    await user.click(screen.getByRole('button', { name: 'Consultar reclamos guardados' }))

    await waitFor(() => {
      expect(screen.getByText('reclamo-item-777')).toBeTruthy()
    })

    expect(fetchMock.mock.calls[0][0]).toContain('/api/v1/claims/saved?')
    expect(fetchMock.mock.calls[0][0]).toContain('idCard=1315779726')
    expect(fetchMock.mock.calls[0][1].method).toBe('GET')

    // Ahora hacer clic en "Reanudar" del elemento encontrado
    await user.click(screen.getByRole('button', { name: 'Reanudar' }))

    await waitFor(() => {
      expect(onReanudar).toHaveBeenCalledTimes(1)
    })

    expect(fetchMock.mock.calls[1][0]).toContain('/api/v1/claims/saved/reclamo-item-777/resume')
    expect(fetchMock.mock.calls[1][1].method).toBe('POST')
    expect(JSON.parse(fetchMock.mock.calls[1][1].body as string)).toEqual({ idCard: '1315779726' })
  })

  it('permite consultar detalle con GET /api/v1/claims/saved/{id}?idCard=...', async () => {
    const claimId = '45e3481c-2df9-4afa-98c0-ea9abcad111e'
    const idCard = '1315779726'
    const onReanudar = vi.fn()

    const fetchMock = vi.fn().mockResolvedValue(
      new Response(
        JSON.stringify({
          id: claimId,
          sessionId: 'sesion-45e',
          idCard,
          fullName: 'Rubén Godoy Nazario',
          contactInfo: 'ruben@example.com',
          status: 'EN_REVISION',
          createdAt: '2026-11-03',
        }),
        { headers: { 'Content-Type': 'application/json' } },
      ),
    )
    vi.stubGlobal('fetch', fetchMock)
    const user = userEvent.setup()

    render(<ReclamosGuardados onReanudar={onReanudar} />)

    // Cambiar al tab de ID
    await user.click(screen.getByRole('tab', { name: /Consultar por ID de reclamo/ }))

    // Llenar inputs
    await user.type(screen.getByLabelText('ID del reclamo guardado (id) *'), claimId)
    await user.clear(screen.getByLabelText('Identificación / Cédula (idCard) *'))
    await user.type(screen.getByLabelText('Identificación / Cédula (idCard) *'), idCard)

    // Hacer clic en "Consultar detalle (GET)"
    await user.click(screen.getByRole('button', { name: 'Consultar detalle (GET)' }))

    await waitFor(() => {
      expect(screen.getByText('EN_REVISION')).toBeTruthy()
    })

    expect(fetchMock).toHaveBeenCalledTimes(1)
    const [url, opciones] = fetchMock.mock.calls[0]
    expect(url).toContain(`/api/v1/claims/saved/${claimId}`)
    expect(url).toContain('idCard=1315779726')
    expect(opciones.method).toBe('GET')
  })

  it('permite consultar directamente por ID y cédula para reanudar', async () => {
    const claimId = 'mi-reclamo-id-555'
    const idCard = '1315779726'
    const onReanudar = vi.fn()

    const fetchMock = vi.fn().mockResolvedValue(
      new Response(
        JSON.stringify({
          sessionId: 'sesion-reanudada-444',
          layer1State: 'DONE',
          layer2State: 'DONE',
          layer3State: 'WAITING_FOR_DECISION',
          viabilityReport: 'Informe de prueba',
        }),
        { headers: { 'Content-Type': 'application/json' } },
      ),
    )
    vi.stubGlobal('fetch', fetchMock)
    const user = userEvent.setup()

    render(<ReclamosGuardados onReanudar={onReanudar} />)

    // Cambiar al tab directo
    await user.click(screen.getByRole('tab', { name: /Consultar por ID de reclamo/ }))

    // Llenar inputs en tab directo
    await user.type(screen.getByLabelText('ID del reclamo guardado (id) *'), claimId)
    await user.clear(screen.getByLabelText('Identificación / Cédula (idCard) *'))
    await user.type(screen.getByLabelText('Identificación / Cédula (idCard) *'), idCard)

    await user.click(screen.getByRole('button', { name: /Consultar y Reanudar/ }))

    await waitFor(() => {
      expect(onReanudar).toHaveBeenCalledTimes(1)
    })

    expect(fetchMock).toHaveBeenCalledTimes(1)
    const [url, opciones] = fetchMock.mock.calls[0]
    expect(url).toContain(`/api/v1/claims/saved/${claimId}/resume`)
    expect(JSON.parse(opciones.body as string)).toEqual({ idCard: '1315779726' })
    expect(onReanudar).toHaveBeenCalledWith(
      'sesion-reanudada-444',
      expect.objectContaining({
        etapa: 'capa3',
        informeViabilidad: 'Informe de prueba',
      }),
    )
  })
})
