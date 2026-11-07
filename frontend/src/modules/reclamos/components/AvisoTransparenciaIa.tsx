import { useState, useEffect } from 'react'

type Props = {
  abierto: boolean
  alCerrar: () => void
  alAbrir: () => void
}

export function AvisoTransparenciaIa({ abierto, alCerrar, alAbrir }: Props) {
  const [pestanaActiva, setPestanaActiva] = useState<'ia' | 'limitaciones' | 'datos'>('ia')

  // Cerrar con la tecla Esc
  useEffect(() => {
    function manejarTecla(e: KeyboardEvent) {
      if (e.key === 'Escape' && abierto) {
        alCerrar()
      }
    }
    window.addEventListener('keydown', manejarTecla)
    return () => window.removeEventListener('keydown', manejarTecla)
  }, [abierto, alCerrar])

  return (
    <>
      {/* Banner superior permanente de transparencia y aviso de IA */}
      <aside className="ai-transparency-banner" aria-label="Aviso de transparencia sobre Inteligencia Artificial y datos">
        <div className="ai-banner-header">
          <div className="ai-banner-badge">
            <span className="ai-badge-icon" aria-hidden="true">🤖</span>
            <span>Aviso de Transparencia del Sistema</span>
          </div>
          <button
            type="button"
            className="ai-banner-toggle-btn"
            onClick={alAbrir}
            aria-haspopup="dialog"
          >
            Ver aviso completo y política de datos &rarr;
          </button>
        </div>

        <div className="ai-banner-grid">
          <div className="ai-banner-item">
            <strong className="ai-item-title">
              <span aria-hidden="true">⚖️</span> Interacción con IA
            </strong>
            <p>
              Usted interactúa con un sistema asistido por Inteligencia Artificial generativa. Es una herramienta de orientación ciudadana, <strong>no es un abogado ni proporciona asesoría jurídica vinculante</strong>.
            </p>
          </div>

          <div className="ai-banner-item">
            <strong className="ai-item-title">
              <span aria-hidden="true">⚠️</span> Limitaciones del sistema
            </strong>
            <p>
              Circunscrito a la Ley Orgánica de Defensa del Consumidor (LODC) de Ecuador. Requiere <strong>revisión humana obligatoria</strong>; las indemnizaciones quedan reservadas a la autoridad competente.
            </p>
          </div>

          <div className="ai-banner-item">
            <strong className="ai-item-title">
              <span aria-hidden="true">🛡️</span> Minimización de datos
            </strong>
            <p>
              <strong>No ingrese datos personales reales en el relato</strong>. Los datos identificativos (cédula, nombre, contacto) se aíslan del modelo de IA y se usan solo por código determinista para el Word final.
            </p>
          </div>
        </div>
      </aside>

      {/* Modal accesible con la documentación detallada */}
      {abierto && (
        <div
          className="modal-overlay"
          role="presentation"
          onClick={(e) => {
            if (e.target === e.currentTarget) alCerrar()
          }}
        >
          <div
            className="modal-dialog"
            role="dialog"
            aria-modal="true"
            aria-labelledby="modal-transparencia-titulo"
          >
            <div className="modal-header">
              <div className="modal-title-wrapper">
                <span className="modal-badge-icon" aria-hidden="true">⚖️</span>
                <div>
                  <h2 id="modal-transparencia-titulo" className="modal-title">
                    Transparencia, Limitaciones y Tratamiento de Datos
                  </h2>
                  <p className="modal-subtitle">
                    Generador Inteligente de Reclamos de Consumo · Marco ético y normativo
                  </p>
                </div>
              </div>
              <button
                type="button"
                className="modal-close-btn"
                onClick={alCerrar}
                aria-label="Cerrar aviso de transparencia"
              >
                &times;
              </button>
            </div>

            <nav className="modal-tabs" aria-label="Secciones del aviso de transparencia">
              <button
                type="button"
                className={`modal-tab-btn ${pestanaActiva === 'ia' ? 'active' : ''}`}
                onClick={() => setPestanaActiva('ia')}
                aria-selected={pestanaActiva === 'ia'}
                role="tab"
              >
                🤖 Interacción con IA
              </button>
              <button
                type="button"
                className={`modal-tab-btn ${pestanaActiva === 'limitaciones' ? 'active' : ''}`}
                onClick={() => setPestanaActiva('limitaciones')}
                aria-selected={pestanaActiva === 'limitaciones'}
                role="tab"
              >
                ⚠️ Limitaciones del Sistema
              </button>
              <button
                type="button"
                className={`modal-tab-btn ${pestanaActiva === 'datos' ? 'active' : ''}`}
                onClick={() => setPestanaActiva('datos')}
                aria-selected={pestanaActiva === 'datos'}
                role="tab"
              >
                🛡️ Tratamiento y Minimización de Datos
              </button>
            </nav>

            <div className="modal-body" role="tabpanel">
              {pestanaActiva === 'ia' && (
                <article className="modal-tab-content">
                  <h3>1. Aviso de interacción con Inteligencia Artificial (IA)</h3>
                  <div className="info-callout">
                    <p>
                      <strong>Identificación explícita:</strong> Este servicio utiliza modelos avanzados de lenguaje artificial (LLM) y recuperación de información vectorial (RAG) para orientarle en la redacción de reclamos de consumo.
                    </p>
                  </div>

                  <h4>¿Cómo interviene la IA en cada capa del sistema?</h4>
                  <ul className="modal-feature-list">
                    <li>
                      <strong>Capa 1 · Clasificación inicial del reclamo:</strong> La IA analiza la narrativa proporcionada por el usuario para identificar el tipo de reclamo y consultar fragmentos pertinentes de la Ley Orgánica de Defensa del Consumidor (LODC) en una base vectorial cerrada.
                    </li>
                    <li>
                      <strong>Capa 2 · Recolección de parámetros:</strong> La IA detecta datos faltantes en la narrativa y formula preguntas aclaratorias sobre fechas, montos o descripción del problema.
                    </li>
                    <li>
                      <strong>Capa 3 · Evaluación y propuesta procesal:</strong> Analiza los hechos frente a los artículos recuperados y sugiere rutas de acción, incluyendo el trámite de orientación y mediación ante la Defensoría del Pueblo (DPE).
                    </li>
                    <li>
                      <strong>Capa 4 · Cálculos deterministas (Sin intervención de IA):</strong> Por razones de seguridad jurídica y exactitud matemática, <em>el modelo de IA no calcula plazos, multas ni montos</em>. Todos los cálculos los realiza código Java con precisión matemática (`BigDecimal`), evitando alucinaciones numéricas.
                    </li>
                    <li>
                      <strong>Capa 5 · Redacción asistida del borrador:</strong> La IA redacta únicamente las secciones narrativas de antecedentes y pretensiones, las cuales se integran por código dentro de una plantilla estructurada de Word (.docx).
                    </li>
                  </ul>

                  <div className="alert-callout alert-warning">
                    <strong>Herramienta de asistencia, no patrocinio legal:</strong> El sistema actúa exclusivamente como un asistente de estructuración técnica y redacción. No es un abogado, no ofrece asesoramiento jurídico profesional y no sustituye la consulta o el patrocinio de un profesional del derecho.
                  </div>
                </article>
              )}

              {pestanaActiva === 'limitaciones' && (
                <article className="modal-tab-content">
                  <h3>2. Limitaciones conocidas del sistema</h3>
                  <p>
                    Para garantizar la honestidad técnica y la confianza ciudadana, se declaran de forma transparente las limitaciones del prototipo:
                  </p>

                  <div className="limitations-grid">
                    <div className="limitation-card">
                      <strong>⚖️ Alcance normativo acotado</strong>
                      <p>
                        El corpus legal indexado se limita a los artículos de la Ley Orgánica de Defensa del Consumidor (LODC) de Ecuador y guías de la Defensoría del Pueblo. No abarca el reglamento general ni normativas sectoriales ajenas.
                      </p>
                    </div>

                    <div className="limitation-card">
                      <strong>🎲 Variabilidad e imprecisiones de los modelos LLM</strong>
                      <p>
                        Los modelos de lenguaje pueden generar redacciones con variaciones o sugerir enfoques no aplicables a casos particulares. Las citas legales en texto libre deben ser cotejadas antes de cualquier trámite formal.
                      </p>
                    </div>

                    <div className="limitation-card">
                      <strong>🏛️ Decisiones reservadas a la autoridad</strong>
                      <p>
                        Rubros complejos como indemnizaciones por daños y perjuicios morales o lucro cesante quedan catalogados como <code>A_DEFINIR_POR_AUTORIDAD</code> y no se calculan de manera artificial.
                      </p>
                    </div>

                    <div className="limitation-card">
                      <strong>📑 Evidencia documental declarativa</strong>
                      <p>
                        Los documentos y facturas señalados por el usuario se registran de forma declarativa. El sistema no valida la autenticidad material, vigencia ni legalidad de los comprobantes adjuntos.
                      </p>
                    </div>

                    <div className="limitation-card">
                      <strong>👤 Supervisión humana obligatoria (Human-in-the-Loop)</strong>
                      <p>
                        El usuario debe revisar detenidamente el informe de viabilidad, validar los parámetros y verificar el documento descargado antes de firmarlo o presentarlo ante cualquier entidad.
                      </p>
                    </div>

                    <div className="limitation-card">
                      <strong>⏱️ Cobertura parcial del catálogo</strong>
                      <p>
                        El motor de cálculo determinista cubre principalmente reclamos de productos defectuosos, falta de entrega o incumplimiento de garantía. Otros casos requieren fijación por la autoridad.
                      </p>
                    </div>
                  </div>
                </article>
              )}

              {pestanaActiva === 'datos' && (
                <article className="modal-tab-content">
                  <h3>3. Tratamiento y minimización de datos personales documentada</h3>
                  <div className="info-callout">
                    <p>
                      En observancia con la <strong>Ley Orgánica de Protección de Datos Personales (LOPDP) de Ecuador</strong> y buenas prácticas éticas, este sistema aplica el <strong>principio de minimización de datos</strong> en todas sus capas.
                    </p>
                  </div>

                  <h4>Arquitectura de protección y aislamiento de datos:</h4>
                  <div className="privacy-flow-steps">
                    <div className="privacy-step-card">
                      <span className="privacy-step-num">1</span>
                      <div>
                        <strong>Fase de relato e interacción con IA (Datos anonimizados)</strong>
                        <p>
                          Se solicita expresamente al usuario <em>no ingresar información personal identificable (PII)</em> en el relato ni en las respuestas intermedias (sin nombres reales, números de cédula, direcciones o teléfonos). El texto del relato se procesa con el proveedor de IA únicamente con fines de comprensión semántica.
                        </p>
                      </div>
                    </div>

                    <div className="privacy-step-card">
                      <span className="privacy-step-num">2</span>
                      <div>
                        <strong>Fase de generación documental (Aislamiento de la IA)</strong>
                        <p>
                          Los datos del consumidor (cédula de 10 dígitos, nombre completo, teléfono y correo) se solicitan únicamente en la etapa final. <strong>Estos datos NUNCA son enviados a ningún modelo de Inteligencia Artificial</strong>. Son inyectados localmente y por código determinista directamente en la plantilla Word (.docx).
                        </p>
                      </div>
                    </div>

                    <div className="privacy-step-card">
                      <span className="privacy-step-num">3</span>
                      <div>
                        <strong>Ciclo de vida y retención temporal de sesiones</strong>
                        <p>
                          Las sesiones activas en el servidor (Redis) caducan automáticamente tras <strong>24 horas</strong> de inactividad, y la sesión en el cliente dura <strong>60 minutos</strong>. El token de autorización (<code>X-Claim-Token</code>) reside exclusivamente en la memoria volátil del navegador y se elimina al recargar la página.
                        </p>
                      </div>
                    </div>

                    <div className="privacy-step-card">
                      <span className="privacy-step-num">4</span>
                      <div>
                        <strong>Resguardo voluntario de reclamos</strong>
                        <p>
                          Si usted decide guardar su reclamo para continuar más adelante, sus datos se almacenan en la base de datos de resguardo asociada a su cédula. Ningún tercero ni modelo externo tiene acceso a este archivo.
                        </p>
                      </div>
                    </div>
                  </div>

                  <div className="alert-callout alert-success">
                    <strong>Derechos y control ciudadano:</strong> Usted tiene la potestad de limpiar la sesión, descartar los datos en memoria en cualquier instante con el botón <em>Limpiar</em> y conservar el control absoluto del reclamo generado.
                  </div>
                </article>
              )}
            </div>

            <div className="modal-footer">
              <span className="modal-footer-note">
                Proyecto académico UNIANDES · Cumplimiento de transparencia y LOPDP
              </span>
              <button type="button" className="btn btn-primary" onClick={alCerrar}>
                Entendido y cerrar
              </button>
            </div>
          </div>
        </div>
      )}
    </>
  )
}

