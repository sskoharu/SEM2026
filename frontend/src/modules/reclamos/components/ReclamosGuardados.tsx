import { useCallback, useEffect, useRef, useState, type FormEvent } from 'react'
import {
  listarReclamosGuardados,
  obtenerIdCardLocal,
  obtenerReclamoGuardado,
  reanudarReclamo,
  type ReclamoGuardado,
} from '../services/savedClaimsService'
import type { Analisis } from '../types/reclamo.types'
import { normalizarError } from '../../../shared/http/httpErrors'

type Props = {
  onReanudar: (sessionId: string, analisis: Analisis) => void
  refreshKey?: number
}

export function ReclamosGuardados({ onReanudar, refreshKey = 0 }: Props) {
  const [tab, setTab] = useState<'historial' | 'directo'>('historial')
  const [idCard, setIdCard] = useState(obtenerIdCardLocal)
  const [claimIdDirecto, setClaimIdDirecto] = useState('')
  const [reclamos, setReclamos] = useState<ReclamoGuardado[]>([])
  const [reclamoDetalle, setReclamoDetalle] = useState<ReclamoGuardado | null>(null)
  const [cargando, setCargando] = useState(false)
  const [error, setError] = useState('')
  const [mensaje, setMensaje] = useState('')
  const solicitud = useRef<AbortController | null>(null)

  useEffect(() => () => solicitud.current?.abort(), [])

  // Consultar historial por cédula (GET /api/v1/claims/saved?idCard=...&idcard=...)
  const consultarHistorial = useCallback(async () => {
    const identificacion = idCard.trim()
    if (!identificacion) {
      setError('Ingrese su identificación para consultar sus reclamos guardados.')
      setReclamos([])
      return
    }
    solicitud.current?.abort()
    const controlador = new AbortController()
    solicitud.current = controlador
    setCargando(true)
    setError('')
    setMensaje('')
    try {
      const resultado = await listarReclamosGuardados(identificacion, controlador.signal)
      setReclamos(resultado)
      setMensaje(
        resultado.length === 0
          ? `No existen reclamos guardados para la identificación ${identificacion}.`
          : `Se encontraron ${resultado.length} reclamo(s) guardado(s).`,
      )
    } catch (razon) {
      if (!controlador.signal.aborted) {
        setError(normalizarError(razon).mensaje)
        setReclamos([])
      }
    } finally {
      if (solicitud.current === controlador) {
        solicitud.current = null
        setCargando(false)
      }
    }
  }, [idCard])

  // Consultar detalle por ID y cédula (GET /api/v1/claims/saved/{id}?idCard=...)
  async function consultarDetallePorId(idParaConsultar?: string, cedulaParaConsultar?: string) {
    const claimId = (idParaConsultar ?? claimIdDirecto).trim()
    const identificacion = (cedulaParaConsultar ?? idCard).trim()

    if (!claimId || !identificacion) {
      setError('Debe ingresar tanto el ID del reclamo guardado como su identificación (idCard).')
      return
    }

    solicitud.current?.abort()
    const controlador = new AbortController()
    solicitud.current = controlador
    setCargando(true)
    setError('')
    setMensaje('')

    try {
      const resultado = await obtenerReclamoGuardado(claimId, identificacion, controlador.signal)
      setReclamoDetalle(resultado)
      setMensaje('Detalle del reclamo obtenido exitosamente.')
    } catch (razon) {
      if (!controlador.signal.aborted) {
        setError(normalizarError(razon).mensaje)
      }
    } finally {
      if (solicitud.current === controlador) {
        solicitud.current = null
        setCargando(false)
      }
    }
  }

  // Reanudar directamente por ID de reclamo y cédula (POST /api/v1/claims/saved/{id}/resume)
  async function ejecutarReanudacion(idParaReanudar?: string, cedulaParaReanudar?: string) {
    const claimId = (idParaReanudar ?? claimIdDirecto).trim()
    const identificacion = (cedulaParaReanudar ?? idCard).trim()

    if (!claimId || !identificacion) {
      setError('Debe ingresar tanto el ID del reclamo guardado como su identificación (idCard).')
      return
    }

    solicitud.current?.abort()
    const controlador = new AbortController()
    solicitud.current = controlador
    setCargando(true)
    setError('')
    setMensaje('')

    try {
      const resultado = await reanudarReclamo(claimId, identificacion, controlador.signal)
      onReanudar(resultado.sessionId, resultado.analisis)
      setMensaje('Reclamo consultado y reanudado exitosamente.')
    } catch (razon) {
      if (!controlador.signal.aborted) {
        setError(normalizarError(razon).mensaje)
      }
    } finally {
      if (solicitud.current === controlador) {
        solicitud.current = null
        setCargando(false)
      }
    }
  }

  function reanudarPorId(evento: FormEvent<HTMLFormElement>) {
    evento.preventDefault()
    void ejecutarReanudacion()
  }

  async function reanudarDesdeLista(reclamo: ReclamoGuardado) {
    const claimId = reclamo.id || reclamo.sessionId || ''
    await ejecutarReanudacion(claimId, idCard)
  }

  async function verDetalleDesdeLista(reclamo: ReclamoGuardado) {
    const claimId = reclamo.id || reclamo.sessionId || ''
    await consultarDetallePorId(claimId, idCard)
  }

  // Si cambia refreshKey (ej. se guardó un reclamo nuevo), refrescar historial
  const previoRefreshKey = useRef(refreshKey)
  useEffect(() => {
    if (previoRefreshKey.current !== refreshKey) {
      previoRefreshKey.current = refreshKey
      if (tab === 'historial' && idCard.trim()) {
        const temporizador = setTimeout(() => {
          void consultarHistorial()
        }, 0)
        return () => clearTimeout(temporizador)
      }
    }
  }, [refreshKey, tab, consultarHistorial, idCard])

  return (
    <section className="saved-claims panel" aria-label="Mis reclamos guardados" id="seccion-mis-reclamos">
      <div className="panel-heading">
        <h2>Consultar y reanudar reclamos guardados</h2>
        <p>
          Consulte su lista con <code>GET /api/v1/claims/saved?idCard=...</code>, consulte un reclamo específico con{' '}
          <code>GET /api/v1/claims/saved/{'{id}'}?idCard=...</code> o reanúdelo con{' '}
          <code>POST /api/v1/claims/saved/{'{id}'}/resume</code>.
        </p>
      </div>

      <div className="saved-claims-tabs" role="tablist" aria-label="Opciones de consulta">
        <button
          type="button"
          role="tab"
          aria-selected={tab === 'historial'}
          className={`saved-claims-tab ${tab === 'historial' ? 'active' : ''}`}
          onClick={() => { setTab('historial'); setError(''); setMensaje(''); setReclamoDetalle(null) }}
        >
          Consultar por cédula (Historial)
        </button>
        <button
          type="button"
          role="tab"
          aria-selected={tab === 'directo'}
          className={`saved-claims-tab ${tab === 'directo' ? 'active' : ''}`}
          onClick={() => { setTab('directo'); setError(''); setMensaje('') }}
        >
          Consultar por ID de reclamo (Detalle / Reanudar)
        </button>
      </div>

      {tab === 'historial' ? (
        <form
          className="saved-claims-search"
          onSubmit={(evento) => {
            evento.preventDefault()
            void consultarHistorial()
          }}
        >
          <label className="full-field">
            Identificación / Cédula (idCard) *
            <input
              aria-label="Identificación para consultar reclamos"
              value={idCard}
              onChange={(evento) => setIdCard(evento.target.value)}
              placeholder="Ingrese su cédula (ej: 1315779726)"
              inputMode="numeric"
              required
            />
          </label>
          <div className="form-actions" style={{ marginTop: '10px' }}>
            <button type="submit" className="btn btn-primary" disabled={cargando}>
              {cargando ? 'Consultando en backend...' : 'Consultar reclamos guardados'}
            </button>
          </div>
        </form>
      ) : (
        <form className="saved-claims-direct-form" onSubmit={reanudarPorId}>
          <div className="form-grid">
            <label>
              ID del reclamo guardado (id) *
              <input
                aria-label="ID del reclamo guardado"
                value={claimIdDirecto}
                onChange={(evento) => setClaimIdDirecto(evento.target.value)}
                placeholder="Ej: 45e3481c-2df9-4afa-98c0-ea9abcad111e"
                required
              />
            </label>

            <label>
              Identificación / Cédula (idCard) *
              <input
                aria-label="Identificación para reanudar"
                value={idCard}
                onChange={(evento) => setIdCard(evento.target.value)}
                placeholder="Ej: 1315779726"
                inputMode="numeric"
                required
              />
            </label>
          </div>

          <div className="form-actions" style={{ marginTop: '14px', display: 'flex', gap: '10px' }}>
            <button
              type="button"
              className="btn btn-secondary"
              disabled={cargando}
              onClick={() => void consultarDetallePorId()}
            >
              {cargando ? 'Consultando...' : 'Consultar detalle (GET)'}
            </button>
            <button type="submit" className="btn btn-primary" disabled={cargando}>
              {cargando ? 'Reanudando...' : 'Consultar y Reanudar (POST)'}
            </button>
          </div>
        </form>
      )}

      {error && (
        <p className="error-message" role="alert" style={{ marginTop: '12px' }}>
          {error}
        </p>
      )}
      {mensaje && (
        <p role="status" style={{ marginTop: '12px', color: '#1758a8', fontWeight: 600 }}>
          {mensaje}
        </p>
      )}

      {reclamoDetalle && (
        <article className="saved-claim-card" style={{ marginTop: '16px', background: '#f8fbff', borderColor: '#b6d2f5' }}>
          <div className="saved-claim-card-header">
            <span className="saved-claim-title">
              📋 {reclamoDetalle.fullName || 'Detalle del reclamo'}
            </span>
            <span className="saved-claim-status">{reclamoDetalle.status || 'Guardado'}</span>
          </div>
          <div className="saved-claim-meta">
            <span>
              <strong>ID:</strong> <code>{reclamoDetalle.id}</code>
            </span>
            {reclamoDetalle.idCard && (
              <span>
                <strong>Cédula:</strong> {reclamoDetalle.idCard}
              </span>
            )}
            {reclamoDetalle.contactInfo && (
              <span>
                <strong>Contacto:</strong> {reclamoDetalle.contactInfo}
              </span>
            )}
            {reclamoDetalle.createdAt && (
              <span>
                <strong>Fecha creación:</strong> {reclamoDetalle.createdAt}
              </span>
            )}
            {reclamoDetalle.updatedAt && (
              <span>
                <strong>Última actualización:</strong> {reclamoDetalle.updatedAt}
              </span>
            )}
          </div>
          <div style={{ marginTop: '10px' }}>
            <button
              type="button"
              className="btn btn-primary"
              disabled={cargando}
              onClick={() => void ejecutarReanudacion(reclamoDetalle.id, reclamoDetalle.idCard ?? idCard)}
            >
              Reanudar este reclamo
            </button>
          </div>
        </article>
      )}

      {tab === 'historial' && reclamos.length > 0 && (
        <div className="saved-claims-grid" style={{ marginTop: '16px' }}>
          {reclamos.map((reclamo) => (
            <article key={reclamo.id} className="saved-claim-card">
              <div className="saved-claim-card-header">
                <span className="saved-claim-title">{reclamo.fullName || 'Reclamo guardado'}</span>
                <span className="saved-claim-status">{reclamo.status || 'Guardado'}</span>
              </div>
              <div className="saved-claim-meta">
                <span>
                  <strong>ID:</strong> <code>{reclamo.id}</code>
                </span>
                {reclamo.idCard && (
                  <span>
                    <strong>Cédula:</strong> {reclamo.idCard}
                  </span>
                )}
                {reclamo.contactInfo && (
                  <span>
                    <strong>Contacto:</strong> {reclamo.contactInfo}
                  </span>
                )}
                {reclamo.createdAt && (
                  <span>
                    <strong>Fecha:</strong> {reclamo.createdAt}
                  </span>
                )}
              </div>
              <div style={{ marginTop: '8px', display: 'flex', gap: '8px' }}>
                <button
                  type="button"
                  className="btn btn-outline"
                  disabled={cargando}
                  onClick={() => void verDetalleDesdeLista(reclamo)}
                >
                  Ver detalle
                </button>
                <button
                  type="button"
                  className="btn btn-secondary"
                  disabled={cargando}
                  onClick={() => void reanudarDesdeLista(reclamo)}
                >
                  Reanudar
                </button>
              </div>
            </article>
          ))}
        </div>
      )}
    </section>
  )
}
