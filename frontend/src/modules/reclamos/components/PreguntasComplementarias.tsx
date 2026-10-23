import type { Analisis } from '../types/reclamo.types'

type Propiedades = {
  analisis: Analisis
  respuestas: Record<string, string>
  cambiarRespuesta: (id: string, respuesta: string) => void
  completar: () => void
  modoApi: boolean
}

export function PreguntasComplementarias({ analisis, respuestas, cambiarRespuesta, completar, modoApi }: Propiedades) {
  return (
    <div className="analysis-card questions">
      <h3>
        Información complementaria
      </h3>
      <p>
        Responda lo que conozca. Puede dejar preguntas sin respuesta.
      </p>
      {analisis.preguntas.map(pregunta => (
        <label className="question-field" key={pregunta.id}>
          {pregunta.texto}
          <textarea rows={2} value={respuestas[pregunta.id] ?? ''} onChange={e => cambiarRespuesta(pregunta.id, e.target.value)} placeholder="Respuesta opcional" />
        </label>
      ))}
      <button type="button" className="btn btn-primary" onClick={completar}>
        {modoApi ? 'Continuar con la información' : 'Actualizar resultado (simulación)'}
      </button>
    </div>
  )
}
