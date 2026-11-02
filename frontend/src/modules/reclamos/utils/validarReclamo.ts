import type { Reclamo } from '../types/reclamo.types'
export function validarReclamo(form: Reclamo): string[] {
  const resultado: string[] = []
  if (form.incidentDescription.trim().length < 20) resultado.push('La descripción debe contener al menos 20 caracteres.')
  if (form.incidentDescription.length > 2000) resultado.push('La descripción no debe superar 2000 caracteres.')
  return resultado
}
