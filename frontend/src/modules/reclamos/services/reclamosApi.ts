import { solicitarJson } from '../../../shared/http/httpClient'
import type { Analisis, Pregunta, Reclamo } from '../types/reclamo.types'

type BackendSession = {
  sessionId: string
  currentPhase?: string | null
  layer1State?: string
  layer2State?: string
  layer3State?: string | null
  currentQuestionForUser?: string | null
  currentParameterBeingAsked?: string | null
  clarificationMessage?: string | null
  pendingParametersToAsk?: string[]
  collectedParameters?: Record<string, unknown> | null
  conflictDetails?: {
    description?: string | null
    eventDate?: string | null
    involvedAmount?: number | null
  } | null
  provider?: { commercialName?: string | null } | null
  viabilityReport?: string | null
  actionRouteText?: string | null
  applicableArticles?: string[] | null
  missingQuestions?: string[] | null
}

function esCadena(valor: unknown): valor is string {
  return typeof valor === 'string'
}

function esRespuestaSesion(valor: unknown): valor is BackendSession {
  if (typeof valor !== 'object' || valor === null) return false
  const respuesta = valor as Record<string, unknown>
  return esCadena(respuesta.sessionId)
}

function crearHechos(respuesta: BackendSession): string[] {
  const hechos: string[] = []
  const conflicto = respuesta.conflictDetails
  if (conflicto?.description) hechos.push(`Situación declarada: ${conflicto.description}`)
  if (conflicto?.eventDate) hechos.push(`Fecha indicada: ${conflicto.eventDate}.`)
  if (conflicto?.involvedAmount !== null && conflicto?.involvedAmount !== undefined) {
    hechos.push(`Valor declarado: USD ${conflicto.involvedAmount}.`)
  }
  if (respuesta.provider?.commercialName) hechos.push(`Proveedor identificado: ${respuesta.provider.commercialName}.`)
  for (const [parametro, valor] of Object.entries(respuesta.collectedParameters ?? {})) {
    if (valor !== null && valor !== undefined && String(valor).trim()) {
      hechos.push(`${parametro}: ${typeof valor === 'object' ? JSON.stringify(valor) : String(valor)}.`)
    }
  }
  return hechos
}

function crearPreguntas(respuesta: BackendSession): Pregunta[] {
  if (respuesta.layer1State === 'SOLICITANDO_ACLARACION') {
    const preguntas = (respuesta.missingQuestions ?? []).map((texto, indice) => ({
      id: `aclaracion-${indice}`,
      texto,
    }))
    if (preguntas.length > 0) return preguntas
    if (respuesta.clarificationMessage) {
      return [{ id: 'aclaracion-0', texto: respuesta.clarificationMessage }]
    }
    return []
  }
  if (
    respuesta.layer2State === 'WAITING_FOR_USER' &&
    (respuesta.currentQuestionForUser || respuesta.clarificationMessage)
  ) {
    return [{
      id: respuesta.currentParameterBeingAsked ?? 'capa2',
      texto: respuesta.currentQuestionForUser ?? respuesta.clarificationMessage!,
    }]
  }
  return []
}

function mapearRespuesta(respuesta: BackendSession): Analisis {
  const recopilados = new Set(Object.keys(respuesta.collectedParameters ?? {}))
  const pendientes = respuesta.pendingParametersToAsk ?? []
  const capa2Terminada = respuesta.layer2State === 'DONE' && pendientes.length === 0
  const capa3Activa = capa2Terminada && (
    respuesta.layer3State === 'EVALUATING_VIABILITY' ||
    respuesta.layer3State === 'PRESENTING_REPORT' ||
    respuesta.layer3State === 'WAITING_FOR_DECISION' ||
    respuesta.layer3State === 'BATCH_VALIDATING' ||
    respuesta.layer3State === 'GENERATING_ACTION_ROUTE' ||
    respuesta.layer3State === 'DONE'
  )
  const orientaciones = [
    'Revise que los hechos extraídos sean correctos antes de continuar.',
  ]
  if (respuesta.viabilityReport) orientaciones.unshift(respuesta.viabilityReport)
  return {
    hechos: crearHechos(respuesta),
    preguntas: crearPreguntas(respuesta),
    incongruencias: [],
    orientaciones,
    etapa: respuesta.layer1State === 'SOLICITANDO_ACLARACION'
      ? 'aclaracion'
      : capa3Activa
        ? 'capa3'
        : capa2Terminada
          ? 'completo'
          : 'capa2',
    estadoCapa2: respuesta.layer2State ?? null,
    estadoCapa3: respuesta.layer3State ?? null,
    informeViabilidad: respuesta.viabilityReport ?? null,
    rutaAccion: respuesta.actionRouteText ?? null,
    fuentes: respuesta.applicableArticles ?? [],
    parametrosPendientes: pendientes,
    progreso: {
      completados: recopilados.size,
      total: recopilados.size + pendientes.length,
    },
  }
}

const baseUrl = (import.meta.env.VITE_API_BASE_URL ?? 'http://localhost:8080').replace(/\/+$/, '')

async function solicitarSesion(
  ruta: string,
  cuerpo: unknown,
  senal?: AbortSignal,
): Promise<BackendSession> {
  const respuesta = await solicitarJson<BackendSession>(
    `${baseUrl}${ruta}`,
    {
      operacion: 'analisis-inicial',
      metodo: 'POST',
      cabeceras: { 'Content-Type': 'application/json', Accept: 'application/json' },
      cuerpo: JSON.stringify(cuerpo),
      senal,
      validar: esRespuestaSesion,
    },
  )
  return respuesta
}

export async function analizarConApi(reclamo: Reclamo, senal?: AbortSignal): Promise<{ sessionId: string; analisis: Analisis }> {
  const inicio = await solicitarSesion('/api/v1/claims/start', {
    incidentDescription: reclamo.incidentDescription.trim(),
  }, senal)
  const sesion = inicio.layer1State === 'DONE'
    ? await solicitarSesion(`/api/v1/claims/${encodeURIComponent(inicio.sessionId)}/layer2/start`, {}, senal)
    : inicio
  return { sessionId: sesion.sessionId, analisis: mapearRespuesta(sesion) }
}

export async function iniciarCapa2ConApi(sessionId: string, senal?: AbortSignal): Promise<Analisis> {
  const sesion = await solicitarSesion(`/api/v1/claims/${encodeURIComponent(sessionId)}/layer2/start`, {}, senal)
  return mapearRespuesta(sesion)
}

export async function responderConApi(sessionId: string, respuesta: string, senal?: AbortSignal): Promise<Analisis> {
  const sesion = await solicitarSesion(`/api/v1/claims/${encodeURIComponent(sessionId)}/layer2/answer`, {
    userInput: respuesta,
  }, senal)
  return mapearRespuesta(sesion)
}

export async function aclararConApi(sessionId: string, respuesta: string, senal?: AbortSignal): Promise<Analisis> {
  const sesion = await solicitarSesion(`/api/v1/claims/${encodeURIComponent(sessionId)}/layer1/clarify`, {
    additionalInfo: respuesta,
  }, senal)
  const siguiente = sesion.layer1State === 'DONE'
    ? await solicitarSesion(`/api/v1/claims/${encodeURIComponent(sessionId)}/layer2/start`, {}, senal)
    : sesion
  return mapearRespuesta(siguiente)
}

export async function evaluarCapa3ConApi(sessionId: string, senal?: AbortSignal): Promise<Analisis> {
  const sesion = await solicitarSesion(`/api/v1/claims/evaluate/${encodeURIComponent(sessionId)}`, {}, senal)
  return mapearRespuesta(sesion)
}

export async function aceptarCapa3ConApi(sessionId: string, senal?: AbortSignal): Promise<Analisis> {
  const sesion = await solicitarSesion('/api/v1/claims/accept', {
    sessionId,
  }, senal)
  return mapearRespuesta(sesion)
}