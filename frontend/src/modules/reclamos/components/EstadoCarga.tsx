export function EstadoCarga() {
  return (
    <div className="analysis-placeholder" aria-busy="true">
      <div className="spinner" role="status" aria-label="Preparando análisis" />
      <h3>Preparando análisis...</h3>
      <p>Espere un momento.</p>
    </div>
  )
}
