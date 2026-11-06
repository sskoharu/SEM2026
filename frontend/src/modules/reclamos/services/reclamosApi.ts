import { headersForPath, rememberSession, sessionHeaders } from './sessionAccess'
import { solicitarJson } from '../../../shared/http/httpClient'
import type { Analisis, FuenteJuridica, OpcionAccion, ParametroFormulario, Pregunta, Reclamo } from '../types/reclamo.types'

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
  legalSources?: FuenteJuridica[] | null
  citedSourceIds?: string[] | null
  missingQuestions?: string[] | null
  actionRouteOptions?: { options?: OpcionAccion[] | null } | null
  selectedOptionId?: string | null
  selectedOptionTitle?: string | null
  selectedOptionDescription?: string | null
  formInputsRequired?: ParametroFormulario[] | null
  executionPlan?: Record<string, unknown> | null
  status?: string | null
  message?: string | null
}

function esCadena(valor: unknown): valor is string {
  return typeof valor === 'string'
}

export function esRespuestaSesion(valor: unknown): valor is BackendSession {
  if (typeof valor !== 'object' || valor === null) return false
  const respuesta = valor as Record<string, unknown>
  if (!esCadena(respuesta.sessionId) || !respuesta.sessionId.trim()) return false
  for (const field of ['pendingParametersToAsk', 'applicableArticles', 'citedSourceIds', 'missingQuestions']) {
    const value = respuesta[field]
    if (value != null && (!Array.isArray(value) || value.some(item => typeof item !== 'string'))) return false
  }
  for (const field of ['layer1State', 'layer2State', 'layer3State', 'clarificationMessage', 'viabilityReport']) {
    if (respuesta[field] != null && typeof respuesta[field] !== 'string') return false
  }
  if (respuesta.collectedParameters != null && (typeof respuesta.collectedParameters !== 'object' || Array.isArray(respuesta.collectedParameters))) return false
  if (respuesta.legalSources != null && (!Array.isArray(respuesta.legalSources) || respuesta.legalSources.some(source => {
    if (typeof source !== 'object' || source === null) return true
    return ['id', 'law', 'article', 'text', 'url', 'validity'].some(key => typeof (source as Record<string, unknown>)[key] !== 'string')
  }))) return false
  if (respuesta.actionRouteOptions != null) {
    if (typeof respuesta.actionRouteOptions !== 'object') return false
    const options = (respuesta.actionRouteOptions as Record<string, unknown>).options
    if (!Array.isArray(options) || options.some(option => {
      if (!option || typeof option !== 'object') return true
      if (typeof option.optionId !== 'string' || typeof option.title !== 'string') return true
      return option.userInputsRequired != null && (!Array.isArray(option.userInputsRequired) || option.userInputsRequired.some((field: unknown) =>
        !field || typeof field !== 'object' || typeof (field as Record<string, unknown>).parameterName !== 'string'))
    })) return false
  }
  return true
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
    respuesta.layer3State === 'DONE' ||
    respuesta.layer3State === 'WAITING_FOR_ACTION_SELECTION' ||
    respuesta.layer3State === 'WAITING_FOR_PROCEDURAL_INPUT' ||
    respuesta.layer3State === 'ERROR'
  )
  const opcionSeleccionada = respuesta.actionRouteOptions?.options?.find(
    opcion => opcion.optionId === respuesta.selectedOptionId,
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
    fuentesJuridicas: Array.isArray(respuesta.legalSources) ? respuesta.legalSources : [],
    fuentesCitadas: Array.isArray(respuesta.citedSourceIds) ? respuesta.citedSourceIds : [],
    opcionesAccion: respuesta.actionRouteOptions?.options ?? [],
    opcionSeleccionada: opcionSeleccionada?.title ?? respuesta.selectedOptionTitle ?? null,
    camposFormulario: opcionSeleccionada?.userInputsRequired ?? respuesta.formInputsRequired ?? [],
    avisoFormulario: respuesta.clarificationMessage ?? null,
    planEjecucion: respuesta.executionPlan ?? null,
    parametrosPendientes: pendientes,
    progreso: {
      completados: recopilados.size,
      total: recopilados.size + pendientes.length,
    },
  }
}

const baseUrl = (import.meta.env.VITE_API_BASE_URL ?? 'http://localhost:8080').replace(/\/+$/, '')

const TIMEOUT_ANALISIS_IA_MS = 120_000

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
      timeoutMs: TIMEOUT_ANALISIS_IA_MS,
      cabeceras: { 'Content-Type': 'application/json', Accept: 'application/json', ...headersForPath(ruta) },
      cuerpo: JSON.stringify(cuerpo),
      senal,
      validar: esRespuestaSesion,
      alRecibir: (datos, cabeceras) => rememberSession(datos.sessionId, cabeceras.get('X-Claim-Token')),
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
  const sesion = await solicitarSesion(`/api/v1/claims/accept/${encodeURIComponent(sessionId)}`, {}, senal)
  return mapearRespuesta(sesion)
}
async function solicitarPasoCapa3(ruta: string, cuerpo: unknown, sessionId: string, senal?: AbortSignal): Promise<Analisis> {
  const sesion = await solicitarJson<BackendSession>(`${baseUrl}${ruta}`, {
    operacion: 'analisis-inicial',
    metodo: 'POST',
    cabeceras: { 'Content-Type': 'application/json', Accept: 'application/json', ...sessionHeaders(sessionId) },
    cuerpo: JSON.stringify(cuerpo),
    senal,
    validar: esRespuestaSesion,
  })
  if (sesion.sessionId !== sessionId) {
    throw new Error('La sesión devuelta no corresponde al reclamo solicitado.')
  }
  return mapearRespuesta(sesion)
}

export async function seleccionarAccionConApi(sessionId: string, optionId: string, senal?: AbortSignal): Promise<Analisis> {
  return solicitarPasoCapa3(`/api/v1/claims/select-action/${encodeURIComponent(sessionId)}`, { optionId }, sessionId, senal)
}

export async function enviarFormularioConApi(sessionId: string, datos: Record<string, string>, senal?: AbortSignal): Promise<Analisis> {
  return solicitarPasoCapa3(`/api/v1/claims/submit-procedural-form/${encodeURIComponent(sessionId)}`, datos, sessionId, senal)
}

export type ResultadoCalculos = {
  sessionId: string
  status: string
  executionPlan: Record<string, unknown>
  nextPhase: string
}

function esResultadoCalculos(valor: unknown): valor is ResultadoCalculos {
  if (typeof valor !== 'object' || valor === null) {
    return false
  }

  const datos = valor as Record<string, unknown>
  return (
    typeof datos.sessionId === 'string' &&
    datos.status === 'CALCULATIONS_COMPLETED' &&
    datos.nextPhase === 'DOCUMENT_GENERATION' &&
    typeof datos.executionPlan === 'object' &&
    datos.executionPlan !== null &&
    !Array.isArray(datos.executionPlan) &&
    (datos.executionPlan as Record<string, unknown>).calculationStatus === 'COMPLETED'
  )
}

export async function ejecutarCalculosConApi(
  sessionId: string,
  senal?: AbortSignal,
): Promise<ResultadoCalculos> {
  const resultado = await solicitarJson<ResultadoCalculos>(
    `${baseUrl}/api/v1/claims/${encodeURIComponent(sessionId)}/layer4/calculate`,
    {
      operacion: 'analisis-inicial',
      metodo: 'POST',
      cabeceras: { Accept: 'application/json', ...sessionHeaders(sessionId) },
      senal,
      validar: esResultadoCalculos,
    },
  )

  if (resultado.sessionId !== sessionId) {
    throw new Error('La respuesta de cálculos pertenece a otra sesión.')
  }

  return resultado
}

export type DatosDocumento = {
  fullName: string
  idCard: string
  email: string
  phone: string
  providerCommercialName: string
  providerRuc: string
  providerAddress: string
}

export type VistaPreviaDocumento = {
  previewId: string
  hechosRedactados: string
  pretensionesRedactadas: string
  documentoCompleto: string
}

export async function prepararVistaPreviaConApi(
  sessionId: string,
  datos: DatosDocumento,
  senal?: AbortSignal,
): Promise<VistaPreviaDocumento> {
  const respuesta = await fetch(
    `${baseUrl}/api/v1/claims/${encodeURIComponent(sessionId)}/layer5/preview`,
    {
      method: 'POST',
      headers: { 'Content-Type': 'application/json', Accept: 'application/json', ...sessionHeaders(sessionId) },
      body: JSON.stringify({ ...datos, confirmGeneration: true }),
      signal: senal,
    },
  )

  if (!respuesta.ok) {
    throw new Error(`No fue posible preparar la vista previa (HTTP ${respuesta.status}).`)
  }

  const vista: unknown = await respuesta.json()
  if (
    !vista || typeof vista !== 'object' ||
    typeof (vista as VistaPreviaDocumento).previewId !== 'string' ||
    typeof (vista as VistaPreviaDocumento).hechosRedactados !== 'string' ||
    typeof (vista as VistaPreviaDocumento).pretensionesRedactadas !== 'string' ||
    typeof (vista as VistaPreviaDocumento).documentoCompleto !== 'string'
  ) {
    throw new Error('El backend devolvió una vista previa incompleta.')
  }

  return vista as VistaPreviaDocumento
}

export async function generarDocumentoConApi(
  sessionId: string,
  datos: DatosDocumento,
  previewId: string,
  senal?: AbortSignal,
): Promise<Blob> {
  const respuesta = await fetch(
    `${baseUrl}/api/v1/claims/${encodeURIComponent(sessionId)}/layer5/generate`,
    {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        ...sessionHeaders(sessionId),
        Accept: 'application/vnd.openxmlformats-officedocument.wordprocessingml.document',
      },
      body: JSON.stringify({ ...datos, confirmGeneration: true, approveFinalText: true, previewId }),
      signal: senal,
    },
  )

  if (!respuesta.ok) {
    throw new Error(`No fue posible generar el documento (HTTP ${respuesta.status}).`)
  }

  const tipo = respuesta.headers.get('Content-Type') ?? ''
  if (!tipo.includes('application/vnd.openxmlformats-officedocument.wordprocessingml.document')) {
    throw new Error('El servidor no devolvió un documento Word válido.')
  }

  const archivo = await respuesta.blob()
  if (archivo.size === 0) {
    throw new Error('El documento generado está vacío.')
  }

  return archivo
}
