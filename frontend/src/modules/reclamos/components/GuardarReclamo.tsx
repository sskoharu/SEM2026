import { useState, type FormEvent } from 'react'
import {
  guardarReclamo,
  obtenerIdCardLocal,
  type ReclamoGuardado,
} from '../services/savedClaimsService'
import { normalizarError } from '../../../shared/http/httpErrors'

type Props = {
  sessionId: string
  initialIdCard?: string
  initialFullName?: string
  initialContactInfo?: string
  onGuardadoExitoso?: (reclamo: ReclamoGuardado) => void
}

export function GuardarReclamo({
  sessionId,
  initialIdCard = '',
  initialFullName = '',
  initialContactInfo = '',
  onGuardadoExitoso,
}: Props) {
  const [idCard, setIdCard] = useState(() => initialIdCard.trim() || obtenerIdCardLocal())
  const [fullName, setFullName] = useState(() => initialFullName.trim())
  const [contactInfo, setContactInfo] = useState(() => initialContactInfo.trim())
  const [cargando, setCargando] = useState(false)
  const [error, setError] = useState('')
  const [guardado, setGuardado] = useState<ReclamoGuardado | null>(null)

  async function manejarEnvio(evento: FormEvent<HTMLFormElement>) {
    evento.preventDefault()
    setError('')

    const cedula = idCard.trim()
    const nombre = fullName.trim()
    const contacto = contactInfo.trim()

    if (!cedula || !nombre || !contacto) {
      setError('Debe completar su identificación, nombre completo y datos de contacto para guardar el reclamo.')
      return
    }

    setCargando(true)
    try {
      const resultado = await guardarReclamo(sessionId, {
        idCard: cedula,
        fullName: nombre,
        contactInfo: contacto,
      })
      setGuardado(resultado)
      onGuardadoExitoso?.(resultado)
    } catch (razon) {
      setError(normalizarError(razon).mensaje)
    } finally {
      setCargando(false)
    }
  }

  return (
    <section className="analysis-card saved-session-actions panel" aria-label="Guardar progreso del reclamo">
      <div className="panel-heading">
        <h2>Guardar progreso del reclamo</h2>
        <p>
          Guarde esta sesión en el backend para poder consultarla o reanudarla más adelante.
          Proporcione sus datos personales para registrarla:
        </p>
      </div>

      <form onSubmit={manejarEnvio} className="guardar-reclamo-form" noValidate>
        <div className="form-grid">
          <label>
            Identificación / Cédula *
            <input
              type="text"
              name="idCard"
              aria-label="Identificación o cédula para guardar"
              placeholder="Ej: 8034867066076"
              value={idCard}
              onChange={(e) => setIdCard(e.target.value)}
              disabled={cargando}
              required
            />
          </label>

          <label>
            Nombre completo *
            <input
              type="text"
              name="fullName"
              aria-label="Nombre completo para guardar"
              placeholder="Ej: Rubén Godoy Nazario"
              value={fullName}
              onChange={(e) => setFullName(e.target.value)}
              disabled={cargando}
              required
            />
          </label>
        </div>

        <label className="full-field">
          Información de contacto (teléfono, correo o dirección) *
          <input
            type="text"
            name="contactInfo"
            aria-label="Información de contacto para guardar"
            placeholder="Ej: 0999999999 - usuario@ejemplo.com"
            value={contactInfo}
            onChange={(e) => setContactInfo(e.target.value)}
            disabled={cargando}
            required
          />
        </label>

        {error && (
          <p className="error-message" role="alert">
            {error}
          </p>
        )}

        {guardado && (
          <div className="saved-claim-success" role="status">
            <strong>✅ Reclamo guardado exitosamente.</strong>
            <p>
              ID del reclamo guardado: <code>{guardado.id}</code>
            </p>
            <p>
              Puede consultar y reanudar este reclamo en cualquier momento ingresando su ID y su cédula.
            </p>
          </div>
        )}

        <div className="form-actions" style={{ marginTop: '14px' }}>
          <button
            type="submit"
            className="btn btn-primary"
            disabled={cargando}
          >
            {cargando ? 'Guardando...' : 'Guardar reclamo'}
          </button>
        </div>
      </form>
    </section>
  )
}

