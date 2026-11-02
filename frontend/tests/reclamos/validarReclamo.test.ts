import { describe, expect, it } from 'vitest'
import { validarReclamo } from '../../src/modules/reclamos/utils/validarReclamo'
import { inicial } from '../../src/modules/reclamos/constants/reclamo.constants'

export const valido = {
  ...inicial,
  incidentDescription: 'El producto no funciona desde la primera utilización.',
}

describe('Validaciones de la capa 1', () => {
  it('rechaza descripción superior al límite de la interfaz', () => {
    expect(validarReclamo({ ...valido, incidentDescription: 'a'.repeat(2001) })).toHaveLength(1)
  })
  it('rechaza relato vacío o insuficiente', () => expect(validarReclamo(inicial)).toHaveLength(1))
  it('rechaza descripción corta', () => expect(validarReclamo({ ...valido, incidentDescription: 'Corto' })).toHaveLength(1))
  it('acepta un relato válido sin datos de capas posteriores', () => expect(validarReclamo(valido)).toEqual([]))
})
