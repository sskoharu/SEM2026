import type { Analisis } from '../types/reclamo.types'

type Propiedades = {
  analisis: Analisis
  respuestas: Record<string, string>
  cambiarRespuesta: (id: string, respuesta: string) => void
  completar: () => void
}

export function PreguntasComplementarias({ analisis, respuestas, cambiarRespuesta, completar }: Propiedades) {
  return (
    <div className="analysis-card questions">
      <h3>
        Información complementaria
      </h3>
      <p>
        Responda lo que conozca. Puede dejar preguntas sin respuesta.
      </p>
      <div className="privacy-field-notice" role="note" style={{ margin: '8px 0 14px' }}>
        <span className="privacy-notice-icon" aria-hidden="true">🛡️</span>
        <div>
          <strong>Minimización de datos:</strong> Estas respuestas son procesadas por Inteligencia Artificial. Evite proporcionar nombres reales, números de cédula, direcciones o datos de contacto.
        </div>
      </div>
      {analisis.preguntas.map(pregunta => (
        <label className="question-field" key={pregunta.id}>
          {pregunta.texto}
          <textarea rows={2} value={respuestas[pregunta.id] ?? ''} onChange={e => cambiarRespuesta(pregunta.id, e.target.value)} placeholder="Respuesta opcional" />
        </label>
      ))}
      <button type="button" className="btn btn-primary" onClick={completar}>
        Continuar con la información
      </button>
    </div>
  )
}
