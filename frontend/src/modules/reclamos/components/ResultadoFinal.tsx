import type { Analisis } from '../types/reclamo.types'

export function ResultadoFinal({ analisis, evaluar, aceptar }: { analisis: Analisis; evaluar: () => void; aceptar: () => void }) {
  return (
    <div className="analysis-card completed">
      <h3>
        {analisis.etapa === 'capa3' ? 'Evaluación jurídica' : 'Capa 2 completada'}
      </h3>
      <p>
        {analisis.informeViabilidad ?? 'El backend terminó de recopilar y validar la información disponible para el reclamo.'}
      </p>
      {analisis.fuentes.length > 0 && <div className="stage-status">
        <strong>Fuentes recuperadas</strong>
        <ul>
          {analisis.fuentes.map((fuente) => <li key={fuente}>Art. {fuente}</li>)}
        </ul>
      </div>}
      {analisis.etapa === 'completo' && <button type="button" className="btn btn-primary" onClick={evaluar}>
        Continuar con la evaluación jurídica
      </button>}
      {analisis.estadoCapa3 === 'WAITING_FOR_DECISION' && !analisis.rutaAccion && <button type="button" className="btn btn-primary" onClick={aceptar}>
        Aceptar evaluación y generar ruta de acción
      </button>}
      {analisis.rutaAccion && <div className="stage-status">
        <strong>Ruta de acción</strong>
        <span>{analisis.rutaAccion}</span>
      </div>}
      {analisis.preguntas.length > 0 && <p>
        Preguntas sin respuesta: {analisis.preguntas.length}. No impiden continuar.
      </p>}
    </div>
  )
}
