import type { Reclamo } from '../types/reclamo.types'

export const inicial: Reclamo = {
  fullName: '', email: '', phone: '', idCard: '', claimType: 'producto',
  productOrService: '', provider: '', providerCity: '', purchaseDate: '', paidAmount: '',
  incidentDescription: '', requestedSolution: '', otherSolution: '',
}

export const opcionesProducto = [
  ['reparacion', 'Reparación del producto'],
  ['cambio', 'Cambio o reposición'],
  ['devolucion', 'Devolución del dinero'],
  ['otra', 'Otra solución'],
]
export const opcionesServicio = [
  ['correccion', 'Corrección del servicio'],
  ['reembolso', 'Reembolso del valor pagado'],
  ['cumplimiento', 'Cumplimiento de lo ofrecido'],
  ['otra', 'Otra solución'],
]
