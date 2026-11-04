// @vitest-environment jsdom
import { afterEach, expect, it, vi } from 'vitest'
import { cleanup, fireEvent, render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import App from '../../src/App'
import { forgetSessions } from '../../src/modules/reclamos/services/sessionAccess'

afterEach(() => {
  cleanup()
  forgetSessions()
  vi.unstubAllGlobals()
  vi.restoreAllMocks()
})

it('recorre las cinco capas desde React, revisa datos completos y aprueba la descarga con token', async () => {
  const id = '11111111-1111-1111-1111-111111111111'
  const token = 'b'.repeat(43)
  const session = {
    sessionId: id,
    layer1State: 'DONE',
    layer2State: 'DONE',
    pendingParametersToAsk: [],
    collectedParameters: { pricePaid: '10' },
  }
  const json = (value: unknown, headers?: Record<string, string>) => new Response(JSON.stringify(value), {
    headers: { 'Content-Type': 'application/json', ...headers },
  })
  const fetchMock = vi.fn()
    .mockResolvedValueOnce(json({ ...session, layer2State: null }, { 'X-Claim-Token': token }))
    .mockResolvedValueOnce(json(session))
    .mockResolvedValueOnce(json({ ...session, layer3State: 'WAITING_FOR_DECISION', viabilityReport: 'Evaluación sintética con revisión pendiente.' }))
    .mockResolvedValueOnce(json({ ...session, layer3State: 'WAITING_FOR_ACTION_SELECTION', actionRouteOptions: {
      options: [{ optionId: 'option', title: 'Opción sintética', userInputsRequired: [] }],
    } }))
    .mockResolvedValueOnce(json({ ...session, layer3State: 'DONE', executionPlan: { action_title: 'Opción sintética' } }))
    .mockResolvedValueOnce(json({ sessionId: id, status: 'CALCULATIONS_COMPLETED', nextPhase: 'DOCUMENT_GENERATION', executionPlan: { calculationStatus: 'COMPLETED' } }))
    .mockResolvedValueOnce(json({ previewId: 'preview', hechosRedactados: 'Hechos sintéticos.', pretensionesRedactadas: 'Petición sintética.', documentoCompleto: 'Contenido íntegro sintético para revisión.' }))
    .mockResolvedValueOnce(new Response(new Blob(['synthetic-docx']), {
      headers: { 'Content-Type': 'application/vnd.openxmlformats-officedocument.wordprocessingml.document' },
    }))
  vi.stubGlobal('fetch', fetchMock)
  Object.defineProperty(URL, 'createObjectURL', { configurable: true, value: vi.fn(() => 'blob:fixture') })
  Object.defineProperty(URL, 'revokeObjectURL', { configurable: true, value: vi.fn() })
  vi.spyOn(HTMLAnchorElement.prototype, 'click').mockImplementation(() => {})
  const user = userEvent.setup()
  render(<App />)
  fireEvent.change(screen.getByRole('textbox', { name: /Qué ocurrió/ }), {
    target: { value: 'Compré un producto defectuoso; relato exclusivamente sintético.' },
  })
  await user.click(screen.getByRole('button', { name: 'Analizar con IA' }))
  await user.click(await screen.findByRole('button', { name: 'Continuar con la evaluación jurídica' }))
  await user.click(await screen.findByRole('button', { name: 'Aceptar evaluación y generar ruta de acción' }))
  await user.click(await screen.findByRole('button', { name: 'Elegir esta opción' }))
  await user.click(await screen.findByRole('button', { name: 'Ejecutar cálculos' }))
  await screen.findByText('Cálculos completados correctamente.')
  for (const [name, value] of [
    ['Nombre completo', 'Consumidor de prueba'],
    ['Identificación', 'TEST12345'],
    ['Correo electrónico', 'prueba@example.test'],
    ['Teléfono', '0999999999'],
    ['Nombre del proveedor', 'Proveedor ficticio'],
  ]) {
    fireEvent.change(screen.getByLabelText(name), { target: { value } })
  }
  await user.click(screen.getByRole('checkbox', { name: /Confirmo que deseo generar/ }))
  await user.click(screen.getByRole('button', { name: 'Preparar vista previa' }))
  await screen.findByText('Contenido íntegro sintético para revisión.')
  const approve = screen.getByRole('button', { name: 'Aprobar y descargar Word' }) as HTMLButtonElement
  expect(approve.disabled).toBe(true)
  await user.click(screen.getByRole('checkbox', { name: /He leído y apruebo el documento completo/ }))
  await user.click(approve)
  await screen.findByText('Sesión COMPLETED. Documento generado y aprobado.')
  expect(fetchMock).toHaveBeenCalledTimes(8)
  for (const call of fetchMock.mock.calls.slice(1)) {
    expect(new Headers(call[1].headers).get('X-Claim-Token')).toBe(token)
  }
  const payload = JSON.parse(fetchMock.mock.calls[7][1].body as string)
  expect(payload.fullName).toBe('Consumidor de prueba')
  expect(payload.email).toBe('prueba@example.test')
  expect(payload.approveFinalText).toBe(true)
  expect(payload.previewId).toBe('preview')
})
