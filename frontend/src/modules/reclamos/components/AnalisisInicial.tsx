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
  modoApi: boolean
}

export function AnalisisInicial({ estado, analisis, respuestas, mensaje, errorSolicitud, metrica, cambiarRespuesta, completar, modoApi }: Propiedades) {
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
        <strong>{modoApi ? 'Modo API:' : 'Demostración:'}</strong>{' '}
        {modoApi
          ? 'el análisis se solicita al backend Spring Boot mediante el modelo configurado en el servidor.'
          : 'este módulo no consulta Ollama ni el backend; las preguntas se generan mediante reglas locales.'}
      </div>
      {estado === 'inicio' && <div className="analysis-placeholder">
        <div className="analysis-symbol">✦</div>
        <h3>
          Esperando análisis
        </h3>
        <p>
          Complete el formulario para iniciar el análisis {modoApi ? 'con el backend' : 'simulado'}.
        </p>
      </div>}
      {estado === 'cargando' && <EstadoCarga />}
      {estado === 'error' && <MensajeError mensaje={errorSolicitud} />}
      {analisis && (estado === 'preguntas' || estado === 'final') && <>
        <div className="analysis-card facts">
          <h3>
            {modoApi ? 'Extracción del backend' : 'Hechos declarados'}
          </h3>
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
        {estado === 'preguntas' && (!modoApi || analisis.preguntas.length > 0) &&
          <PreguntasComplementarias analisis={analisis} respuestas={respuestas} cambiarRespuesta={cambiarRespuesta} completar={completar} modoApi={modoApi} />}
        {estado === 'final' && <ResultadoFinal analisis={analisis} />}
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
          {modoApi ? 'Tiempo total de la solicitud:' : 'Tiempo de procesamiento local:'} {metrica.duracionMs.toFixed(0)} ms.
          {' '}Tokens de entrada, salida y total: {modoApi
            ? `${metrica.tokensEntrada ?? 'no disponibles'}, ${metrica.tokensSalida ?? 'no disponibles'}, ${metrica.tokensTotal ?? 'no disponibles'}.`
            : 'No disponibles (simulación).'}
        </p>
      )}
      {mensaje && <div className="feedback" role="status">{mensaje}</div>}
    </section>
  )
}
