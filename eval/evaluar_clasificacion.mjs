import fs from 'node:fs'
import path from 'node:path'
import { fileURLToPath } from 'node:url'

const directory = path.dirname(fileURLToPath(import.meta.url))
if (!process.argv[2]) {
  console.error('Uso: node eval/evaluar_clasificacion.mjs archivo-observaciones.jsonl')
  process.exit(1)
}
const read = file => fs.readFileSync(file, 'utf8').split(/\r?\n/).filter(Boolean).map(line => JSON.parse(line))
const expected = read(path.join(directory, 'casos_clasificacion.jsonl'))
const observations = read(path.resolve(process.argv[2]))
const index = new Map()
for (const row of observations) {
  if (typeof row.id !== 'string' || index.has(row.id) || !expected.some(item => item.id === row.id)) {
    throw new Error('Identificador duplicado o desconocido en clasificación.')
  }
  index.set(row.id, row)
}
const details = expected.map(item => {
  const row = index.get(item.id)
  const observed = typeof row?.observed_claim_type === 'string' && typeof row?.response_text === 'string'
  return { id: item.id, expected: item.expected_claim_type, observed: observed ? row.observed_claim_type : null,
    passed: observed ? row.observed_claim_type === item.expected_claim_type : null }
})
const complete = expected.length >= 10 && details.every(row => row.passed !== null)
const passed = details.filter(row => row.passed === true).length
const percentage = complete ? passed * 100 / expected.length : null
const report = { scope: 'Clasificación del agente con observaciones reales', expected: expected.length,
  observed: details.filter(row => row.passed !== null).length, passed, percentage,
  target: 80, status: !complete ? 'NO_ACREDITADO' : percentage >= 80 ? 'CUMPLE' : 'NO_CUMPLE', details }
const output = path.resolve(directory, '../docs/evidencias/semana-3/resultados/clasificacion.json')
fs.writeFileSync(output, JSON.stringify(report, null, 2) + '\n')
console.log(JSON.stringify({ status: report.status, percentage: report.percentage }))
