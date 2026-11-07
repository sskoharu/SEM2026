import type {
  AbstentionInfo,
  AgentStep,
  Analisis,
  OpcionAccion,
  ParametroFormulario,
  Pregunta,
  RagEvidenceItem,
} from '../types/reclamo.types'

export type SesionGuardada = {
  sessionId: string
  layer1State?: string
  layer2State?: string
  layer3State?: string | null
  clarificationMessage?: string | null
  missingQuestions?: string[] | null
  pendingParametersToAsk?: string[]
  collectedParameters?: Record<string, unknown> | null
  currentQuestionForUser?: string | null
  currentParameterBeingAsked?: string | null
  conflictDetails?: {
    description?: string | null
    eventDate?: string | null
    involvedAmount?: number | null
  } | null
  provider?: { commercialName?: string | null } | null
  viabilityReport?: string | null
  actionRouteText?: string | null
  applicableArticles?: string[] | null
  actionRouteOptions?: { options?: OpcionAccion[] | null } | null
  selectedOptionId?: string | null
  selectedOptionTitle?: string | null
  formInputsRequired?: ParametroFormulario[] | null
  executionPlan?: Record<string, unknown> | null
  claimType?: string | null
  secondaryClaimType?: string | null
  ragEvidence?: RagEvidenceItem[] | null
  abstention?: AbstentionInfo | null
  agentTrace?: AgentStep[] | null
}

export function extraerSesion(valor: unknown): SesionGuardada | null {
  if (typeof valor !== 'object' || valor === null) return null
  const datos = valor as Record<string, unknown>
  if (typeof datos.sessionId === 'string' && datos.sessionId.trim().length > 0) {
    return datos as unknown as SesionGuardada
  }
  for (const clave of ['session', 'data', 'claim', 'savedClaim', 'result']) {
    const anidado = datos[clave]
    if (typeof anidado === 'object' && anidado !== null) {
      const anidadoObj = anidado as Record<string, unknown>
      if (typeof anidadoObj.sessionId === 'string' && anidadoObj.sessionId.trim().length > 0) {
        return anidadoObj as unknown as SesionGuardada
      }
    }
  }
  return null
}

export function esRespuestaSesion(valor: unknown): valor is SesionGuardada {
  return extraerSesion(valor) !== null
}

export function mapearSesionGuardada(raw: unknown): { sessionId: string; analisis: Analisis } {
  const sesion = extraerSesion(raw) ?? (raw as SesionGuardada)
  const pendientes = sesion.pendingParametersToAsk ?? []
  const capa2Terminada = sesion.layer2State === 'DONE' && pendientes.length === 0
  const capa3Activa = capa2Terminada && Boolean(sesion.layer3State)
  const opcion = sesion.actionRouteOptions?.options?.find(item => item.optionId === sesion.selectedOptionId)

  const hechos: string[] = []
  if (sesion.conflictDetails?.description) {
    hechos.push(`Situación declarada: ${sesion.conflictDetails.description}`)
  }
  if (sesion.conflictDetails?.eventDate) {
    hechos.push(`Fecha indicada: ${sesion.conflictDetails.eventDate}.`)
  }
  if (sesion.conflictDetails?.involvedAmount !== null && sesion.conflictDetails?.involvedAmount !== undefined) {
    hechos.push(`Valor declarado: USD ${sesion.conflictDetails.involvedAmount}.`)
  }
  if (sesion.provider?.commercialName) {
    hechos.push(`Proveedor identificado: ${sesion.provider.commercialName}.`)
  }
  for (const [clave, valor] of Object.entries(sesion.collectedParameters ?? {})) {
    if (valor !== null && valor !== undefined && String(valor).trim()) {
      hechos.push(`${clave}: ${typeof valor === 'object' ? JSON.stringify(valor) : String(valor)}.`)
    }
  }

  let preguntas: Pregunta[] = []
  if (sesion.layer1State === 'SOLICITANDO_ACLARACION') {
    preguntas = (sesion.missingQuestions ?? []).map((texto, indice) => ({
      id: `aclaracion-${indice}`,
      texto,
    }))
    if (preguntas.length === 0 && sesion.clarificationMessage) {
      preguntas = [{ id: 'aclaracion-0', texto: sesion.clarificationMessage }]
    }
  } else if (sesion.layer2State === 'WAITING_FOR_USER' && sesion.currentQuestionForUser) {
    preguntas = [{ id: sesion.currentParameterBeingAsked ?? 'capa2', texto: sesion.currentQuestionForUser }]
  }

  const etapa: Analisis['etapa'] = sesion.layer1State === 'SOLICITANDO_ACLARACION'
    ? 'aclaracion'
    : capa3Activa
      ? 'capa3'
      : capa2Terminada
        ? 'completo'
        : 'capa2'

  return {
    sessionId: sesion.sessionId,
    analisis: {
      hechos,
      preguntas,
      incongruencias: [],
      orientaciones: sesion.viabilityReport ? [sesion.viabilityReport] : [],
      etapa,
      estadoCapa2: sesion.layer2State ?? null,
      estadoCapa3: sesion.layer3State ?? null,
      informeViabilidad: sesion.viabilityReport ?? null,
      rutaAccion: sesion.actionRouteText ?? null,
      fuentes: sesion.applicableArticles ?? [],
      fuentesJuridicas: [],
      fuentesCitadas: [],
      ragEvidence: Array.isArray(sesion.ragEvidence) ? sesion.ragEvidence : [],
      abstention: sesion.abstention ?? null,
      agentTrace: Array.isArray(sesion.agentTrace) ? sesion.agentTrace : null,
      tipoReclamo: sesion.claimType ?? null,
      tipoReclamoSecundario: sesion.secondaryClaimType ?? null,
      opcionesAccion: sesion.actionRouteOptions?.options ?? [],
      opcionSeleccionada: opcion?.title ?? sesion.selectedOptionTitle ?? null,
      camposFormulario: opcion?.userInputsRequired ?? sesion.formInputsRequired ?? [],
      avisoFormulario: sesion.clarificationMessage ?? null,
      planEjecucion: sesion.executionPlan ?? null,
      parametrosPendientes: pendientes,
      progreso: {
        completados: Object.keys(sesion.collectedParameters ?? {}).length,
        total: Object.keys(sesion.collectedParameters ?? {}).length + pendientes.length,
      },
    },
  }
}
