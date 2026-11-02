import { useEffect, useRef, useState, type FormEvent } from 'react'
import './App.css'
import type { Analisis, Estado, Reclamo } from './modules/reclamos/types/reclamo.types'
import { inicial } from './modules/reclamos/constants/reclamo.constants'
import { validarReclamo } from './modules/reclamos/utils/validarReclamo'
import { FormularioReclamo } from './modules/reclamos/components/FormularioReclamo'
import { AnalisisInicial } from './modules/reclamos/components/AnalisisInicial'

import { aceptarCapa3ConApi, aclararConApi, analizarConApi, evaluarCapa3ConApi, responderConApi } from './modules/reclamos/services/reclamosApi'
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
  const sessionId = useRef<string | null>(null)

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
    setMensaje('Enviando reclamo al backend para análisis.')
    try {
      const resultado = await analizarConApi(form, controlador.signal)
      if (controlador.signal.aborted || solicitudActiva.current !== controlador) return
      setAnalisis(resultado.analisis)
      setRespuestas({})
      if (resultado.analisis.preguntas.length) {
        setEstado('preguntas')
        setMensaje('Análisis del backend listo. Revise la información extraída.')
      } else if (resultado.analisis.etapa === 'completo' || resultado.analisis.etapa === 'capa3') {
        setEstado('final')
        setMensaje(resultado.analisis.etapa === 'capa3'
          ? 'La evaluación jurídica está lista para revisión.'
          : 'La capa 2 terminó. Revise la información recopilada.')
      } else {
        setEstado('error')
        const pendientes = resultado.analisis.parametrosPendientes
        setErrorSolicitud(
          pendientes.length > 0
            ? `El backend marcó la capa 2 como ${resultado.analisis.estadoCapa2 ?? 'incompleta'}, pero aún faltan parámetros: ${pendientes.join(', ')}.`
            : 'El backend no devolvió preguntas ni indicó que la capa 2 haya terminado.',
        )
        setMensaje('La sesión quedó detenida porque el estado recibido no permite continuar de forma segura.')
      }
      sessionId.current = resultado.sessionId
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

  async function aceptarCapa3() {
    const idSesion = sessionId.current
    if (
      !idSesion ||
      !analisis ||
      analisis.estadoCapa2 !== 'DONE' ||
      analisis.estadoCapa3 !== 'WAITING_FOR_DECISION' ||
      solicitudActiva.current
    ) return
    const controlador = new AbortController()
    solicitudActiva.current = controlador
    setEstado('cargando')
    try {
      const resultado = await aceptarCapa3ConApi(idSesion, controlador.signal)
      if (controlador.signal.aborted || solicitudActiva.current !== controlador) return
      setAnalisis(resultado)
      setEstado('final')
      setMensaje(resultado.rutaAccion ? 'Ruta de acción generada.' : 'La evaluación fue aceptada.')
    } catch (error) {
      if (controlador.signal.aborted || solicitudActiva.current !== controlador) return
      setEstado('error')
      setErrorSolicitud(normalizarError(error).mensaje)
    } finally {
      if (solicitudActiva.current === controlador) {
        solicitudActiva.current = null
        setMetrica(obtenerMetricas().at(-1) ?? null)
      }
    }
  }

  async function evaluarCapa3() {
    const idSesion = sessionId.current
    if (
      !idSesion ||
      !analisis ||
      analisis.etapa !== 'completo' ||
      analisis.estadoCapa2 !== 'DONE' ||
      analisis.estadoCapa3 !== null ||
      solicitudActiva.current
    ) return
    const controlador = new AbortController()
    solicitudActiva.current = controlador
    setEstado('cargando')
    try {
      const resultado = await evaluarCapa3ConApi(idSesion, controlador.signal)
      if (controlador.signal.aborted || solicitudActiva.current !== controlador) return
      setAnalisis(resultado)
      setEstado('final')
      setMensaje('La evaluación jurídica está lista para revisión.')
    } catch (error) {
      if (controlador.signal.aborted || solicitudActiva.current !== controlador) return
      setEstado('error')
      setErrorSolicitud(normalizarError(error).mensaje)
    } finally {
      if (solicitudActiva.current === controlador) {
        solicitudActiva.current = null
        setMetrica(obtenerMetricas().at(-1) ?? null)
      }
    }
  }

  async function completar() {
    if (!analisis || solicitudActiva.current) return
    const pregunta = analisis.preguntas[0]
    const esAclaracion = pregunta?.id.startsWith('aclaracion-') ?? false
    const respuestasAclaracion = esAclaracion
      ? analisis.preguntas
        .map(actual => respuestas[actual.id]?.trim())
        .filter((texto): texto is string => Boolean(texto))
      : []
    const respuesta = esAclaracion
      ? analisis.preguntas
        .map((actual, indice) => {
          const texto = respuestas[actual.id]?.trim()
          return texto ? `${actual.texto} Respuesta: ${texto}` : `Pregunta ${indice + 1} sin respuesta.`
        })
        .join('\n')
      : pregunta ? respuestas[pregunta.id]?.trim() : ''
    const idSesion = sessionId.current
    if (esAclaracion && respuestasAclaracion.length === 0) {
      setMensaje('Escriba al menos una aclaración para continuar.')
      return
    }
    if (!respuesta || !idSesion) {
      setMensaje('Responda la pregunta actual para continuar con el flujo del backend.')
      return
    }
    const controlador = new AbortController()
    solicitudActiva.current = controlador
    setEstado('cargando')
    try {
      const resultado = esAclaracion
        ? await aclararConApi(idSesion, respuesta, controlador.signal)
        : await responderConApi(idSesion, respuesta, controlador.signal)
      if (controlador.signal.aborted || solicitudActiva.current !== controlador) return
      setAnalisis(resultado)
      setRespuestas({})
      if (resultado.preguntas.length) {
        setEstado('preguntas')
        setMensaje('Información guardada. Revise el siguiente dato solicitado.')
      } else if (resultado.etapa === 'completo' || resultado.etapa === 'capa3') {
        setEstado('final')
        setMensaje(resultado.etapa === 'capa3'
          ? 'La evaluación jurídica está lista para revisión.'
          : 'La capa 2 terminó. Revise la información recopilada.')
      } else {
        setEstado('error')
        const pendientes = resultado.parametrosPendientes
        setErrorSolicitud(
          pendientes.length > 0
            ? `El backend marcó la capa 2 como ${resultado.estadoCapa2 ?? 'incompleta'}, pero aún faltan parámetros: ${pendientes.join(', ')}.`
            : 'El backend no devolvió una siguiente pregunta ni indicó que la capa 2 haya terminado.',
        )
        setMensaje('La sesión quedó detenida porque el estado recibido no permite continuar de forma segura.')
      }
    } catch (error) {
      if (controlador.signal.aborted || solicitudActiva.current !== controlador) return
      setEstado('error')
      setErrorSolicitud(normalizarError(error).mensaje)
      setMensaje('Sus datos se conservan. Puede volver a enviar la respuesta.')
    } finally {
      if (solicitudActiva.current === controlador) {
        solicitudActiva.current = null
        setMetrica(obtenerMetricas().at(-1) ?? null)
      }
    }
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
    sessionId.current = null
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
          <span className="environment" title="Análisis conectado al backend Spring Boot">
            Modo API · Backend conectado
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
          <FormularioReclamo form={form} errores={errores} estado={estado} cambiar={cambiar} analizar={analizar} reiniciar={reiniciar} />
          <AnalisisInicial errorSolicitud={errorSolicitud} metrica={metrica} estado={estado} analisis={analisis} respuestas={respuestas} mensaje={mensaje} cambiarRespuesta={cambiarRespuesta} completar={completar} evaluarCapa3={evaluarCapa3} aceptarCapa3={aceptarCapa3} />
        </div>
        <footer className="page-footer">UNIANDES · Proyecto académico · Prototipo sin almacenamiento ni asesoría legal</footer>
      </main>
    </div>
  )
}

export default App
