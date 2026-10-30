import type { Analisis, Pregunta, Reclamo } from '../types/reclamo.types'

export function generarSimulacion(reclamo: Reclamo): Analisis {
  const hechos = [
    `El consumidor identifica a ${reclamo.provider} como proveedor.`,
    `El reclamo se relaciona con: ${reclamo.productOrService}.`,
    `Situación declarada: ${reclamo.incidentDescription.trim()}`,
  ]
  hechos.push(`Fecha indicada: ${reclamo.purchaseDate}.`)
  hechos.push(`Valor declarado: USD ${reclamo.paidAmount}.`)
  const preguntas: Pregunta[] = []
  preguntas.push({ id: 'contacto', texto: '¿Ya informó el problema al proveedor? ¿Qué respuesta recibió?' })
  if (reclamo.incidentDescription.trim().length < 70) {
    preguntas.push({ id: 'detalle', texto: '¿Qué falla o incumplimiento ocurrió y cómo le afectó?' })
  }
  return {
    hechos, preguntas, incongruencias: [],
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
