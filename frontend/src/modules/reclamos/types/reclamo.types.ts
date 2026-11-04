export type ClaimType = 'producto' | 'servicio' | 'garantia' | 'otro'
export type Estado = 'inicio' | 'cargando' | 'preguntas' | 'final' | 'error'
export type Reclamo = {
  fullName: string
  email: string
  phone: string
  idCard: string
  claimType: ClaimType
  productOrService: string
  provider: string
  providerCity: string
  purchaseDate: string
  paidAmount: string
  incidentDescription: string
  requestedSolution: string
  otherSolution: string
}
export type Pregunta = {
  id: string
  texto: string
}
export type FuenteJuridica = {
  id: string
  law: string
  article: string
  text: string
  url: string
  validity: string
}

export type Analisis = {
  hechos: string[]
  preguntas: Pregunta[]
  incongruencias: string[]
  orientaciones: string[]
  etapa: 'aclaracion' | 'capa2' | 'capa3' | 'completo'
  estadoCapa2: string | null
  estadoCapa3: string | null
  informeViabilidad: string | null
  rutaAccion: string | null
  fuentes: string[]
  fuentesJuridicas: FuenteJuridica[]
  fuentesCitadas: string[]
  opcionesAccion: OpcionAccion[]
  opcionSeleccionada: string | null
  camposFormulario: ParametroFormulario[]
  avisoFormulario: string | null
  planEjecucion: Record<string, unknown> | null
  parametrosPendientes: string[]
  progreso: { completados: number; total: number }
}

export type ParametroFormulario = { parameterName: string; questionForUser?: string | null; currentValue?: string | null; parameterRole?: string | null }
export type OpcionAccion = { optionId: string; title: string; description?: string | null; basedOnArticle?: string | null; userInputsRequired?: ParametroFormulario[] | null }
