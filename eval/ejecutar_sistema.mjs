// Ejecuta entradas reales sin inventar revisión semántica. Node 18+.
import fs from 'node:fs'
import path from 'node:path'
import { fileURLToPath } from 'node:url'

const directory = path.dirname(fileURLToPath(import.meta.url))
const cases = fs.readFileSync(path.join(directory, 'casos_rag.jsonl'), 'utf8')
  .split(/\r?\n/).filter(Boolean).map(line => JSON.parse(line))
const base = process.env.EVAL_BASE_URL
const statusPath = path.join(directory, 'estado_ejecucion.json')
const unavailable = !base
if (unavailable) {
  fs.writeFileSync(statusPath, JSON.stringify({
    status: 'NO_VERIFICADO', executed: 0, expected: cases.length,
    reason: 'No hay URL de una instancia de evaluación disponible. No se iniciaron servicios ni se modificó la base.',
    cases: cases.map(item => ({ id: item.id, status: 'NO_VERIFICADO' })),
  }, null, 2) + '\n')
  console.error('NO_VERIFICADO: no hay instancia de evaluación disponible. No se fabrican respuestas.')
  process.exitCode = 2
} else {
  const output = path.join(directory, 'resultados_observados.jsonl')
  if (fs.existsSync(output)) throw new Error('Ya existen observaciones. Conserve la corrida antes de iniciar otra.')
  const observed = []
  for (const item of cases) {
    try {
      const root = base.replace(/\/$/, '')
      const response = await request(root + '/api/v1/claims/start', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ incidentDescription: item.question }),
      })
      let body = await response.json()
      const row = {
        id: item.id,
        http_status: response.status,
        response_text: JSON.stringify(body),
        execution_mode: 'real_api_automated_follow_up',
        review_required: true,
      }
      if (response.status === 400 && typeof body.error === 'string') row.rejected = true
      if (response.ok && body.sessionId) {
        const token = response.headers.get('X-Claim-Token')
        const sessionRoot = root + '/api/v1/claims/' + encodeURIComponent(body.sessionId)

        if (body.layer1State === 'SOLICITANDO_ACLARACION') {
          const clarified = await request(sessionRoot + '/layer1/clarify', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json', 'X-Claim-Token': token ?? '' },
            body: JSON.stringify({
              additionalInfo: 'La consulta se refiere a la aplicación de la Ley Orgánica de Defensa del Consumidor en Ecuador.',
            }),
          })
          body = await clarified.json()
          row.http_status = clarified.status
          row.response_text = JSON.stringify(body)
        }

        if (body.layer1State === 'DONE') {
          const collection = await request(sessionRoot + '/layer2/start', {
            method: 'POST',
            headers: { 'X-Claim-Token': token ?? '' },
          })
          body = await collection.json()
          row.http_status = collection.status
          row.response_text = JSON.stringify(body)
        }

        let answerCount = 0
        while (body.layer2State === 'WAITING_FOR_USER' && answerCount < 50) {
          const parameter = body.currentParameterBeingAsked
          const userInput = answerForParameter(parameter)
          const answered = await request(sessionRoot + '/layer2/answer', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json', 'X-Claim-Token': token ?? '' },
            body: JSON.stringify({ userInput }),
          })
          body = await answered.json()
          row.http_status = answered.status
          row.response_text = JSON.stringify(body)
          answerCount += 1
        }

        if (body.layer2State === 'WAITING_FOR_USER') {
          throw new Error('La capa 2 excedió el límite de respuestas automatizadas.')
        }

        if (body.layer2State === 'DONE') {
          const evaluated = await request(root + '/api/v1/claims/evaluate/' + encodeURIComponent(body.sessionId), {
            method: 'POST',
            headers: { 'X-Claim-Token': token ?? '' },
          })
          const session = await evaluated.json()
          row.http_status = evaluated.status
          row.response_text = JSON.stringify(session)
          row.retrieved_source_ids = (session.legalSources ?? []).map(source => source.id)
          row.cited_source_ids = session.citedSourceIds ?? []
        }
      }
      // answer_correct/citations_reviewed/abstained requieren revisión de la respuesta real.
      observed.push(row)
    } catch (error) {
      observed.push({ id: item.id, execution_status: 'ERROR_EJECUCION', error_type: error.name })
    }
  }
  fs.writeFileSync(output, observed.map(row => JSON.stringify(row)).join('\n') + '\n')
  fs.writeFileSync(statusPath, JSON.stringify({ status: 'EJECUTADO_PENDIENTE_REVISION', attempted: cases.length }, null, 2))
}

function request(url, options) {
  return fetch(url, { ...options, signal: AbortSignal.timeout(60000) })
}

function answerForParameter(parameter) {
  if (typeof parameter !== 'string' || parameter.trim() === '') {
    throw new Error('La API no indicó el parámetro que se debe responder.')
  }

  const key = parameter.toLowerCase()
  if (key.includes('date') || key.includes('fecha')) return '01/11/2026'
  if (key.includes('amount') || key.includes('price') || key.includes('capital')
      || key.includes('monto') || key.includes('percentage') || key.includes('days')) return '1'
  if (key.startsWith('has') || key.startsWith('is') || key.startsWith('was')) return 'no'
  return 'No aplica para esta consulta de evaluación.'
}
