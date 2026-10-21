import type { Analisis, Reclamo } from '../types/reclamo.types'
import { generarSimulacion } from '../utils/simularAnalisis'
import { esperar } from '../../../shared/http/esperar'
import { crearError, esErrorSolicitud } from '../../../shared/http/httpErrors'
import { finalizarMedicion, iniciarMedicion } from '../../../shared/metrics/metricasService'

export async function analizarSimulacion(reclamo: Reclamo, senal?: AbortSignal): Promise<Analisis> {
  const medicion = iniciarMedicion('analisis-inicial')
  try {
    await esperar(850, senal)
    const resultado = generarSimulacion(reclamo)
    finalizarMedicion(medicion, { estado: 'exito', codigoHttp: null, intentos: 1 })
    return resultado
  } catch (error) {
    const controlado = esErrorSolicitud(error) ? error : crearError('simulacion')
    finalizarMedicion(medicion, {
      estado: controlado.tipo === 'cancelacion' ? 'cancelada' : 'error',
      codigoHttp: null,
      intentos: 1,
    })
    throw controlado
  }
}
