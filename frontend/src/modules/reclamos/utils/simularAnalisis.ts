import type { Analisis, Pregunta, Reclamo } from '../types/reclamo.types'

export function generarSimulacion(reclamo: Reclamo): Analisis {
  const hechos = [
    `El consumidor identifica a ${reclamo.proveedor} como proveedor.`,
    `El reclamo se relaciona con: ${reclamo.producto}.`,
    `Situación declarada: ${reclamo.descripcion.trim()}`,
  ]
  if (reclamo.fecha) hechos.push(`Fecha indicada: ${reclamo.fecha}.`)
  if (reclamo.valor) hechos.push(`Valor declarado: USD ${reclamo.valor}.`)
  if (reclamo.comprobante) {
    const tipo = reclamo.tipoComprobante === 'factura' ? 'factura' : 'nota de venta'
    hechos.push(`El consumidor declara disponer de ${tipo}${reclamo.numeroComprobante ? `, número ${reclamo.numeroComprobante}` : ''}.`)
  }
  const preguntas: Pregunta[] = []
  if (!reclamo.fecha) preguntas.push({ id: 'fecha', texto: '¿Recuerda aproximadamente cuándo adquirió el producto o contrató el servicio?' })
  if (!reclamo.valor) preguntas.push({ id: 'valor', texto: '¿Conoce el valor pagado? Si no lo recuerda, puede indicarlo.' })
  preguntas.push({ id: 'contacto', texto: '¿Ya informó el problema al proveedor? ¿Qué respuesta recibió?' })
  if (reclamo.descripcion.trim().length < 70) {
    preguntas.push({ id: 'detalle', texto: '¿Qué falla o incumplimiento ocurrió y cómo le afectó?' })
  }
  return {
    hechos, preguntas,
    orientaciones: [
      'Revise que los hechos registrados sean correctos antes de continuar.',
      'Conserve los mensajes y otros antecedentes que tenga disponibles.',
      'Este resultado es una demostración, no una valoración legal ni una respuesta real de IA.',
    ],
  }
}


export function incorporarRespuestas(analisis: Analisis, respuestas: Record<string, string>): Analisis {
  const contestadas = analisis.preguntas
    .filter(p => respuestas[p.id]?.trim())
    .map(p => `${p.texto} Respuesta: ${respuestas[p.id].trim()}`)
  return {
    ...analisis,
    hechos: [...analisis.hechos, ...contestadas],
    preguntas: analisis.preguntas.filter(p => !respuestas[p.id]?.trim()),
  }
}
