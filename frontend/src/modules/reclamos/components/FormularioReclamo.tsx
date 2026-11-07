import type { FormEvent } from 'react'
import type { Estado, Reclamo } from '../types/reclamo.types'

type Propiedades = {
  form: Reclamo
  errores: string[]
  estado: Estado
  cambiar: <K extends keyof Reclamo>(campo: K, valor: Reclamo[K]) => void
  analizar: (evento: FormEvent<HTMLFormElement>) => void
  reiniciar: () => void
  abrirAvisoTransparencia?: () => void
}

export function FormularioReclamo({ form, errores, estado, cambiar, analizar, reiniciar, abrirAvisoTransparencia }: Propiedades) {
  return (
    <section className="panel" aria-label="Formulario para registrar nuevo reclamo">
      <div className="panel-heading">
        <h2>
          Registrar nuevo reclamo
        </h2>
        <p>
          Describa los hechos sin nombres, identificaciones ni datos de contacto reales. El relato se procesa con IA; la identificación se completa después, fuera del modelo.
        </p>
      </div>

      {/* Caja informativa de minimización de datos personales para la interacción con IA */}
      <div className="privacy-callout" role="note" aria-label="Instrucciones de minimización de datos">
        <div className="privacy-callout-icon" aria-hidden="true">🛡️</div>
        <div className="privacy-callout-content">
          <strong>Minimización de datos personales documentada:</strong>
          <p>
            Por su seguridad y privacidad, relate los hechos de forma objetiva. <strong>No incluya su nombre real, número de cédula, teléfono, correo ni datos bancarios</strong>. El texto del relato será analizado por un modelo de Inteligencia Artificial. Sus datos identificativos se solicitarán únicamente al final para la plantilla Word y nunca se enviarán a la IA.
          </p>
          {abrirAvisoTransparencia && (
            <button
              type="button"
              className="privacy-callout-link"
              onClick={abrirAvisoTransparencia}
            >
              Consultar detalles de política de datos y limitaciones &rarr;
            </button>
          )}
        </div>
      </div>

      <form onSubmit={analizar} noValidate aria-describedby={errores.length ? 'errores-reclamo' : undefined}>
        <div className="section-heading-wrapper">
          <h3 className="section-heading">
            Relato del reclamo
          </h3>
          <span className="ai-processing-pill">
            <span aria-hidden="true">🤖</span> Asistido por IA
          </span>
        </div>

        <label className="full-field">
          ¿Qué ocurrió? *
          <textarea disabled={estado === 'cargando'} rows={5} maxLength={2000} value={form.incidentDescription} onChange={e => cambiar('incidentDescription', e.target.value)} placeholder="Explique qué sucedió y qué respuesta recibió del proveedor (sin ingresar nombres reales ni cédula)..." />
          <small>{form.incidentDescription.length}/2000 caracteres</small>
        </label>
        {errores.length > 0 && <div id="errores-reclamo" className="error-message" role="alert">
          <strong>
            Revise la información:
          </strong>
          <ul>
            {errores.map(error => (
              <li key={error}>{error}</li>
            ))}
          </ul>
        </div>}
        <div className="form-actions">
          <button type="button" className="btn btn-light" onClick={reiniciar}>
            Limpiar
          </button>
          <button type="button" className="btn btn-outline" disabled title="Requiere persistencia del backend">
            Guardar borrador (pendiente)
          </button>
          <button type="submit" className="btn btn-primary" disabled={estado === 'cargando'}>{estado === 'cargando' ? 'Analizando...' : 'Analizar con IA'}</button>
        </div>
      </form>
    </section>
  )
}
