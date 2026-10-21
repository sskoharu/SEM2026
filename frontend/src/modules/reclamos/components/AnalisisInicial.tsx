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
}

export function AnalisisInicial({ estado, analisis, respuestas, mensaje, errorSolicitud, metrica, cambiarRespuesta, completar }: Propiedades) {
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
        <strong>
          Demostración:
        </strong> este módulo no consulta Ollama ni el backend. Las preguntas se generan mediante reglas locales. El modo API está pendiente de integración.</div>
      {estado === 'inicio' && <div className="analysis-placeholder">
        <div className="analysis-symbol">✦</div>
        <h3>
          Esperando análisis
        </h3>
        <p>
          Complete el formulario para probar el flujo de análisis simulado.
        </p>
      </div>}
      {estado === 'cargando' && <EstadoCarga />}
      {estado === 'error' && <MensajeError mensaje={errorSolicitud} />}
      {analisis && (estado === 'preguntas' || estado === 'final') && <>
        <div className="analysis-card facts">
          <h3>
            Hechos declarados
          </h3>
          <ul>
            {analisis.hechos.map((hecho, i) => (
              <li key={i}>{hecho}</li>
            ))}
          </ul>
        </div>
        {estado === 'preguntas' && <PreguntasComplementarias analisis={analisis} respuestas={respuestas} cambiarRespuesta={cambiarRespuesta} completar={completar} />}
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
          Tiempo de procesamiento local: {metrica.duracionMs.toFixed(0)} ms.
          {' '}Tokens de entrada, salida y total: No disponibles (simulación).
        </p>
      )}
      {mensaje && <div className="feedback" role="status">{mensaje}</div>}
    </section>
  )
}
