import type { FormEvent } from 'react'
import type { Estado, Reclamo } from '../types/reclamo.types'

type Propiedades = {
  form: Reclamo
  errores: string[]
  estado: Estado
  cambiar: <K extends keyof Reclamo>(campo: K, valor: Reclamo[K]) => void
  analizar: (evento: FormEvent<HTMLFormElement>) => void
  reiniciar: () => void
}

export function FormularioReclamo({ form, errores, estado, cambiar, analizar, reiniciar }: Propiedades) {
  return (
    <section className="panel">
      <div className="panel-heading">
        <h2>
          Registrar nuevo reclamo
        </h2>
        <p>
          Describa los hechos sin nombres, identificaciones ni datos de contacto reales. El relato se procesa con IA; la identificación se completa después, fuera del modelo.
        </p>
      </div>
      <form onSubmit={analizar} noValidate aria-describedby={errores.length ? 'errores-reclamo' : undefined}>
        <h3 className="section-heading">
          Relato del reclamo
        </h3>
        <label className="full-field">
          ¿Qué ocurrió? *
          <textarea disabled={estado === 'cargando'} rows={5} maxLength={2000} value={form.incidentDescription} onChange={e => cambiar('incidentDescription', e.target.value)} placeholder="Explique qué sucedió y qué respuesta recibió del proveedor..." />
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
