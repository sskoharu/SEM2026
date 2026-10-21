// @vitest-environment jsdom
import { act, cleanup, fireEvent, render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import App from '../../src/App'
import * as simulacion from '../../src/modules/reclamos/services/simulacionService'
import { crearError } from '../../src/shared/http/httpErrors'
import { limpiarMetricas, obtenerMetricas } from '../../src/shared/metrics/metricasService'

function llenar() {
  fireEvent.change(screen.getByLabelText('Nombre completo *'), { target: { value: 'Consumidor de prueba' } })
  fireEvent.change(screen.getByLabelText('Correo electrónico *'), { target: { value: 'prueba@example.test' } })
  fireEvent.change(screen.getByLabelText('Producto o servicio *'), { target: { value: 'Computadora' } })
  fireEvent.change(screen.getByLabelText('Proveedor o establecimiento *'), { target: { value: 'Proveedor de prueba' } })
  fireEvent.change(screen.getByLabelText(/Describa qué ocurrió/), { target: { value: 'El equipo no funciona desde la primera utilización.' } })
  fireEvent.click(screen.getByLabelText('Cambio o reposición'))
}
function enviar() {
  fireEvent.submit(screen.getByRole('button', { name: 'Analizar (simulación)' }).closest('form')!)
}
async function terminarEspera() {
  await act(async () => { await vi.advanceTimersByTimeAsync(850) })
}

beforeEach(() => limpiarMetricas())
afterEach(() => { cleanup(); vi.useRealTimers(); vi.restoreAllMocks() })

describe('Flujo React en DOM de prueba, sin backend', () => {
  it('descarta el análisis anterior al editar y conserva los datos del formulario', async () => {
    vi.useFakeTimers()
    render(<App />)
    llenar()
    enviar()
    await terminarEspera()
    fireEvent.change(screen.getByLabelText('Proveedor o establecimiento *'), { target: { value: 'Otro proveedor' } })
    expect(screen.getByText('Esperando análisis')).toBeDefined()
    expect(screen.queryByText('Información complementaria')).toBeNull()
    expect((screen.getByLabelText('Nombre completo *') as HTMLInputElement).value).toBe('Consumidor de prueba')
  })
  it('muestra validaciones y permite corregir los datos', async () => {
    render(<App />)
    enviar()
    expect(screen.getByRole('alert').textContent).toContain('Ingrese el nombre completo.')
    const usuario = userEvent.setup()
    await usuario.type(screen.getByLabelText('Nombre completo *'), 'Prueba')
    expect((screen.getByLabelText('Nombre completo *') as HTMLInputElement).value).toBe('Prueba')
  })
  it('recorre carga, preguntas, resultado final y reinicio', async () => {
    vi.useFakeTimers()
    render(<App />)
    llenar()
    enviar()
    expect(screen.getByRole('status', { name: 'Preparando análisis' })).toBeDefined()
    expect((screen.getByRole('button', { name: 'Analizando...' }) as HTMLButtonElement).disabled).toBe(true)
    expect((screen.getByLabelText('Nombre completo *') as HTMLInputElement).disabled).toBe(true)
    expect(screen.getByText('Análisis inicial').closest('[aria-current="step"]')).not.toBeNull()
    await terminarEspera()
    expect(screen.getByText('Información complementaria')).toBeDefined()
    expect(screen.getByText(/Tokens de entrada, salida y total: No disponibles/)).toBeDefined()
    fireEvent.change(screen.getByLabelText(/Ya informó el problema/), { target: { value: 'El proveedor no respondió.' } })
    fireEvent.click(screen.getByRole('button', { name: 'Actualizar resultado (simulación)' }))
    expect(screen.getByText('Resultado final de demostración')).toBeDefined()
    expect(screen.getByText(/Respuesta: El proveedor no respondió/)).toBeDefined()
    expect(screen.getByText(/Preguntas sin respuesta: 3/)).toBeDefined()
    fireEvent.click(screen.getByRole('button', { name: 'Limpiar' }))
    expect(screen.getByText('Esperando análisis')).toBeDefined()
    expect((screen.getByLabelText('Nombre completo *') as HTMLInputElement).value).toBe('')
  })
  it('bloquea doble envío incluso antes del siguiente render', async () => {
    vi.useFakeTimers()
    const servicio = vi.spyOn(simulacion, 'analizarSimulacion')
    render(<App />)
    llenar()
    const formulario = screen.getByRole('button', { name: 'Analizar (simulación)' }).closest('form')!
    act(() => { fireEvent.submit(formulario); fireEvent.submit(formulario) })
    expect(servicio).toHaveBeenCalledTimes(1)
    await terminarEspera()
    expect(obtenerMetricas()).toHaveLength(1)
  })
  it('conserva datos al fallar y permite recuperar la solicitud', async () => {
    vi.useFakeTimers()
    vi.spyOn(simulacion, 'analizarSimulacion').mockRejectedValueOnce(crearError('timeout'))
    render(<App />)
    llenar()
    await act(async () => enviar())
    expect(screen.getByRole('alert').textContent).toContain('superó el tiempo de espera')
    expect((screen.getByLabelText('Nombre completo *') as HTMLInputElement).value).toBe('Consumidor de prueba')
    expect((screen.getByRole('button', { name: 'Analizar (simulación)' }) as HTMLButtonElement).disabled).toBe(false)
    enviar()
    await terminarEspera()
    expect(screen.getByText('Información complementaria')).toBeDefined()
  })
  it('limpiar cancela y una respuesta anterior no reaparece', async () => {
    vi.useFakeTimers()
    render(<App />)
    llenar()
    enviar()
    fireEvent.click(screen.getByRole('button', { name: 'Limpiar' }))
    await terminarEspera()
    expect(screen.getByText('Esperando análisis')).toBeDefined()
    expect(screen.queryByText('Información complementaria')).toBeNull()
    expect(obtenerMetricas()[0].estado).toBe('cancelada')
    llenar()
    enviar()
    await terminarEspera()
    expect(screen.getByText('Información complementaria')).toBeDefined()
  })
  it('cancela al desmontar la interfaz', async () => {
    vi.useFakeTimers()
    const vista = render(<App />)
    llenar()
    enviar()
    vista.unmount()
    await terminarEspera()
    expect(obtenerMetricas()[0].estado).toBe('cancelada')
  })
  it('muestra comprobante opcional, cambia soluciones y señala borrador pendiente', () => {
    render(<App />)
    fireEvent.click(screen.getByLabelText(/Dispongo de factura/))
    expect(screen.getByLabelText('Tipo de comprobante')).toBeDefined()
    fireEvent.change(screen.getByLabelText('Tipo de reclamo *'), { target: { value: 'servicio' } })
    expect(screen.getByLabelText('Corrección del servicio')).toBeDefined()
    expect(screen.queryByLabelText('Cambio o reposición')).toBeNull()
    expect((screen.getByRole('button', { name: 'Guardar borrador (pendiente)' }) as HTMLButtonElement).disabled).toBe(true)
  })
})
