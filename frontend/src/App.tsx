import { useEffect, useRef, useState, type FormEvent } from 'react'
import './App.css'
import type { Analisis, Estado, Reclamo } from './modules/reclamos/types/reclamo.types'
import { inicial } from './modules/reclamos/constants/reclamo.constants'
import { validarReclamo } from './modules/reclamos/utils/validarReclamo'
import { FormularioReclamo } from './modules/reclamos/components/FormularioReclamo'
import { AnalisisInicial } from './modules/reclamos/components/AnalisisInicial'

import {
  aceptarCapa3ConApi,
  aclararConApi,
  analizarConApi,
  evaluarCapa3ConApi,
  responderConApi,
  seleccionarAccionConApi,
  enviarFormularioConApi,
  ejecutarCalculosConApi,
  generarDocumentoConApi,
  prepararVistaPreviaConApi,
  type VistaPreviaDocumento,
  type DatosDocumento,
} from './modules/reclamos/services/reclamosApi'
import { forgetSessions } from './modules/reclamos/services/sessionAccess'
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
  const [datosProcedimiento, setDatosProcedimiento] = useState<Record<string, string>>({})
  const [calculosCompletos, setCalculosCompletos] = useState(false)
  const [confirmarDocumento, setConfirmarDocumento] = useState(false)
  const [aprobarTexto, setAprobarTexto] = useState(false)
  const [documentoCompletado, setDocumentoCompletado] = useState(false)
  const [vistaPrevia, setVistaPrevia] = useState<VistaPreviaDocumento | null>(null)
  const [rucProveedor, setRucProveedor] = useState('')
  const [direccionProveedor, setDireccionProveedor] = useState('')
  const [errorSolicitud, setErrorSolicitud] = useState('')
  const [metrica, setMetrica] = useState<MetricaSolicitud | null>(null)
  const solicitudActiva = useRef<AbortController | null>(null)
  const sessionId = useRef<string | null>(null)

  useEffect(() => () => solicitudActiva.current?.abort(), [])

  function invalidarAnalisis() {
    forgetSessions()
    setDocumentoCompletado(false)
    setCalculosCompletos(false)
    setConfirmarDocumento(false)
    setAprobarTexto(false)
    setVistaPrevia(null)
    setDatosProcedimiento({})
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

  async function ejecutarAccion(operacion: 'seleccionar' | 'formulario', opcionId?: string) {
    const id = sessionId.current
    if (!id || !analisis || solicitudActiva.current) return
    if (operacion === 'seleccionar' && (analisis.estadoCapa3 !== 'WAITING_FOR_ACTION_SELECTION' || !opcionId)) return
    if (operacion === 'formulario') {
      if (analisis.estadoCapa3 !== 'WAITING_FOR_PROCEDURAL_INPUT') return
      const faltantes = analisis.camposFormulario.filter(campo => !(datosProcedimiento[campo.parameterName] ?? campo.currentValue ?? '').trim())
      if (faltantes.length) { setMensaje('Complete todos los campos solicitados antes de continuar.'); return }
    }
    setCalculosCompletos(false)
    setConfirmarDocumento(false)
    setAprobarTexto(false)
    setVistaPrevia(null)
    const controlador = new AbortController()
    solicitudActiva.current = controlador
    setEstado('cargando')
    setErrorSolicitud('')
    try {
      const resultado = operacion === 'seleccionar'
        ? await seleccionarAccionConApi(id, opcionId!, controlador.signal)
        : await enviarFormularioConApi(id, Object.fromEntries(analisis.camposFormulario.map(campo => [campo.parameterName, datosProcedimiento[campo.parameterName] ?? campo.currentValue ?? ''])), controlador.signal)
      if (controlador.signal.aborted || solicitudActiva.current !== controlador) return
      setAnalisis(resultado)
      setEstado('final')
      setMensaje(resultado.estadoCapa3 === 'DONE' ? 'Etapa jurídica completada.' : 'Revise la información solicitada para continuar.')
    } catch (error) {
      if (controlador.signal.aborted || solicitudActiva.current !== controlador) return
      setEstado('final')
      setErrorSolicitud(normalizarError(error).mensaje)
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

  async function calcularImportes() {
    const id = sessionId.current
    if (!id || analisis?.estadoCapa3 !== 'DONE' || solicitudActiva.current) {
      return
    }

    const controlador = new AbortController()
    solicitudActiva.current = controlador
    setEstado('cargando')
    setErrorSolicitud('')

    try {
      const calculos = await ejecutarCalculosConApi(id, controlador.signal)
      setAnalisis(actual => actual ? { ...actual, planEjecucion: calculos.executionPlan } : actual)
      if (controlador.signal.aborted) {
        return
      }

      setCalculosCompletos(true)
      setEstado('final')
      setMensaje('Cálculos finalizados. Revise los datos antes de generar el documento.')
    } catch (error) {
      if (!controlador.signal.aborted) {
        setEstado('final')
        setErrorSolicitud(normalizarError(error).mensaje)
      }
    } finally {
      if (solicitudActiva.current === controlador) {
        solicitudActiva.current = null
      }
    }
  }

  function datosDocumento(): DatosDocumento {
    return {
      fullName: form.fullName.trim(),
      idCard: form.idCard.trim(),
      email: form.email.trim(),
      phone: form.phone.trim(),
      providerCommercialName: form.provider.trim(),
      providerRuc: rucProveedor.trim(),
      providerAddress: direccionProveedor.trim(),
    }
  }

  async function prepararVistaPrevia() {
    const id = sessionId.current
    if (!id || !calculosCompletos || !confirmarDocumento || solicitudActiva.current) {
      return
    }

    const controlador = new AbortController()
    solicitudActiva.current = controlador
    setEstado('cargando')
    setErrorSolicitud('')
    setVistaPrevia(null)
    setAprobarTexto(false)

    try {
      const datos = datosDocumento()
      if (!datos.fullName || !/^[A-Za-z0-9-]{5,20}$/.test(datos.idCard)
          || !/^[^\s@]+@[^\s@]+[.][^\s@]+$/.test(datos.email)
          || !/^[+0-9() -]{7,25}$/.test(datos.phone) || !datos.providerCommercialName
          || (datos.providerRuc && !/^[0-9]{13}$/.test(datos.providerRuc))) {
        setErrorSolicitud('Complete nombre, identificación, correo, teléfono y proveedor. Revise el formato del RUC opcional.')
        return
      }
      const vista = await prepararVistaPreviaConApi(id, datos, controlador.signal)
      if (!controlador.signal.aborted) {
        setVistaPrevia(vista)
        setMensaje('Revise los hechos y pretensiones antes de aprobar el documento.')
      }
    } catch (error) {
      if (!controlador.signal.aborted) {
        setErrorSolicitud(normalizarError(error).mensaje)
      }
    } finally {
      if (solicitudActiva.current === controlador) {
        solicitudActiva.current = null
        setEstado('final')
      }
    }
  }

  async function descargarDocumento() {
    const id = sessionId.current
    if (!id || !vistaPrevia || !aprobarTexto || solicitudActiva.current) {
      return
    }

    const controlador = new AbortController()
    solicitudActiva.current = controlador
    setEstado('cargando')
    setErrorSolicitud('')

    try {
      const archivo = await generarDocumentoConApi(
        id,
        datosDocumento(),
        vistaPrevia.previewId,
        controlador.signal,
      )
      if (controlador.signal.aborted) {
        return
      }

      const url = URL.createObjectURL(archivo)
      const enlace = document.createElement('a')
      enlace.href = url
      enlace.download = `Reclamo_LODC_${id.slice(0, 8)}.docx`
      document.body.appendChild(enlace)
      enlace.click()
      enlace.remove()
      setTimeout(() => URL.revokeObjectURL(url), 1000)
      setDocumentoCompletado(true)
      setVistaPrevia(null)
      setMensaje('Documento aprobado y descargado. Verifique su contenido antes de presentarlo.')
    } catch (error) {
      if (!controlador.signal.aborted) {
        setErrorSolicitud(normalizarError(error).mensaje)
      }
    } finally {
      if (solicitudActiva.current === controlador) {
        solicitudActiva.current = null
        setEstado('final')
      }
    }
  }

  function reiniciar() {
    forgetSessions()
    setDocumentoCompletado(false)
    solicitudActiva.current?.abort()
    solicitudActiva.current = null
    setErrorSolicitud('')
    setMetrica(null)
    setForm(inicial)
    setErrores([])
    setEstado('inicio')
    setAnalisis(null)
    setRespuestas({})
    setDatosProcedimiento({})
    setCalculosCompletos(false)
    setConfirmarDocumento(false)
    setAprobarTexto(false)
    setVistaPrevia(null)
    setRucProveedor('')
    setDireccionProveedor('')
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
          <AnalisisInicial errorSolicitud={errorSolicitud} metrica={metrica} estado={estado} analisis={analisis} respuestas={respuestas} mensaje={mensaje} cambiarRespuesta={cambiarRespuesta} completar={completar} evaluarCapa3={evaluarCapa3} aceptarCapa3={aceptarCapa3} seleccionarAccion={(id) => ejecutarAccion('seleccionar', id)} enviarFormulario={() => ejecutarAccion('formulario')} datosProcedimiento={datosProcedimiento} cambiarDato={(clave, valor) => setDatosProcedimiento(actual => ({ ...actual, [clave]: valor }))} />
        </div>
        {documentoCompletado && <p role="status">Sesión COMPLETED. Documento generado y aprobado.</p>}
        {estado === 'final' && analisis?.estadoCapa3 === 'DONE' && !documentoCompletado && (
          <section className="analysis-card" aria-label="Generación del documento">
            <h2>Documento del reclamo</h2>
            <p>
              Primero ejecute los cálculos. Después confirme los datos para
              generar un archivo Word que deberá revisar antes de utilizar.
            </p>
            {!calculosCompletos ? (
              <button
                type="button"
                className="btn btn-primary"
                onClick={calcularImportes}
              >
                Ejecutar cálculos
              </button>
            ) : (
              <div className="stage-status">
                <p role="status">Cálculos completados correctamente.</p>
                {(['fullName', 'idCard', 'email', 'phone', 'provider'] as const).map(campo => (
                  <label key={campo}>
                    {{ fullName: 'Nombre completo', idCard: 'Identificación', email: 'Correo electrónico', phone: 'Teléfono', provider: 'Nombre del proveedor' }[campo]}
                    <input required maxLength={campo === 'email' ? 254 : 150}
                      type={campo === 'email' ? 'email' : 'text'} value={form[campo]}
                      onChange={evento => {
                        setForm(actual => ({ ...actual, [campo]: evento.target.value }))
                        setVistaPrevia(null)
                        setAprobarTexto(false)
                      }} />
                  </label>
                ))}
                <label>
                  RUC del proveedor
                  <input
                    type="text"
                    value={rucProveedor}
                    onChange={evento => {
                        setRucProveedor(evento.target.value)
                        setVistaPrevia(null)
                        setAprobarTexto(false)
                      }}
                  />
                </label>
                <label>
                  Dirección del proveedor
                  <input
                    type="text"
                    value={direccionProveedor}
                    onChange={evento => {
                        setDireccionProveedor(evento.target.value)
                        setVistaPrevia(null)
                        setAprobarTexto(false)
                      }}
                  />
                </label>
                <label>
                  <input
                    type="checkbox"
                    checked={confirmarDocumento}
                    onChange={evento => setConfirmarDocumento(evento.target.checked)}
                  />
                  Confirmo que deseo generar el borrador con los datos proporcionados.
                </label>
                <button
                  type="button"
                  className="btn btn-primary"
                  disabled={!confirmarDocumento}
                  onClick={prepararVistaPrevia}
                >
                  Preparar vista previa
                </button>
                {vistaPrevia && (
                  <div className="document-preview" aria-label="Vista previa del reclamo">
                    <h3>Documento completo para revisión</h3>
                    <pre style={{ whiteSpace: 'pre-wrap' }}>{vistaPrevia.documentoCompleto}</pre>
                    <h3>Hechos redactados</h3>
                    <p style={{ whiteSpace: 'pre-wrap' }}>{vistaPrevia.hechosRedactados}</p>
                    <h3>Pretensiones redactadas</h3>
                    <p style={{ whiteSpace: 'pre-wrap' }}>{vistaPrevia.pretensionesRedactadas}</p>
                    <label>
                      <input
                        type="checkbox"
                        checked={aprobarTexto}
                        onChange={evento => setAprobarTexto(evento.target.checked)}
                      />
                      He leído y apruebo el documento completo mostrado, sus datos, fuentes y pretensiones.
                    </label>
                    <button
                      type="button"
                      className="btn btn-primary"
                      disabled={!aprobarTexto}
                      onClick={descargarDocumento}
                    >
                      Aprobar y descargar Word
                    </button>
                  </div>
                )}
              </div>
            )}
          </section>
        )}
        <footer className="page-footer">UNIANDES · Proyecto académico · Sesión temporal de 60 minutos; orientación con IA, sin asesoría legal</footer>
      </main>
    </div>
  )
}

export default App
