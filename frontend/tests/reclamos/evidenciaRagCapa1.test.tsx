// @vitest-environment jsdom
import { afterEach, describe, expect, it, vi } from 'vitest'
import { cleanup, render, screen } from '@testing-library/react'
import { EvidenciaRagCapa1 } from '../../src/modules/reclamos/components/EvidenciaRagCapa1'
import type { RagEvidenceItem } from '../../src/modules/reclamos/types/reclamo.types'

afterEach(() => {
  cleanup()
  vi.restoreAllMocks()
})

const fixtureRagEvidence: RagEvidenceItem[] = [
  {
    fragmentId: 'lodc_ley_art_20',
    numeroArticulo: '20',
    score: 0.5880215167999268,
    snippet: 'Art. 20.- Defectos y Vicios Ocultos.- El consumidor podrá optar por la rescisión del contrato...',
    estadoRevision: 'APROBADO',
    corpusVersion: '426e9b95941c5b5c',
  },
  {
    fragmentId: 'lodc_ley_art_71',
    numeroArticulo: '71',
    score: 0.5507352352142334,
    snippet: 'Art. 71.- Indemnización, Reparación, Reposición y Devolución...',
    estadoRevision: 'APROBADO',
    corpusVersion: '426e9b95941c5b5c',
  },
]

describe('Componente EvidenciaRagCapa1', () => {
  it('renderiza la lista de fragmentos RAG con artículos, scores y snippets', () => {
    render(
      <EvidenciaRagCapa1
        ragEvidence={fixtureRagEvidence}
        abstention={null}
        agentTrace={null}
        tipoReclamo="VICIOS_OCULTOS_CALIDAD"
        fuentes={['20', '71']}
      />,
    )

    expect(screen.getByText(/Trazabilidad y Evidencia RAG \(Capa 1\)/i)).toBeTruthy()
    expect(screen.getByText(/Vicios Ocultos Calidad/i)).toBeTruthy()
    expect(screen.getAllByText(/Art\. 20/).length).toBeGreaterThanOrEqual(1)
    expect(screen.getAllByText(/Art\. 71/).length).toBeGreaterThanOrEqual(1)
    expect(screen.getByText(/Defectos y Vicios Ocultos/i)).toBeTruthy()
    expect(screen.getByText(/Indemnización, Reparación/i)).toBeTruthy()
    expect(screen.getByText(/58.8%/i)).toBeTruthy()
    expect(screen.getAllByText('APROBADO').length).toBeGreaterThanOrEqual(1)
  })

  it('muestra el estado de abstención como ninguna cuando abstention es null', () => {
    render(
      <EvidenciaRagCapa1
        ragEvidence={[]}
        abstention={null}
        agentTrace={null}
      />,
    )

    expect(screen.getByText(/Abstención:/i)).toBeTruthy()
    expect(screen.getByText(/Ninguna/i)).toBeTruthy()
    expect(screen.getByText(/Traza del agente:/i)).toBeTruthy()
  })

  it('muestra alerta de abstención cuando abstention contiene datos', () => {
    render(
      <EvidenciaRagCapa1
        ragEvidence={[]}
        abstention={{
          motivo: 'Similitud semántica menor a 0.40',
          mejorScore: 0.32,
          umbral: 0.4,
        }}
        agentTrace={null}
      />,
    )

    expect(screen.getByRole('alert')).toBeTruthy()
    expect(screen.getByText(/Abstención activada por evidencia insuficiente/i)).toBeTruthy()
    expect(screen.getByText(/Similitud semántica menor a 0.40/i)).toBeTruthy()
    expect(screen.getByText(/0.3200/i)).toBeTruthy()
  })

  it('muestra los pasos de agentTrace cuando están presentes', () => {
    render(
      <EvidenciaRagCapa1
        ragEvidence={fixtureRagEvidence}
        abstention={null}
        agentTrace={[
          {
            paso: 1,
            herramienta: 'VectorSearchTool',
            argumentos: { query: 'celular defectuoso' },
            resultado: '5 fragmentos recuperados',
            estado: 'SUCCESS',
          },
        ]}
      />,
    )

    expect(screen.getByText('Paso 1')).toBeTruthy()
    expect(screen.getByText('VectorSearchTool')).toBeTruthy()
    expect(screen.getByText(/5 fragmentos recuperados/i)).toBeTruthy()
  })
})
