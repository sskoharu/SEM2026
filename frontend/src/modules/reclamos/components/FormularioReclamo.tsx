import type { FormEvent } from 'react'
import type { ClaimType, Estado, Reclamo } from '../types/reclamo.types'
import { opcionesProducto, opcionesServicio } from '../constants/reclamo.constants'

type Propiedades = {
  form: Reclamo
  errores: string[]
  estado: Estado
  cambiar: <K extends keyof Reclamo>(campo: K, valor: Reclamo[K]) => void
  cambiarTipo: (tipo: ClaimType) => void
  analizar: (evento: FormEvent<HTMLFormElement>) => void
  reiniciar: () => void
  modoApi: boolean
}

export function FormularioReclamo({ form, errores, estado, cambiar, cambiarTipo, analizar, reiniciar, modoApi }: Propiedades) {
  const soluciones = form.claimType === 'servicio' ? opcionesServicio : opcionesProducto
  return (
    <section className="panel">
      <div className="panel-heading">
        <h2>
          Registrar nuevo reclamo
        </h2>
        <p>
          Los campos con * son obligatorios.
        </p>
      </div>
      <form onSubmit={analizar} noValidate aria-describedby={errores.length ? 'errores-reclamo' : undefined}>
        <h3 className="section-heading">
          1. Datos del consumidor
        </h3>
        <div className="form-grid">
          <label>
            Nombre completo *
            <input disabled={estado === 'cargando'} value={form.fullName} onChange={e => cambiar('fullName', e.target.value)} placeholder="Nombre y apellido" />
          </label>
          <label>
            Correo electrónico (opcional)
            <input disabled={estado === 'cargando'} type="email" value={form.email} onChange={e => cambiar('email', e.target.value)} placeholder="correo@ejemplo.com" />
          </label>
          <label>
            Teléfono (opcional)
            <input disabled={estado === 'cargando'} value={form.phone} onChange={e => cambiar('phone', e.target.value)} placeholder="099..." />
          </label>
          <label>
            Cédula *
            <input disabled={estado === 'cargando'} inputMode="numeric" maxLength={10} value={form.idCard} onChange={e => cambiar('idCard', e.target.value)} placeholder="10 dígitos" />
          </label>
        </div>
        <h3 className="section-heading">
          2. Información de la compra o servicio
        </h3>
        <div className="form-grid">
          <label>
            Tipo de reclamo *
            <select disabled={estado === 'cargando'} value={form.claimType} onChange={e => cambiarTipo(e.target.value as ClaimType)}>
              <option value="producto">Producto defectuoso</option>
              <option value="servicio">Problema con un servicio</option>
              <option value="garantia">Problema de garantía</option>
              <option value="otro">Otro reclamo</option>
            </select>
          </label>
          <label>
            Producto o servicio *
            <input disabled={estado === 'cargando'} value={form.productOrService} onChange={e => cambiar('productOrService', e.target.value)} placeholder="Ej. computadora portátil" />
          </label>
          <label>
            Proveedor o establecimiento *
            <input disabled={estado === 'cargando'} value={form.provider} onChange={e => cambiar('provider', e.target.value)} placeholder="Nombre del proveedor" />
          </label>
          <label>
            Ciudad del proveedor *
            <input disabled={estado === 'cargando'} value={form.providerCity} onChange={e => cambiar('providerCity', e.target.value)} placeholder="Ej. Quito" />
          </label>
          <label>
            Fecha de compra *
            <input disabled={estado === 'cargando'} type="date" value={form.purchaseDate} onChange={e => cambiar('purchaseDate', e.target.value)} />
          </label>
          <label>
            Valor pagado USD *
            <input disabled={estado === 'cargando'} type="number" min="0" step="0.01" value={form.paidAmount} onChange={e => cambiar('paidAmount', e.target.value)} placeholder="0.00" />
          </label>
        </div>
        <h3 className="section-heading">
          3. Detalle del reclamo
        </h3>
        <label className="full-field">
          Describa qué ocurrió *
          <textarea disabled={estado === 'cargando'} rows={5} maxLength={2000} value={form.incidentDescription} onChange={e => cambiar('incidentDescription', e.target.value)} placeholder="Explique qué sucedió y qué respuesta recibió del proveedor..." />
          <small>{form.incidentDescription.length}/2000 caracteres</small>
        </label>
        <fieldset className="solution-fieldset">
          <legend>
            ¿Qué solución solicita? *
          </legend>
          <div className="solution-options">
            {soluciones.map(([valor, texto]) => (
              <label key={valor}>
                <input disabled={estado === 'cargando'} type="radio" name="solucion" checked={form.requestedSolution === valor} onChange={() => cambiar('requestedSolution', valor)} />
                {texto}
              </label>
            ))}
          </div>
          {form.requestedSolution === 'otra' && <input disabled={estado === 'cargando'} aria-label="Describa otra solución" value={form.otherSolution} onChange={e => cambiar('otherSolution', e.target.value)} placeholder="Describa la solución solicitada" />}
        </fieldset>
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
          <button type="submit" className="btn btn-primary" disabled={estado === 'cargando'}>{estado === 'cargando' ? 'Analizando...' : modoApi ? 'Analizar con IA' : 'Analizar (simulación)'}</button>
        </div>
      </form>
    </section>
  )
}
