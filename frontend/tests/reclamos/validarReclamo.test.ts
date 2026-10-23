import { describe, expect, it } from 'vitest'
import { validarReclamo } from '../../src/modules/reclamos/utils/validarReclamo'
import { inicial } from '../../src/modules/reclamos/constants/reclamo.constants'

export const valido = {
  ...inicial, fullName: 'Consumidor de prueba', email: 'prueba@example.test',
  idCard: '0102030405', productOrService: 'Computadora', provider: 'Proveedor de prueba',
  providerCity: 'Quito', purchaseDate: '2024-02-29', paidAmount: '10',
  incidentDescription: 'El producto no funciona desde la primera utilización.', requestedSolution: 'cambio',
}

describe('Formulario y validaciones originales', () => {
  it.each(['2026-02-30', 'fecha', '2999-01-01'])('rechaza fecha inválida o futura %s', fecha => {
    expect(validarReclamo({ ...valido, purchaseDate: fecha })).toContain('Ingrese una fecha de compra válida que no sea futura.')
  })
  it('acepta una fecha real pasada', () => expect(validarReclamo({ ...valido, purchaseDate: '2024-02-29' })).toEqual([]))
  it.each(['1e3', '0.001', ' '])('rechaza importe sin formato monetario %s', valor => {
    expect(validarReclamo({ ...valido, paidAmount: valor })).toHaveLength(1)
  })
  it('rechaza descripción superior al límite de la interfaz', () => {
    expect(validarReclamo({ ...valido, incidentDescription: 'a'.repeat(2001) })).toHaveLength(1)
  })
  it('rechaza solución incompatible con servicio', () => {
    expect(validarReclamo({ ...valido, claimType: 'servicio' })).toHaveLength(1)
  })
  it('rechaza campos vacíos', () => expect(validarReclamo(inicial)).toHaveLength(9))
  it('rechaza correo inválido', () => expect(validarReclamo({ ...valido, email: 'incorrecto' })).toContain('Ingrese un correo electrónico válido.'))
  it('rechaza descripción corta', () => expect(validarReclamo({ ...valido, incidentDescription: 'Corto' })).toHaveLength(1))
  it('requiere solución seleccionada', () => expect(validarReclamo({ ...valido, requestedSolution: '' })).toHaveLength(1))
  it('acepta formulario válido sin campos opcionales', () => expect(validarReclamo(valido)).toEqual([]))
  it('requiere texto para otra solución', () => expect(validarReclamo({ ...valido, requestedSolution: 'otra' })).toHaveLength(1))
  it.each(['-1', 'Infinity', 'abc'])('rechaza valor no válido %s', valor => {
    expect(validarReclamo({ ...valido, paidAmount: valor })).toHaveLength(1)
  })
  it('rechaza valor cero', () => expect(validarReclamo({ ...valido, paidAmount: '0' })).toHaveLength(1))
})
