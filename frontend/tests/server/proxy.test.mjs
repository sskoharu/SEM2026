
import test from 'node:test'
import assert from 'node:assert/strict'
import { createFrontendServer } from '../../server/server.mjs'

const backendUrl = 'https://backend.example.run.app'
const sessionId = '12345678-1234-1234-1234-123456789abc'
const claimToken = 'A'.repeat(43)
const docx =
  'application/vnd.openxmlformats-officedocument.wordprocessingml.document'

async function withServer(run, overrides = {}) {
  const calls = []
  const { fetchBackend: customFetchBackend, ...settings } = overrides

  const server = createFrontendServer({
    backendUrl,
    getIdentityToken: async () => 'Bearer simulated-token',
    ...settings,

    fetchBackend: async (url, options) => {
      calls.push({ url, options })

      if (customFetchBackend) {
        return customFetchBackend(url, options)
      }

      return new Response(JSON.stringify({ ok: true }), {
        status: 200,
        headers: {
          'content-type': 'application/json',
          'x-claim-token': claimToken,
        },
      })
    },
  })

  await new Promise((resolve) => {
    server.listen(0, '127.0.0.1', resolve)
  })

  try {
    const url = `http://127.0.0.1:${server.address().port}`
    await run(url, calls)
  } finally {
    await new Promise((resolve, reject) => {
      server.close((error) => {
        if (error) reject(error)
        else resolve()
      })
    })
  }
}

function post(url, path, { headers = {}, body = '{}' } = {}) {
  return fetch(`${url}${path}`, {
    method: 'POST',
    headers: {
      'content-type': 'application/json',
      ...headers,
    },
    body,
  })
}

test('rechaza endpoint no autorizado sin llamar al backend', async () => {
  await withServer(async (url, calls) => {
    const response = await post(
      url,
      `/api/v1/claims/batch-update/${sessionId}`,
    )

    assert.equal(response.status, 404)
    assert.equal(calls.length, 0)
  })
})

test('rechaza token inválido y ausente antes de obtener IAM', async () => {
  let authCalls = 0

  await withServer(async (url, calls) => {
    for (const headers of [
      {},
      { 'x-claim-token': 'invalido' },
    ]) {
      const response = await post(
        url,
        `/api/v1/claims/${sessionId}/layer1/clarify`,
        { headers },
      )

      assert.equal(response.status, 403)
    }

    assert.equal(authCalls, 0)
    assert.equal(calls.length, 0)
  }, {
    getIdentityToken: async () => {
      authCalls++
      return 'Bearer simulated-token'
    },
  })
})

test('rechaza método, parámetros, content-type, JSON y tamaño', async () => {
  await withServer(async (url, calls) => {
    const getResponse = await fetch(`${url}/api/v1/claims/start`)
    assert.equal(getResponse.status, 405)

    const queryResponse = await post(
      url,
      '/api/v1/claims/start?x=1',
    )
    assert.equal(queryResponse.status, 400)

    const contentTypeResponse = await post(
      url,
      '/api/v1/claims/start',
      {
        headers: {
          'content-type': 'text/plain',
        },
      },
    )
    assert.equal(contentTypeResponse.status, 415)

    const invalidJsonResponse = await post(
      url,
      '/api/v1/claims/start',
      { body: '{' },
    )
    assert.equal(invalidJsonResponse.status, 400)

    const largeBodyResponse = await post(
      url,
      '/api/v1/claims/start',
      {
        body: JSON.stringify({
          a: 'x'.repeat(131072),
        }),
      },
    )
    assert.equal(largeBodyResponse.status, 413)

    assert.equal(calls.length, 0)
  })
})

test('limita inicios y no envía solicitudes rechazadas al backend', async () => {
  await withServer(async (url, calls) => {
    for (let i = 0; i < 5; i++) {
      const response = await post(
        url,
        '/api/v1/claims/start',
        { body: '{' },
      )

      assert.equal(response.status, 400)
    }

    const limitedResponse = await post(
      url,
      '/api/v1/claims/start',
      { body: '{' },
    )

    assert.equal(limitedResponse.status, 429)
    assert.equal(calls.length, 0)
  })
})

test('envía IAM solo desde el servidor y preserva el token de sesión', async () => {
  await withServer(async (url, calls) => {
    const response = await post(
      url,
      `/api/v1/claims/${sessionId}/layer1/clarify`,
      {
        headers: {
          'x-claim-token': claimToken,
          authorization: 'Bearer forged-client-token',
        },
      },
    )

    assert.equal(response.status, 200)
    assert.equal(
      response.headers.get('x-claim-token'),
      claimToken,
    )

    assert.equal(calls.length, 1)

    assert.equal(
      calls[0].url,
      `${backendUrl}/api/v1/claims/${sessionId}/layer1/clarify`,
    )

    assert.equal(
      calls[0].options.headers['X-Serverless-Authorization'],
      'Bearer simulated-token',
    )

    assert.equal(
      calls[0].options.headers['X-Claim-Token'],
      claimToken,
    )

    assert.equal(
      calls[0].options.headers.Authorization,
      undefined,
    )
  })
})

test('preserva bytes y cabeceras DOCX', async () => {
  const bytes = Uint8Array.from([
    0x50, 0x4b, 0x03, 0x04, 0, 255, 12,
  ])

  await withServer(async (url, calls) => {
    const response = await post(
      url,
      `/api/v1/claims/${sessionId}/layer5/generate`,
      {
        headers: {
          'x-claim-token': claimToken,
          accept: docx,
        },
      },
    )

    assert.equal(response.status, 200)
    assert.equal(
      response.headers.get('content-type'),
      docx,
    )

    assert.equal(
      response.headers.get('content-disposition'),
      'attachment; filename="reclamo.docx"',
    )

    assert.deepEqual(
      new Uint8Array(await response.arrayBuffer()),
      bytes,
    )

    assert.equal(calls.length, 1)
    assert.equal(
      calls[0].options.headers.Accept,
      docx,
    )
  }, {
    fetchBackend: async () => {
      return new Response(bytes, {
        status: 200,
        headers: {
          'content-type': docx,
          'content-disposition':
            'attachment; filename="reclamo.docx"',
        },
      })
    },
  })
})

test('falla de IAM responde 502 sin llamar al backend', async () => {
  await withServer(async (url, calls) => {
    const response = await post(
      url,
      '/api/v1/claims/start',
    )

    assert.equal(response.status, 502)
    assert.equal(calls.length, 0)

    assert.doesNotMatch(
      await response.text(),
      /credential|secret|stack/i,
    )
  }, {
    getIdentityToken: async () => {
      throw new Error('private credential failure')
    },
  })
})
