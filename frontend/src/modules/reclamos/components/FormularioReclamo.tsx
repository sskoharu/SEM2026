import type { FormEvent } from 'react'
import type { Estado, Reclamo, Tipo } from '../types/reclamo.types'
import { opcionesProducto, opcionesServicio } from '../constants/reclamo.constants'

type Propiedades = {
  form: Reclamo
  errores: string[]
  estado: Estado
  cambiar: <K extends keyof Reclamo>(campo: K, valor: Reclamo[K]) => void
  cambiarTipo: (tipo: Tipo) => void
  analizar: (evento: FormEvent<HTMLFormElement>) => void
  reiniciar: () => void
}

export function FormularioReclamo({ form, errores, estado, cambiar, cambiarTipo, analizar, reiniciar }: Propiedades) {
  const soluciones = form.tipo === 'servicio' ? opcionesServicio : opcionesProducto
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
            <input disabled={estado === 'cargando'} value={form.nombre} onChange={e => cambiar('nombre', e.target.value)} placeholder="Nombre y apellido" />
          </label>
          <label>
            Correo electrónico *
            <input disabled={estado === 'cargando'} type="email" value={form.correo} onChange={e => cambiar('correo', e.target.value)} placeholder="correo@ejemplo.com" />
          </label>
          <label>
            Teléfono (opcional)
            <input disabled={estado === 'cargando'} value={form.telefono} onChange={e => cambiar('telefono', e.target.value)} placeholder="099..." />
          </label>
          <label>
            Cédula (opcional)
            <input disabled={estado === 'cargando'} value={form.cedula} onChange={e => cambiar('cedula', e.target.value)} placeholder="Número de identificación" />
          </label>
        </div>
        <h3 className="section-heading">
          2. Información de la compra o servicio
        </h3>
        <div className="form-grid">
          <label>
            Tipo de reclamo *
            <select disabled={estado === 'cargando'} value={form.tipo} onChange={e => cambiarTipo(e.target.value as Tipo)}>
              <option value="producto">Producto defectuoso</option>
              <option value="servicio">Problema con un servicio</option>
              <option value="garantia">Problema de garantía</option>
              <option value="otro">Otro reclamo</option>
            </select>
          </label>
          <label>
            Producto o servicio *
            <input disabled={estado === 'cargando'} value={form.producto} onChange={e => cambiar('producto', e.target.value)} placeholder="Ej. computadora portátil" />
          </label>
          <label>
            Proveedor o establecimiento *
            <input disabled={estado === 'cargando'} value={form.proveedor} onChange={e => cambiar('proveedor', e.target.value)} placeholder="Nombre del proveedor" />
          </label>
          <label>
            Fecha de compra (opcional)
            <input disabled={estado === 'cargando'} type="date" value={form.fecha} onChange={e => cambiar('fecha', e.target.value)} />
          </label>
          <label>
            Valor pagado USD (opcional)
            <input disabled={estado === 'cargando'} type="number" min="0" step="0.01" value={form.valor} onChange={e => cambiar('valor', e.target.value)} placeholder="0.00" />
          </label>
        </div>
        <div className="receipt-panel">
          <label className="checkbox-label">
            <input disabled={estado === 'cargando'} type="checkbox" checked={form.comprobante} onChange={e => cambiar('comprobante', e.target.checked)} /> Dispongo de factura o nota de venta (opcional)</label>
          <p>
            No es obligatorio disponer de comprobante. No se adjuntarán archivos ni se utilizará OCR.
          </p>
          {form.comprobante && <div className="form-grid">
            <label>
              Tipo de comprobante
              <select disabled={estado === 'cargando'} value={form.tipoComprobante} onChange={e => cambiar('tipoComprobante', e.target.value)}>
                <option value="factura">Factura</option>
                <option value="nota">Nota de venta</option>
              </select>
            </label>
            <label>
              Número (opcional)
              <input disabled={estado === 'cargando'} value={form.numeroComprobante} onChange={e => cambiar('numeroComprobante', e.target.value)} placeholder="Número del comprobante" />
            </label>
          </div>}
        </div>
        <h3 className="section-heading">
          3. Detalle del reclamo
        </h3>
        <label className="full-field">
          Describa qué ocurrió *
          <textarea disabled={estado === 'cargando'} rows={5} maxLength={5000} value={form.descripcion} onChange={e => cambiar('descripcion', e.target.value)} placeholder="Explique qué sucedió y qué respuesta recibió del proveedor..." />
          <small>{form.descripcion.length}/5000 caracteres</small>
        </label>
        <fieldset className="solution-fieldset">
          <legend>
            ¿Qué solución solicita? *
          </legend>
          <div className="solution-options">
            {soluciones.map(([valor, texto]) => (
              <label key={valor}>
                <input disabled={estado === 'cargando'} type="radio" name="solucion" checked={form.solucion === valor} onChange={() => cambiar('solucion', valor)} />
                {texto}
              </label>
            ))}
          </div>
          {form.solucion === 'otra' && <input disabled={estado === 'cargando'} aria-label="Describa otra solución" value={form.otraSolucion} onChange={e => cambiar('otraSolucion', e.target.value)} placeholder="Describa la solución solicitada" />}
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
          <button type="submit" className="btn btn-primary" disabled={estado === 'cargando'}>{estado === 'cargando' ? 'Analizando...' : 'Analizar (simulación)'}</button>
        </div>
      </form>
    </section>
  )
}
