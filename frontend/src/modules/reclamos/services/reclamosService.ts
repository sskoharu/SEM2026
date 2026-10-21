import type { Analisis, Reclamo } from '../types/reclamo.types'
import { crearError } from '../../../shared/http/httpErrors'
import { analizarSimulacion } from './simulacionService'

export type ConfiguracionReclamos =
  | { modo: 'demostracion' }
  | { modo: 'api'; analizar?: (reclamo: Reclamo, senal?: AbortSignal) => Promise<Analisis> }

// El adaptador API solo se proporcionará cuando exista un contrato acordado.
export async function analizarReclamo(
  reclamo: Reclamo,
  senal?: AbortSignal,
  configuracion: ConfiguracionReclamos = { modo: 'demostracion' },
): Promise<Analisis> {
  if (senal?.aborted) throw crearError('cancelacion')
  if (configuracion.modo === 'demostracion') return analizarSimulacion(reclamo, senal)
  if (!configuracion.analizar) throw crearError('integracion')
  return configuracion.analizar(reclamo, senal)
}
