import { describe, expect, it } from 'vitest'
import { validarReclamo } from '../../src/modules/reclamos/utils/validarReclamo'
import { inicial } from '../../src/modules/reclamos/constants/reclamo.constants'

export const valido = {
  ...inicial, nombre: 'Consumidor de prueba', correo: 'prueba@example.test',
  producto: 'Computadora', proveedor: 'Proveedor de prueba',
  descripcion: 'El producto no funciona desde la primera utilización.', solucion: 'cambio',
}

describe('Formulario y validaciones originales', () => {
  it.each(['2026-02-30', 'fecha', '2999-01-01'])('rechaza fecha inválida o futura %s', fecha => {
    expect(validarReclamo({ ...valido, fecha })).toContain('Ingrese una fecha de compra válida que no sea futura.')
  })
  it('acepta una fecha real pasada', () => expect(validarReclamo({ ...valido, fecha: '2024-02-29' })).toEqual([]))
  it.each(['1e3', '0.001', ' '])('rechaza importe sin formato monetario %s', valor => {
    expect(validarReclamo({ ...valido, valor })).toHaveLength(1)
  })
  it('rechaza descripción superior al límite de la interfaz', () => {
    expect(validarReclamo({ ...valido, descripcion: 'a'.repeat(5001) })).toHaveLength(1)
  })
  it('rechaza solución incompatible con servicio', () => {
    expect(validarReclamo({ ...valido, tipo: 'servicio' })).toHaveLength(1)
  })
  it('rechaza campos vacíos', () => expect(validarReclamo(inicial)).toHaveLength(6))
  it('rechaza correo inválido', () => expect(validarReclamo({ ...valido, correo: 'incorrecto' })).toContain('Ingrese un correo electrónico válido.'))
  it('rechaza descripción corta', () => expect(validarReclamo({ ...valido, descripcion: 'Corto' })).toHaveLength(1))
  it('requiere solución seleccionada', () => expect(validarReclamo({ ...valido, solucion: '' })).toHaveLength(1))
  it('acepta formulario válido sin campos opcionales', () => expect(validarReclamo(valido)).toEqual([]))
  it('requiere texto para otra solución', () => expect(validarReclamo({ ...valido, solucion: 'otra' })).toHaveLength(1))
  it.each(['-1', 'Infinity', 'abc'])('rechaza valor no válido %s', valor => {
    expect(validarReclamo({ ...valido, valor })).toHaveLength(1)
  })
  it('acepta valor cero', () => expect(validarReclamo({ ...valido, valor: '0' })).toEqual([]))
})
