import { useEffect, useRef, useState, type FormEvent } from 'react'
import './App.css'
import type { Analisis, Estado, Reclamo, Tipo } from './modules/reclamos/types/reclamo.types'
import { inicial } from './modules/reclamos/constants/reclamo.constants'
import { validarReclamo } from './modules/reclamos/utils/validarReclamo'
import { incorporarRespuestas } from './modules/reclamos/utils/simularAnalisis'
import { FormularioReclamo } from './modules/reclamos/components/FormularioReclamo'
import { AnalisisInicial } from './modules/reclamos/components/AnalisisInicial'

import { analizarReclamo } from './modules/reclamos/services/reclamosService'
import { normalizarError } from './shared/http/httpErrors'
import { obtenerMetricas } from './shared/metrics/metricasService'
import type { MetricaSolicitud } from './shared/metrics/metricas.types'

function App() {
  const [form, setForm] = useState<Reclamo>(inicial)
  const [errores, setErrores] = useState<string[]>([])
  const [estado, setEstado] = useState<Estado>('inicio')
  const [analisis, setAnalisis] = useState<Analisis | null>(null)
  const [respuestas, setRespuestas] = useState<Record<string, string>>({})
  const [mensaje, setMensaje] = useState('')
  const [errorSolicitud, setErrorSolicitud] = useState('')
  const [metrica, setMetrica] = useState<MetricaSolicitud | null>(null)
  const solicitudActiva = useRef<AbortController | null>(null)

  useEffect(() => () => solicitudActiva.current?.abort(), [])

  function invalidarAnalisis() {
    setAnalisis(null)
    setRespuestas({})
    setMetrica(null)
    setErrorSolicitud('')
    setEstado('inicio')
  }

  function cambiar<K extends keyof Reclamo>(campo: K, valor: Reclamo[K]) {
    if (solicitudActiva.current) return
    invalidarAnalisis()
    setForm(actual => ({ ...actual, [campo]: valor }))
    setMensaje('')
  }

  function cambiarTipo(tipo: Tipo) {
    if (solicitudActiva.current) return
    invalidarAnalisis()
    setForm(actual => ({ ...actual, tipo, solucion: '', otraSolucion: '' }))
    setMensaje('')
  }

  async function analizar(evento: FormEvent<HTMLFormElement>) {
    evento.preventDefault()
    // La referencia se actualiza antes del siguiente render: también bloquea dos envíos en el mismo instante.
    if (solicitudActiva.current) return
    const fallos = validarReclamo(form)
    setErrores(fallos)
    if (fallos.length) return
    const controlador = new AbortController()
    solicitudActiva.current = controlador
    setEstado('cargando')
    setErrorSolicitud('')
    setMetrica(null)
    setMensaje('Preparando análisis simulado. No se está llamando al backend.')
    try {
      const resultado = await analizarReclamo(form, controlador.signal)
      if (controlador.signal.aborted || solicitudActiva.current !== controlador) return
      setAnalisis(resultado)
      setRespuestas({})
      setEstado('preguntas')
      setMensaje('Análisis simulado listo. Revise las preguntas complementarias.')
    } catch (error) {
      if (controlador.signal.aborted || solicitudActiva.current !== controlador) return
      setEstado('error')
      setErrorSolicitud(normalizarError(error).mensaje)
      setMensaje('Sus datos se conservan. Puede volver a analizar el reclamo.')
    } finally {
      if (solicitudActiva.current === controlador) {
        solicitudActiva.current = null
        setMetrica(obtenerMetricas().at(-1) ?? null)
      }
    }
  }

  function completar() {
    if (!analisis || solicitudActiva.current) return
    setAnalisis(incorporarRespuestas(analisis, respuestas))
    setEstado('final')
    setMensaje('Resultado simulado actualizado. Las preguntas sin respuesta no impiden continuar.')
  }

  function cambiarRespuesta(id: string, respuesta: string) {
    setRespuestas(actual => ({ ...actual, [id]: respuesta }))
  }

  function reiniciar() {
    solicitudActiva.current?.abort()
    solicitudActiva.current = null
    setErrorSolicitud('')
    setMetrica(null)
    setForm(inicial)
    setErrores([])
    setEstado('inicio')
    setAnalisis(null)
    setRespuestas({})
    setMensaje('')
  }

  const paso = estado === 'final' ? 4 : estado === 'preguntas' ? 3 : estado === 'cargando' ? 2 : 1

  return (
    <div className="app-layout">
      <aside className="sidebar">
        <div className="brand">
          <span className="brand-icon">
            ⚖
          </span>
          <span>
            Reclamos IA
          </span>
        </div>
        <nav aria-label="Navegación principal">
          <div className="nav-active">▣ &nbsp; Nuevo reclamo</div>
          <div className="nav-disabled">▤ &nbsp; Mis reclamos (pendiente)</div>
        </nav>
        <p className="sidebar-footer">
          Proyecto académico
          <strong>
            UNIANDES
          </strong>
        </p>
      </aside>
      <main className="main-content">
        <header className="page-header">
          <div>
            <h1>
              Generador Inteligente de Reclamos
            </h1>
            <p>
              Asistencia para reclamos de consumo en Ecuador
            </p>
          </div>
          <span className="environment" title="Modo API pendiente de integración con Spring Boot">
            Modo demostración · Sin IA real
          </span>
        </header>
        <section className="steps" aria-label="Etapas del proceso">
          {['Registrar reclamo', 'Análisis inicial', 'Información faltante', 'Resultado final'].map((nombre, indice) => (
            <div className={`step ${paso === indice + 1 ? 'step-active' : ''}`} key={nombre} aria-current={paso === indice + 1 ? 'step' : undefined}>
              <span className="step-number">{indice + 1}</span>
              <span>{nombre}</span>
            </div>
          ))}
        </section>
        <div className="content-grid">
          <FormularioReclamo form={form} errores={errores} estado={estado} cambiar={cambiar} cambiarTipo={cambiarTipo} analizar={analizar} reiniciar={reiniciar} />
          <AnalisisInicial errorSolicitud={errorSolicitud} metrica={metrica} estado={estado} analisis={analisis} respuestas={respuestas} mensaje={mensaje} cambiarRespuesta={cambiarRespuesta} completar={completar} />
        </div>
        <footer className="page-footer">UNIANDES · Proyecto académico · Prototipo sin almacenamiento ni asesoría legal</footer>
      </main>
    </div>
  )
}

export default App
