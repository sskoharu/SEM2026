import { solicitarJson } from '../../../shared/http/httpClient'
import type { Analisis } from '../types/reclamo.types'
import {
  esRespuestaSesion,
  extraerSesion,
  mapearSesionGuardada,
  type SesionGuardada,
} from './savedClaimsMapper'
import { rememberSession, sessionHeaders } from './sessionAccess'

export type GuardarReclamoRequest = {
  idCard: string
  fullName: string
  contactInfo: string
}

export type ReclamoGuardado = {
  id: string
  sessionId?: string | null
  idCard?: string | null
  fullName?: string | null
  contactInfo?: string | null
  status?: string | null
  createdAt?: string | null
  updatedAt?: string | null
  detalles?: Record<string, unknown> | null
}

export type ReanudarReclamoResponse = {
  sessionId: string
  analisis: Analisis
}

const baseUrl = (
  import.meta.env.VITE_API_BASE_URL ?? 'https://asistente-inteligente-4kiq.onrender.com'
).replace(/\/+$/, '')
const idCardStorageKey = 'reclamos:idCard'

function obligatorio(valor: string, nombre: string): string {
  const limpio = valor.trim()
  if (!limpio) throw new Error(`${nombre} es obligatorio.`)
  return limpio
}

function esReclamoGuardado(valor: unknown): valor is ReclamoGuardado {
  if (typeof valor !== 'object' || valor === null) return false
  const datos = valor as Record<string, unknown>
  const id = datos.id ?? datos.savedClaimId ?? datos.claimId ?? datos.sessionId ?? datos._id
  if (typeof id === 'number') return true
  return typeof id === 'string' && id.trim().length > 0
}

function normalizarReclamoGuardado(
  valor: unknown,
  defaultSessionId?: string,
  defaults?: Partial<GuardarReclamoRequest>,
): ReclamoGuardado {
  const datos = (typeof valor === 'object' && valor !== null ? valor : {}) as Record<string, unknown>
  const rawId = datos.id ?? datos.savedClaimId ?? datos.claimId ?? datos.sessionId ?? datos._id ?? defaultSessionId ?? ''
  const id = String(rawId)
  return {
    id,
    sessionId: datos.sessionId ? String(datos.sessionId) : (defaultSessionId ?? null),
    idCard: datos.idCard ? String(datos.idCard) : (datos.idcard ? String(datos.idcard) : defaults?.idCard ?? null),
    fullName: datos.fullName
      ? String(datos.fullName)
      : datos.fullname
        ? String(datos.fullname)
        : datos.name
          ? String(datos.name)
          : defaults?.fullName ?? null,
    contactInfo: datos.contactInfo
      ? String(datos.contactInfo)
      : datos.contactinfo
        ? String(datos.contactinfo)
        : datos.contact
          ? String(datos.contact)
          : defaults?.contactInfo ?? null,
    status: datos.status ? String(datos.status) : (datos.message ? String(datos.message) : 'Guardado'),
    createdAt: datos.createdAt ? String(datos.createdAt) : (datos.created_at ? String(datos.created_at) : null),
    updatedAt: datos.updatedAt ? String(datos.updatedAt) : (datos.updated_at ? String(datos.updated_at) : null),
    detalles: datos,
  }
}

function encontrarLista(datos: Record<string, unknown>): unknown[] | null {
  for (const clave of ['content', 'claims', 'savedClaims', 'data', 'items', 'results', 'list']) {
    if (Array.isArray(datos[clave])) {
      return datos[clave] as unknown[]
    }
  }
  return null
}

function validarLista(valor: unknown): valor is ReclamoGuardado[] | Record<string, unknown> {
  if (Array.isArray(valor)) return true
  if (typeof valor !== 'object' || valor === null) return false
  const datos = valor as Record<string, unknown>
  const lista = encontrarLista(datos)
  return lista !== null || Object.keys(datos).length === 0
}

function extraerLista(valor: unknown): ReclamoGuardado[] {
  if (Array.isArray(valor)) {
    return valor.filter(esReclamoGuardado).map(item => normalizarReclamoGuardado(item))
  }
  if (typeof valor === 'object' && valor !== null) {
    const lista = encontrarLista(valor as Record<string, unknown>)
    if (lista) {
      return lista.filter(esReclamoGuardado).map(item => normalizarReclamoGuardado(item))
    }
  }
  return []
}

function validarRespuestaGuardado(valor: unknown): valor is Record<string, unknown> {
  return typeof valor === 'object' && valor !== null
}

export function guardarIdCardLocal(idCard: string): void {
  const valor = idCard.trim()
  if (valor) localStorage.setItem(idCardStorageKey, valor)
}

export function obtenerIdCardLocal(): string {
  return localStorage.getItem(idCardStorageKey) ?? ''
}

export async function guardarReclamo(
  sessionId: string,
  datos: GuardarReclamoRequest,
  senal?: AbortSignal,
): Promise<ReclamoGuardado> {
  const id = encodeURIComponent(obligatorio(sessionId, 'El sessionId'))
  const idCard = obligatorio(datos.idCard, 'La identificación')
  const fullName = obligatorio(datos.fullName, 'El nombre completo')
  const contactInfo = obligatorio(datos.contactInfo, 'La información de contacto')

  const respuesta = await solicitarJson<Record<string, unknown>>(`${baseUrl}/api/v1/claims/${id}/save`, {
    operacion: 'guardar-reclamo',
    metodo: 'POST',
    cabeceras: {
      'Content-Type': 'application/json',
      Accept: 'application/json',
      ...sessionHeaders(sessionId),
    },
    cuerpo: JSON.stringify({ idCard, fullName, contactInfo }),
    senal,
    validar: validarRespuestaGuardado,
  })
  guardarIdCardLocal(idCard)
  return normalizarReclamoGuardado(respuesta, sessionId, { idCard, fullName, contactInfo })
}

export async function listarReclamosGuardados(
  idCard: string,
  senal?: AbortSignal,
): Promise<ReclamoGuardado[]> {
  const valor = encodeURIComponent(obligatorio(idCard, 'La identificación'))
  const respuesta = await solicitarJson<unknown>(
    `${baseUrl}/api/v1/claims/saved?idCard=${valor}&idcard=${valor}`,
    {
      operacion: 'listar-reclamos-guardados',
      metodo: 'GET',
      cabeceras: { Accept: 'application/json' },
      senal,
      validar: validarLista,
    },
  )
  guardarIdCardLocal(idCard)
  return extraerLista(respuesta)
}

export const consultarReclamosGuardados = listarReclamosGuardados

export async function obtenerReclamoGuardado(
  id: string,
  idCard?: string,
  senal?: AbortSignal,
): Promise<ReclamoGuardado> {
  const claimId = encodeURIComponent(obligatorio(id, 'El id del reclamo guardado'))
  const cedula = (idCard?.trim() || obtenerIdCardLocal()).trim()
  const query = cedula
    ? `?idCard=${encodeURIComponent(cedula)}&idcard=${encodeURIComponent(cedula)}`
    : ''
  const respuesta = await solicitarJson<unknown>(
    `${baseUrl}/api/v1/claims/saved/${claimId}${query}`,
    {
      operacion: 'obtener-reclamo-guardado',
      metodo: 'GET',
      cabeceras: { Accept: 'application/json' },
      senal,
      validar: validarRespuestaGuardado,
    },
  )
  if (cedula) guardarIdCardLocal(cedula)
  return normalizarReclamoGuardado(respuesta, id)
}

export async function reanudarReclamo(
  id: string,
  idCard: string,
  senal?: AbortSignal,
): Promise<ReanudarReclamoResponse> {
  const claimId = encodeURIComponent(obligatorio(id, 'El id del reclamo guardado'))
  const identificacion = obligatorio(idCard, 'La identificación')
  const sesion = await solicitarJson<SesionGuardada>(
    `${baseUrl}/api/v1/claims/saved/${claimId}/resume`,
    {
      operacion: 'reanudar-reclamo',
      metodo: 'POST',
      cabeceras: { 'Content-Type': 'application/json', Accept: 'application/json' },
      cuerpo: JSON.stringify({ idCard: identificacion }),
      senal,
      validar: esRespuestaSesion,
      alRecibir: (datos, cabeceras) => {
        const sesionObj = extraerSesion(datos)
        if (sesionObj?.sessionId) {
          rememberSession(sesionObj.sessionId, cabeceras.get('X-Claim-Token'))
        }
      },
    },
  )
  guardarIdCardLocal(identificacion)
  return mapearSesionGuardada(sesion)
}
