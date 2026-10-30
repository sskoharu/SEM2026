import type { Reclamo } from '../types/reclamo.types'
import { opcionesProducto, opcionesServicio } from '../constants/reclamo.constants'

export function validarReclamo(form: Reclamo): string[] {
  const resultado: string[] = []
  if (!form.fullName.trim()) resultado.push('Ingrese el nombre completo.')
  if (form.email.trim() && !/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(form.email.trim())) resultado.push('Ingrese un correo electrónico válido.')
  if (!/^\d{10}$/.test(form.idCard.trim())) resultado.push('La cédula debe contener exactamente 10 dígitos.')
  if (!form.productOrService.trim()) resultado.push('Indique el producto o servicio.')
  if (!form.provider.trim()) resultado.push('Indique el proveedor o establecimiento.')
  if (!form.providerCity.trim()) resultado.push('Indique la ciudad del proveedor.')
  if (form.incidentDescription.trim().length < 20) resultado.push('La descripción debe contener al menos 20 caracteres.')
  if (form.incidentDescription.length > 2000) resultado.push('La descripción no debe superar 2000 caracteres.')
  if (!form.requestedSolution) resultado.push('Seleccione la solución que solicita.')
  const soluciones = form.claimType === 'servicio' ? opcionesServicio : opcionesProducto
  if (form.requestedSolution && !soluciones.some(([valor]) => valor === form.requestedSolution)) resultado.push('Seleccione una solución válida para el tipo de reclamo.')
  if (form.requestedSolution === 'otra' && !form.otherSolution.trim()) resultado.push('Describa la solución solicitada.')
  if (!/^(?:0|[1-9]\d*)(?:\.\d{1,2})?$/.test(form.paidAmount) ||
    !Number.isFinite(Number(form.paidAmount)) || Number(form.paidAmount) <= 0) {
    resultado.push('El valor pagado debe ser mayor que cero y tener un formato válido.')
  }
  if (!form.purchaseDate) resultado.push('Indique la fecha de compra.')
  else {
    const fecha = new Date(`${form.purchaseDate}T00:00:00Z`)
    const hoy = new Date()
    const fechaLocal = `${hoy.getFullYear()}-${String(hoy.getMonth() + 1).padStart(2, '0')}-${String(hoy.getDate()).padStart(2, '0')}`
    if (!/^\d{4}-\d{2}-\d{2}$/.test(form.purchaseDate) || !Number.isFinite(fecha.getTime()) ||
      fecha.toISOString().slice(0, 10) !== form.purchaseDate || form.purchaseDate > fechaLocal) {
      resultado.push('Ingrese una fecha de compra válida que no sea futura.')
    }
  }
  return resultado
}
