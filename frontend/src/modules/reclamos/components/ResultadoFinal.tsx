import type { Analisis } from '../types/reclamo.types'

export function ResultadoFinal({ analisis }: { analisis: Analisis }) {
  return (
    <div className="analysis-card completed">
      <h3>
        Resultado final de demostración
      </h3>
      <p>
        Se incorporaron las respuestas proporcionadas. No se realizó un reanálisis con IA.
      </p>
      {analisis.preguntas.length > 0 && <p>
        Preguntas sin respuesta: {analisis.preguntas.length}. No impiden continuar.
      </p>}
    </div>
  )
}
