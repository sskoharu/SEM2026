import { useState } from 'react'
import { createRoot } from 'react-dom/client'
import '../../src/index.css'
import '../../src/App.css'
import { solicitarJson } from '../../src/shared/http/httpClient'
import { normalizarError } from '../../src/shared/http/httpErrors'
import { obtenerMetricas } from '../../src/shared/metrics/metricasService'
import { MensajeError } from '../../src/modules/reclamos/components/MensajeError'
import type { MetricaSolicitud } from '../../src/shared/metrics/metricas.types'

type Escenario = '429' | 'timeout' | 'vacia' | 'invalida' | 'conexion' | '500' | 'tokens'
type Fixture = { texto: string; uso?: { entrada: number; salida: number; total: number } }

function instalarMock(escenario: Escenario) {
  const fetchOriginal = window.fetch
  // Exclusivo de esta página de pruebas; no se importa en main.tsx ni en la compilación del producto.
  window.fetch = async (_url, opciones) => {
    if (escenario === 'timeout') {
      return new Promise((_resolver, rechazar) => {
        opciones?.signal?.addEventListener('abort', () => rechazar(new DOMException('Abortado', 'AbortError')))
      })
    }
    if (escenario === 'conexion') throw new TypeError('Conexión simulada')
    if (escenario === '429' || escenario === '500') return new Response('', { status: Number(escenario) })
    if (escenario === 'vacia') return new Response('')
    if (escenario === 'invalida') return new Response('JSON inválido')
    return new Response(JSON.stringify({ texto: 'Fixture de prueba', uso: { entrada: 7, salida: 3, total: 10 } }))
  }
  return () => {
    window.fetch = fetchOriginal
  }
}

export function Laboratorio() {
  const [ocupado, setOcupado] = useState(false)
  const [error, setError] = useState('')
  const [mensaje, setMensaje] = useState('')
  const [metrica, setMetrica] = useState<MetricaSolicitud | null>(null)

  async function probar(escenario: Escenario) {
    if (ocupado) return
    setOcupado(true)
    setError('')
    setMetrica(null)
    setMensaje('Ejecutando escenario con fetch simulado...')
    const restaurarFetch = instalarMock(escenario)
    try {
      await solicitarJson('/recurso-exclusivo-de-prueba', {
        operacion: 'verificacion-http',
        timeoutMs: 100,
        reintentos: { maxIntentos: 3, esperaBaseMs: 50, esperaMaximaMs: 100 },
        validar: (datos): datos is Fixture => typeof datos === 'object' && datos !== null && 'texto' in datos,
        leerTokens: datos => ({ entrada: datos.uso?.entrada ?? null, salida: datos.uso?.salida ?? null, total: datos.uso?.total ?? null }),
      })
      setMensaje('Fixture aceptado. Los tokens 7, 3 y 10 son datos de prueba, no consumo real del modelo.')
    } catch (fallo) {
      setError(normalizarError(fallo).mensaje)
      setMensaje('Error controlado. Puede ejecutar otro escenario.')
    } finally {
      restaurarFetch()
      setMetrica(obtenerMetricas().at(-1) ?? null)
      setOcupado(false)
    }
  }

  return (
    <main className="main-content">
      <section className="panel">
        <h1>Laboratorio HTTP con mocks</h1>
        <p>Pruebas técnicas aisladas. No consulta el backend ni Ollama. Las latencias son medidas localmente.</p>
        <div className="form-actions">
          {(['429', 'timeout', 'vacia', 'invalida', 'conexion', '500', 'tokens'] as const).map(escenario => (
            <button key={escenario} className="btn btn-primary" disabled={ocupado} onClick={() => probar(escenario)}>
              Probar {escenario}
            </button>
          ))}
        </div>
        <p role="status">{mensaje}</p>
        {error && <MensajeError mensaje={error} />}
        {metrica && (
          <section>
            <h2>Métrica de la ejecución de prueba</h2>
            <p>Tokens ausentes se representan como null. HTTP y tokens provienen de mocks identificados.</p>
            <pre style={{ whiteSpace: 'pre-wrap', overflowWrap: 'anywhere' }}>{JSON.stringify(metrica, null, 2)}</pre>
          </section>
        )}
      </section>
    </main>
  )
}

createRoot(document.getElementById('root')!).render(<Laboratorio />)
