import type { AbstentionInfo, AgentStep, RagEvidenceItem } from '../types/reclamo.types'

type Props = {
  ragEvidence: RagEvidenceItem[]
  abstention: AbstentionInfo | null
  agentTrace: AgentStep[] | null
  tipoReclamo?: string | null
  tipoReclamoSecundario?: string | null
  fuentes?: string[]
}

function formatearTipo(tipo: string): string {
  return tipo
    .toLowerCase()
    .split('_')
    .map((palabra) => palabra.charAt(0).toUpperCase() + palabra.slice(1))
    .join(' ')
}

export function EvidenciaRagCapa1({
  ragEvidence,
  abstention,
  agentTrace,
  tipoReclamo,
  tipoReclamoSecundario,
  fuentes = [],
}: Props) {
  return (
    <section className="analysis-card rag-evidence-card" aria-label="Trazabilidad y Evidencia RAG de Capa 1">
      <div className="rag-card-header">
        <div className="rag-title-wrapper">
          <span className="rag-section-icon" aria-hidden="true">📚</span>
          <div>
            <h3>Trazabilidad y Evidencia RAG (Capa 1)</h3>
            <p>
              Fragmentos de la LODC recuperados en la base vectorial, evaluación de abstención y traza del agente.
            </p>
          </div>
        </div>
      </div>

      {/* 1. Tipología de reclamo y artículos aplicables inferidos */}
      {(tipoReclamo || fuentes.length > 0) && (
        <div className="rag-classification-summary">
          {tipoReclamo && (
            <div className="rag-claim-pill">
              <span className="pill-label">Clasificación:</span>
              <strong>{formatearTipo(tipoReclamo)}</strong>
            </div>
          )}
          {tipoReclamoSecundario && (
            <div className="rag-claim-pill secondary">
              <span className="pill-label">Secundario:</span>
              <strong>{formatearTipo(tipoReclamoSecundario)}</strong>
            </div>
          )}
          {fuentes.length > 0 && (
            <div className="rag-articles-list">
              <span className="pill-label">Artículos LODC:</span>
              {fuentes.map((art) => (
                <span key={art} className="article-badge">
                  Art. {art}
                </span>
              ))}
            </div>
          )}
        </div>
      )}

      {/* 2. Campo Abstención */}
      <div className="rag-metadata-block">
        <h4>Control de Abstención (Capa 1)</h4>
        {abstention ? (
          <div className="abstention-alert" role="alert">
            <div className="abstention-alert-header">
              <span className="abstention-icon" aria-hidden="true">⚠️</span>
              <strong>Abstención activada por evidencia insuficiente</strong>
            </div>
            <p className="abstention-motivo">
              <strong>Motivo:</strong> {abstention.motivo || 'Score inferior al umbral de seguridad jurídica.'}
            </p>
            <div className="abstention-metrics">
              <span>
                <strong>Mejor score obtenido:</strong>{' '}
                {abstention.mejorScore !== undefined && abstention.mejorScore !== null
                  ? abstention.mejorScore.toFixed(4)
                  : 'N/A'}
              </span>
              <span>
                <strong>Umbral mínimo de corte:</strong>{' '}
                {abstention.umbral !== undefined && abstention.umbral !== null
                  ? abstention.umbral.toFixed(2)
                  : '0.40'}
              </span>
            </div>
          </div>
        ) : (
          <div className="abstention-ok-badge">
            <span aria-hidden="true">🛡️</span>
            <div>
              <strong>Abstención:</strong> <em>Ninguna</em> — La similitud semántica de los artículos recuperados superó el umbral de seguridad jurídica (≥ 0.40).
            </div>
          </div>
        )}
      </div>

      {/* 3. Campo Agent Trace */}
      <div className="rag-metadata-block">
        <h4>Traza de Ejecución del Agente (Agent Trace)</h4>
        {agentTrace && agentTrace.length > 0 ? (
          <div className="agent-trace-list">
            {agentTrace.map((paso, indice) => (
              <div key={indice} className="agent-trace-step">
                <div className="agent-trace-header">
                  <span className="step-number-tag">Paso {paso.paso ?? indice + 1}</span>
                  <strong>{paso.herramienta ?? 'Herramienta'}</strong>
                  {paso.estado && <span className="step-status-tag">{paso.estado}</span>}
                </div>
                {paso.argumentos && (
                  <div className="agent-trace-data">
                    <span className="data-label">Argumentos:</span>
                    <pre>{JSON.stringify(paso.argumentos, null, 2)}</pre>
                  </div>
                )}
                {paso.resultado && (
                  <div className="agent-trace-data">
                    <span className="data-label">Resultado:</span>
                    <p>{paso.resultado}</p>
                  </div>
                )}
              </div>
            ))}
          </div>
        ) : (
          <div className="agent-trace-empty">
            <span aria-hidden="true">🤖</span>
            <div>
              <strong>Traza del agente:</strong> <em>N/A en esta etapa</em> — Clasificación directa determinista basada en metadatos del corpus cerrado sin llamadas a herramientas externas.
            </div>
          </div>
        )}
      </div>

      {/* 4. Campo RAG Evidence (Respuestas de la evidencia RAG) */}
      <div className="rag-evidence-list-container">
        <h4>
          Evidencia RAG recuperada ({ragEvidence.length} fragmentos)
        </h4>
        {ragEvidence.length === 0 ? (
          <p className="rag-empty-message">No se recuperaron fragmentos RAG en esta sesión.</p>
        ) : (
          <div className="rag-evidence-grid">
            {ragEvidence.map((item, index) => (
              <details
                key={`${item.fragmentId}-${index}`}
                className="rag-evidence-item"
                open={index === 0}
              >
                <summary className="rag-evidence-summary">
                  <div className="rag-summary-title">
                    <span className="rag-art-pill">Art. {item.numeroArticulo}</span>
                    <span className="rag-fragment-id">{item.fragmentId}</span>
                  </div>
                  <div className="rag-summary-badges">
                    <span
                      className={`rag-score-pill ${
                        item.score >= 0.55 ? 'score-high' : item.score >= 0.4 ? 'score-mid' : 'score-low'
                      }`}
                      title={`Puntaje de similitud semántica: ${item.score}`}
                    >
                      Score: {item.score.toFixed(4)} ({(item.score * 100).toFixed(1)}%)
                    </span>
                    <span
                      className={`rag-rev-pill ${
                        item.estadoRevision === 'APROBADO' ? 'rev-approved' : 'rev-pending'
                      }`}
                    >
                      {item.estadoRevision || 'APROBADO'}
                    </span>
                    {item.corpusVersion && (
                      <span className="rag-version-pill" title="Versión de corpus indexado">
                        v:{item.corpusVersion.slice(0, 8)}
                      </span>
                    )}
                  </div>
                </summary>
                <div className="rag-evidence-content">
                  <blockquote className="rag-snippet-text">
                    {item.snippet}
                  </blockquote>
                </div>
              </details>
            ))}
          </div>
        )}
      </div>
    </section>
  )
}

