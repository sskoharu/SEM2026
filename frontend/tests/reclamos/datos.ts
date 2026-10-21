import { inicial } from '../../src/modules/reclamos/constants/reclamo.constants'
export const reclamoValido = {
  ...inicial, nombre: 'Consumidor de prueba', correo: 'prueba@example.test',
  producto: 'Computadora', proveedor: 'Proveedor de prueba',
  descripcion: 'El producto no funciona desde la primera utilización.', solucion: 'cambio',
}
