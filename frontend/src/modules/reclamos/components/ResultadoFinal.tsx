import type { Analisis } from '../types/reclamo.types'

type Propiedades = {
  analisis: Analisis
  evaluar: () => void
  aceptar: () => void
  seleccionarAccion: (id: string) => void
  enviarFormulario: () => void
  datosProcedimiento: Record<string, string>
  cambiarDato: (clave: string, valor: string) => void
}

export function ResultadoFinal({
  analisis,
  evaluar,
  aceptar,
  seleccionarAccion,
  enviarFormulario,
  datosProcedimiento,
  cambiarDato,
}: Propiedades) {
  return (
    <div className="analysis-card completed">
      <h3>
        {analisis.etapa === 'capa3' ? 'Evaluación jurídica' : 'Capa 2 completada'}
      </h3>

      <p>
        {analisis.informeViabilidad ??
          'El backend terminó de recopilar y validar la información disponible para el reclamo.'}
      </p>

      {analisis.fuentesJuridicas.length > 0 && (
        <section className="stage-status" aria-label="Fuentes jurídicas del análisis">
          <h4>Fuentes jurídicas recuperadas</h4>
          <p>
            Las referencias marcadas como citadas aparecen en el razonamiento.
            Las demás son fuentes recuperadas, no necesariamente utilizadas.
            La vigencia debe verificarse antes de presentar un reclamo.
          </p>

          {analisis.fuentesJuridicas.map((fuente, indice) => (
            <details key={`${fuente.id}-${indice}`}>
              <summary>
                {fuente.law} — Art. {fuente.article} — Ref. {fuente.id}
                {analisis.fuentesCitadas.includes(fuente.id)
                  ? ' (citada)'
                  : ' (solo recuperada)'}
              </summary>
              <p>
                <strong>Vigencia:</strong> {fuente.validity || 'No verificada'}
              </p>
              <p style={{ whiteSpace: 'pre-wrap' }}>
                {fuente.text || 'Fragmento no disponible'}
              </p>
              {/^https?:\/\//i.test(fuente.url) && (
                <a href={fuente.url} target="_blank" rel="noopener noreferrer">
                  Consultar fuente original
                </a>
              )}
            </details>
          ))}
        </section>
      )}

      {analisis.fuentesJuridicas.length === 0 && analisis.fuentes.length > 0 && (
        <div className="stage-status">
          <strong>Fuentes recuperadas</strong>
          <ul>
            {analisis.fuentes.map((fuente) => (
              <li key={fuente}>Art. {fuente}</li>
            ))}
          </ul>
        </div>
      )}

      {analisis.estadoCapa3 === 'ERROR' && (
        <div className="stage-status" role="alert">
          <strong>Evaluación jurídica detenida</strong>
          <p>
            {analisis.avisoFormulario ||
              'No fue posible continuar con una evaluación jurídica verificable. No se generarán acciones a partir de este informe.'}
          </p>
        </div>
      )}

      {analisis.etapa === 'completo' && (
        <button type="button" className="btn btn-primary" onClick={evaluar}>
          Continuar con la evaluación jurídica
        </button>
      )}

      {analisis.estadoCapa3 === 'WAITING_FOR_DECISION' && !analisis.rutaAccion && (
        <div className="ai-human-verification-box" role="note">
          <span aria-hidden="true">ℹ️</span>
          <div>
            <strong>Revisión humana indispensable:</strong> La evaluación anterior fue generada con asistencia de IA. Revise que los hechos y artículos reflejen fielmente su situación antes de aceptar.
          </div>
        </div>
      )}

      {analisis.estadoCapa3 === 'WAITING_FOR_DECISION' && !analisis.rutaAccion && (
        <button type="button" className="btn btn-primary" onClick={aceptar}>
          Aceptar evaluación y generar ruta de acción
        </button>
      )}

      {analisis.rutaAccion && (
        <div className="stage-status">
          <strong>Ruta de acción</strong>
          <span>{analisis.rutaAccion}</span>
        </div>
      )}

      {analisis.estadoCapa3 === 'WAITING_FOR_ACTION_SELECTION' && (
        <div className="stage-status">
          <h4>Seleccione cómo desea proceder</h4>
          {analisis.opcionesAccion.length === 0 && (
            <p>No hay opciones disponibles. No se realizará ninguna acción.</p>
          )}
          {analisis.opcionesAccion.map((opcion) => (
            <div key={opcion.optionId} className="analysis-card">
              <strong>{opcion.title}</strong>
              <p>{opcion.description}</p>
              {opcion.basedOnArticle && (
                <p>Referencia: {opcion.basedOnArticle}</p>
              )}
              <button
                type="button"
                className="btn btn-primary"
                onClick={() => seleccionarAccion(opcion.optionId)}
              >
                Elegir esta opción
              </button>
            </div>
          ))}
        </div>
      )}

      {analisis.estadoCapa3 === 'WAITING_FOR_PROCEDURAL_INPUT' && (
        <form
          className="stage-status"
          onSubmit={(evento) => {
            evento.preventDefault()
            enviarFormulario()
          }}
        >
          <h4>
            Información para {analisis.opcionSeleccionada ?? 'la acción seleccionada'}
          </h4>
          {analisis.avisoFormulario && <p role="alert">{analisis.avisoFormulario}</p>}
          {analisis.camposFormulario.map((campo) => (
            <label
              key={campo.parameterName}
              style={{ display: 'block', marginBottom: '1rem' }}
            >
              <span>{campo.questionForUser || campo.parameterName}</span>
              <input
                required
                type="text"
                value={datosProcedimiento[campo.parameterName] ?? campo.currentValue ?? ''}
                onChange={(evento) =>
                  cambiarDato(campo.parameterName, evento.target.value)
                }
              />
            </label>
          ))}
          <button type="submit" className="btn btn-primary">
            Enviar datos para validación
          </button>
        </form>
      )}

      {analisis.estadoCapa3 === 'DONE' && (
        <p role="status">
          La etapa de evaluación y selección jurídica ha finalizado.
        </p>
      )}

      {analisis.preguntas.length > 0 && (
        <p>
          Preguntas sin respuesta: {analisis.preguntas.length}. No impiden continuar.
        </p>
      )}
    </div>
  )
}
