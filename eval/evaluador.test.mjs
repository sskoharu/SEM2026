import test from 'node:test'
import assert from 'node:assert/strict'
import fs from 'node:fs'
import path from 'node:path'
import { fileURLToPath } from 'node:url'
import { spawnSync } from 'node:child_process'

const directory = path.dirname(fileURLToPath(import.meta.url))
const scratch = path.resolve(directory, '..', '..', 'eval-test-artifacts')
fs.mkdirSync(scratch, { recursive: true })
const cases = fs.readFileSync(path.join(directory, 'casos_rag.jsonl'), 'utf8')
  .split(/\r?\n/).filter(Boolean).map(line => JSON.parse(line))

function run(rows) {
  const workspace = fs.mkdtempSync(path.join(scratch, 'fixture-'))
  const input = path.join(workspace, 'synthetic-observations.jsonl')
  fs.writeFileSync(input, rows.map(row => JSON.stringify(row)).join('\n'))
  const output = path.join(workspace, 'output')
  const processResult = spawnSync(process.execPath, [path.join(directory, 'evaluar_resultados.mjs'), input], {
    encoding: 'utf8', env: { ...process.env, EVAL_OUTPUT_DIR: output },
  })
  const reportPath = path.join(output, 'evaluation.json')
  return { ...processResult, report: fs.existsSync(reportPath) ? JSON.parse(fs.readFileSync(reportPath, 'utf8')) : null }
}

function syntheticRows() {
  return cases.map(item => ({
    id: item.id, response_text: 'Fixture sintético del evaluador; no es respuesta de Gemini.',
    retrieved_source_ids: item.expected_source_ids, cited_source_ids: item.expected_source_ids,
    answer_correct: true, citations_reviewed: true, abstained: true, rejected: true,
  }))
}

test('sin observaciones no acredita ningún umbral y registra los veinte faltantes', () => {
  const result = run([])
  assert.equal(result.status, 0)
  assert.equal(result.report.summary.retrieval.status, 'NO_ACREDITADO')
  assert.equal(result.report.summary.retrieval.percentage, null)
  assert.equal(result.report.validation_warnings.length, 20)
})

test('los IDs duplicados se rechazan en lugar de sustituir observaciones', () => {
  const result = run([{ id: 'rag-001' }, { id: 'rag-001' }])
  assert.notEqual(result.status, 0)
  assert.match(result.stderr, /duplicado/)
  assert.equal(result.report, null)
})

test('una cita adicional no recuperada invalida ese resultado', () => {
  const rows = syntheticRows()
  rows[0].cited_source_ids = [...rows[0].cited_source_ids, 'invented-id']
  const result = run(rows)
  assert.equal(result.status, 0)
  assert.equal(result.report.details[0].answer_with_citation_ok, false)
  assert.equal(result.report.summary.correctAnswerWithCitation.passed, 11)
})

test('sin revisión de citas el porcentaje no se declara acreditado', () => {
  const rows = syntheticRows()
  rows[0].citations_reviewed = false
  const result = run(rows)
  assert.equal(result.report.summary.correctAnswerWithCitation.status, 'NO_ACREDITADO')
  assert.equal(result.report.summary.correctAnswerWithCitation.percentage, null)
})
