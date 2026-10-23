import { solicitarJson } from '../../../shared/http/httpClient'
import { crearError } from '../../../shared/http/httpErrors'
import type { Analisis, Pregunta, Reclamo } from '../types/reclamo.types'

type ClaimAnalysisResponse = {
  estado: string
  quePaso: string | null
  cuando: string | null
  proveedor: string | null
  monto: {
    valor: number | null
    moneda: string | null
    textoOriginal: string | null
  } | null
  evidenciaDisponible: string[]
  datosFaltantes: string[]
  incongruencias: string[]
  motivoAbstencion: string | null
  metrics?: {
    promptTokens: number | null
    completionTokens: number | null
    totalTokens: number | null
  } | null
  analizedAt?: string
}

function esCadena(valor: unknown): valor is string {
  return typeof valor === 'string'
}

function esListaDeCadenas(valor: unknown): valor is string[] {
  return Array.isArray(valor) && valor.every(esCadena)
}

function esRespuestaAnalisis(valor: unknown): valor is ClaimAnalysisResponse {
  if (typeof valor !== 'object' || valor === null) return false
  const respuesta = valor as Record<string, unknown>
  const monto = respuesta.monto
  const montoValido = monto === null || (typeof monto === 'object' && monto !== null &&
    (typeof (monto as Record<string, unknown>).valor === 'number' ||
      (monto as Record<string, unknown>).valor === null) &&
    (typeof (monto as Record<string, unknown>).moneda === 'string' ||
      (monto as Record<string, unknown>).moneda === null) &&
    (typeof (monto as Record<string, unknown>).textoOriginal === 'string' ||
      (monto as Record<string, unknown>).textoOriginal === null))
  return esCadena(respuesta.estado) &&
    (esCadena(respuesta.quePaso) || respuesta.quePaso === null) &&
    (esCadena(respuesta.cuando) || respuesta.cuando === null) &&
    (esCadena(respuesta.proveedor) || respuesta.proveedor === null) &&
    montoValido &&
    esListaDeCadenas(respuesta.evidenciaDisponible) &&
    esListaDeCadenas(respuesta.datosFaltantes) &&
    esListaDeCadenas(respuesta.incongruencias) &&
    (esCadena(respuesta.motivoAbstencion) || respuesta.motivoAbstencion === null)
}

function crearHechos(respuesta: ClaimAnalysisResponse): string[] {
  const hechos: string[] = []
  if (respuesta.quePaso) hechos.push(respuesta.quePaso)
  if (respuesta.cuando) hechos.push(`Momento indicado: ${respuesta.cuando}.`)
  if (respuesta.proveedor) hechos.push(`Proveedor identificado: ${respuesta.proveedor}.`)
  if (respuesta.monto) {
    const valor = respuesta.monto.valor === null ? respuesta.monto.textoOriginal : `${respuesta.monto.valor} ${respuesta.monto.moneda ?? ''}`.trim()
    if (valor) hechos.push(`Monto identificado: ${valor}.`)
  }
  if (respuesta.evidenciaDisponible.length > 0) {
    hechos.push(`Evidencia disponible: ${respuesta.evidenciaDisponible.join(', ')}.`)
  }
  return hechos
}

function crearPreguntas(respuesta: ClaimAnalysisResponse): Pregunta[] {
  return respuesta.datosFaltantes.map((campo, indice) => ({
    id: `dato-faltante-${indice}`,
    texto: `Falta confirmar el dato: ${campo}.`,
  }))
}

function mapearRespuesta(respuesta: ClaimAnalysisResponse): Analisis {
  const orientaciones = [
    'Revise que los hechos extraídos sean correctos antes de continuar.',
    'Este análisis es una extracción informativa y no constituye asesoría legal.',
  ]
  if (respuesta.motivoAbstencion) orientaciones.unshift(`Motivo de abstención: ${respuesta.motivoAbstencion}`)
  return {
    hechos: crearHechos(respuesta),
    preguntas: crearPreguntas(respuesta),
    incongruencias: respuesta.incongruencias,
    orientaciones,
  }
}

function construirSolicitud(reclamo: Reclamo) {
  return {
    fullName: reclamo.fullName.trim(),
    email: reclamo.email.trim() || null,
    phone: reclamo.phone.trim() || null,
    idCard: reclamo.idCard.trim(),
    claimType: reclamo.claimType,
    productOrService: reclamo.productOrService.trim(),
    provider: reclamo.provider.trim(),
    providerCity: reclamo.providerCity.trim(),
    purchaseDate: reclamo.purchaseDate,
    paidAmount: Number(reclamo.paidAmount),
    incidentDescription: reclamo.incidentDescription.trim(),
    requestedSolution: reclamo.requestedSolution === 'otra'
      ? reclamo.otherSolution.trim()
      : reclamo.requestedSolution,
  }
}

const baseUrl = (import.meta.env.VITE_API_BASE_URL ?? '').replace(/\/+$/, '')

export async function analizarConApi(reclamo: Reclamo, senal?: AbortSignal): Promise<Analisis> {
  if (!baseUrl) throw crearError('integracion')
  const respuesta = await solicitarJson<ClaimAnalysisResponse>(
    `${baseUrl}/api/v1/claims/analyze?version=v3`,
    {
      operacion: 'analisis-inicial',
      metodo: 'POST',
      cabeceras: { 'Content-Type': 'application/json', Accept: 'application/json' },
      cuerpo: JSON.stringify(construirSolicitud(reclamo)),
      senal,
      validar: esRespuestaAnalisis,
      leerTokens: datos => ({
        entrada: datos.metrics?.promptTokens ?? null,
        salida: datos.metrics?.completionTokens ?? null,
        total: datos.metrics?.totalTokens ?? null,
      }),
    },
  )
  return mapearRespuesta(respuesta)
}