import { inicial } from '../../src/modules/reclamos/constants/reclamo.constants'
export const reclamoValido = {
  ...inicial, fullName: 'Consumidor de prueba', email: 'prueba@example.test',
  idCard: '0102030405', productOrService: 'Computadora', provider: 'Proveedor de prueba',
  providerCity: 'Quito', purchaseDate: '2026-10-01', paidAmount: '10',
  incidentDescription: 'El producto no funciona desde la primera utilización.', requestedSolution: 'cambio',
}
