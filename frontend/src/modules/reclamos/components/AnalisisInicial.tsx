import { EstadoCarga } from './EstadoCarga'
import { MensajeError } from './MensajeError'
import type { MetricaSolicitud } from '../../../shared/metrics/metricas.types'
import type { Analisis, Estado } from '../types/reclamo.types'
import { PreguntasComplementarias } from './PreguntasComplementarias'
import { ResultadoFinal } from './ResultadoFinal'

type Propiedades = {
  errorSolicitud: string
  metrica: MetricaSolicitud | null
  estado: Estado
  analisis: Analisis | null
  respuestas: Record<string, string>
  mensaje: string
  cambiarRespuesta: (id: string, respuesta: string) => void
  completar: () => void
  aceptarCapa3: () => void
  evaluarCapa3: () => void
  seleccionarAccion: (id: string) => void
  enviarFormulario: () => void
  datosProcedimiento: Record<string, string>
  cambiarDato: (clave: string, valor: string) => void
}

export function AnalisisInicial({ estado, analisis, respuestas, mensaje, errorSolicitud, metrica, cambiarRespuesta, completar, evaluarCapa3, aceptarCapa3, seleccionarAccion, enviarFormulario, datosProcedimiento, cambiarDato }: Propiedades) {
  return (
    <section className="panel analysis-panel" aria-live="polite">
      <div className="panel-heading">
        <h2>
          Resultado del análisis
        </h2>
        <p>
          Hechos, información faltante y orientaciones generales
        </p>
      </div>
      <div className="demo-warning">
        <strong>Modo API · Asistencia con Inteligencia Artificial:</strong>{' '}
        el análisis se solicita al backend Spring Boot mediante modelos de IA sobre la LODC. Las orientaciones son de apoyo y no constituyen asesoría jurídica vinculante.
      </div>
      {estado === 'inicio' && <div className="analysis-placeholder">
        <div className="analysis-symbol">✦</div>
        <h3>
          Esperando análisis
        </h3>
        <p>
          Complete el formulario para iniciar el análisis con el backend.
        </p>
      </div>}
      {estado === 'cargando' && <EstadoCarga />}
      {estado === 'error' && <MensajeError mensaje={errorSolicitud} />}
      {analisis && (estado === 'preguntas' || estado === 'final') && <>
        <div className="analysis-card facts">
          <h3>
            Extracción del backend
          </h3>
          <p>
            {analisis.etapa === 'aclaracion'
              ? 'La capa 1 necesita precisar algunos datos del relato.'
              : analisis.etapa === 'capa2'
                ? 'La capa 1 terminó. Ahora el backend está recopilando los parámetros necesarios para el reclamo.'
                : 'El backend terminó la recopilación de información.'}
          </p>
          {analisis.etapa === 'completo' && <div className="stage-status">
            <strong>Capa 2 completada</strong>
            <span>El backend no encontró más parámetros pendientes y está listo para evaluar la viabilidad jurídica.</span>
          </div>}
          <ul>
            {analisis.hechos.map((hecho, i) => (
              <li key={i}>{hecho}</li>
            ))}
          </ul>
        </div>
        {analisis.incongruencias.length > 0 && <div className="analysis-card inconsistencies">
          <h3>Incongruencias detectadas</h3>
          <p>Revise estas diferencias entre los datos registrados y la descripción:</p>
          <ul>
            {analisis.incongruencias.map((incongruencia, i) => (
              <li key={i}>{incongruencia}</li>
            ))}
          </ul>
        </div>}
        {estado === 'preguntas' && analisis.preguntas.length > 0 &&
          <PreguntasComplementarias analisis={analisis} respuestas={respuestas} cambiarRespuesta={cambiarRespuesta} completar={completar} />}
        {estado === 'final' && <ResultadoFinal analisis={analisis} evaluar={evaluarCapa3} aceptar={aceptarCapa3} seleccionarAccion={seleccionarAccion} enviarFormulario={enviarFormulario} datosProcedimiento={datosProcedimiento} cambiarDato={cambiarDato} />}
        <div className="analysis-card guidance">
          <h3>
            Orientaciones generales
          </h3>
          <ul>
            {analisis.orientaciones.map((texto, i) => (
              <li key={i}>{texto}</li>
            ))}
          </ul>
        </div>
      </>}
      {metrica && (
        <p className="feedback">
          Tiempo total de la solicitud: {metrica.duracionMs.toFixed(0)} ms.
          {' '}Tokens de entrada, salida y total: {`${metrica.tokensEntrada ?? 'no disponibles'}, ${metrica.tokensSalida ?? 'no disponibles'}, ${metrica.tokensTotal ?? 'no disponibles'}.`}
        </p>
      )}
      {errorSolicitud && estado === 'final' && <MensajeError mensaje={errorSolicitud} />}
      {mensaje && <div className="feedback" role="status">{mensaje}</div>}
    </section>
  )
}
