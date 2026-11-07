// @vitest-environment jsdom
import { afterEach, describe, expect, it, vi } from 'vitest'
import { cleanup, render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { AvisoTransparenciaIa } from '../../src/modules/reclamos/components/AvisoTransparenciaIa'

afterEach(() => {
  cleanup()
  vi.restoreAllMocks()
})

describe('Componente AvisoTransparenciaIa', () => {
  it('renderiza el banner de transparencia con los 3 avisos fundamentales', () => {
    render(<AvisoTransparenciaIa abierto={false} alCerrar={vi.fn()} alAbrir={vi.fn()} />)

    expect(screen.getByText('Aviso de Transparencia del Sistema')).toBeTruthy()
    expect(screen.getByText(/Interacción con IA/i)).toBeTruthy()
    expect(screen.getByText(/Limitaciones del sistema/i)).toBeTruthy()
    expect(screen.getByText(/Minimización de datos/i)).toBeTruthy()
    expect(
      screen.getByRole('button', { name: /Ver aviso completo y política de datos/i }),
    ).toBeTruthy()
  })

  it('llama a alAbrir al presionar el botón del banner', async () => {
    const user = userEvent.setup()
    const alAbrirMock = vi.fn()
    render(<AvisoTransparenciaIa abierto={false} alCerrar={vi.fn()} alAbrir={alAbrirMock} />)

    await user.click(screen.getByRole('button', { name: /Ver aviso completo y política de datos/i }))
    expect(alAbrirMock).toHaveBeenCalledTimes(1)
  })

  it('muestra el modal y permite navegar entre las secciones de IA, Limitaciones y Minimización de datos', async () => {
    const user = userEvent.setup()
    const alCerrarMock = vi.fn()
    render(<AvisoTransparenciaIa abierto={true} alCerrar={alCerrarMock} alAbrir={vi.fn()} />)

    // El diálogo modal está abierto
    const modal = screen.getByRole('dialog', { name: /Transparencia, Limitaciones y Tratamiento de Datos/i })
    expect(modal).toBeTruthy()

    // Pestaña 1: Interacción con IA (por defecto)
    expect(screen.getByText(/Aviso de interacción con Inteligencia Artificial/i)).toBeTruthy()
    expect(screen.getByText(/Herramienta de asistencia, no patrocinio legal/i)).toBeTruthy()

    // Cambiar a pestaña 2: Limitaciones del sistema
    await user.click(screen.getByRole('tab', { name: /Limitaciones del Sistema/i }))
    expect(screen.getByText(/Limitaciones conocidas del sistema/i)).toBeTruthy()
    expect(screen.getByText(/Alcance normativo acotado/i)).toBeTruthy()
    expect(screen.getByText(/Supervisión humana obligatoria/i)).toBeTruthy()

    // Cambiar a pestaña 3: Tratamiento y minimización de datos
    await user.click(screen.getByRole('tab', { name: /Tratamiento y Minimización de Datos/i }))
    expect(screen.getByText(/Tratamiento y minimización de datos personales documentada/i)).toBeTruthy()
    expect(screen.getByText(/Ley Orgánica de Protección de Datos Personales/i)).toBeTruthy()
    expect(screen.getByText(/Fase de relato e interacción con IA/i)).toBeTruthy()
    expect(screen.getByText(/Fase de generación documental/i)).toBeTruthy()

    // Cerrar modal mediante botón de pie
    await user.click(screen.getByRole('button', { name: /Entendido y cerrar/i }))
    expect(alCerrarMock).toHaveBeenCalledTimes(1)
  })

  it('permite cerrar el modal presionando Escape', async () => {
    const user = userEvent.setup()
    const alCerrarMock = vi.fn()
    render(<AvisoTransparenciaIa abierto={true} alCerrar={alCerrarMock} alAbrir={vi.fn()} />)

    await user.keyboard('{Escape}')
    expect(alCerrarMock).toHaveBeenCalledTimes(1)
  })
})
