import type { Reclamo } from '../types/reclamo.types'
import { opcionesProducto, opcionesServicio } from '../constants/reclamo.constants'

export function validarReclamo(form: Reclamo): string[] {
  const resultado: string[] = []
  if (!form.nombre.trim()) resultado.push('Ingrese el nombre completo.')
  if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(form.correo.trim())) resultado.push('Ingrese un correo electrónico válido.')
  if (!form.producto.trim()) resultado.push('Indique el producto o servicio.')
  if (!form.proveedor.trim()) resultado.push('Indique el proveedor o establecimiento.')
  if (form.descripcion.trim().length < 20) resultado.push('La descripción debe contener al menos 20 caracteres.')
  if (form.descripcion.length > 5000) resultado.push('La descripción no debe superar 5000 caracteres.')
  if (!form.solucion) resultado.push('Seleccione la solución que solicita.')
  const soluciones = form.tipo === 'servicio' ? opcionesServicio : opcionesProducto
  if (form.solucion && !soluciones.some(([valor]) => valor === form.solucion)) resultado.push('Seleccione una solución válida para el tipo de reclamo.')
  if (form.solucion === 'otra' && !form.otraSolucion.trim()) resultado.push('Describa la solución solicitada.')
  if (form.valor && (!/^\d+(\.\d{1,2})?$/.test(form.valor) || !Number.isFinite(Number(form.valor)))) resultado.push('El valor pagado debe ser válido.')
  if (form.fecha) {
    const fecha = new Date(`${form.fecha}T00:00:00Z`)
    const hoy = new Date()
    const fechaLocal = `${hoy.getFullYear()}-${String(hoy.getMonth() + 1).padStart(2, '0')}-${String(hoy.getDate()).padStart(2, '0')}`
    if (!/^\d{4}-\d{2}-\d{2}$/.test(form.fecha) || !Number.isFinite(fecha.getTime()) ||
      fecha.toISOString().slice(0, 10) !== form.fecha || form.fecha > fechaLocal) {
      resultado.push('Ingrese una fecha de compra válida que no sea futura.')
    }
  }
  return resultado
}
