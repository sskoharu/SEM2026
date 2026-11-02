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
  parametrosPendientes: string[]
  progreso: { completados: number; total: number }
}
