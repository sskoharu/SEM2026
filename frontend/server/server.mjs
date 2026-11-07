import http from 'node:http'
import { createReadStream, existsSync, statSync } from 'node:fs'
import { resolve, extname, sep } from 'node:path'
import { fileURLToPath } from 'node:url'
import { pathToFileURL } from 'node:url'
import { GoogleAuth } from 'google-auth-library'

const uuid = '[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}'
const routes = [
  /^\/api\/v1\/claims\/start$/,
  new RegExp(`^/api/v1/claims/${uuid}/layer1/clarify$`),
  new RegExp(`^/api/v1/claims/${uuid}/layer2/(start|answer)$`),
  new RegExp(`^/api/v1/claims/(evaluate|accept|select-action|submit-procedural-form)/${uuid}$`),
  new RegExp(`^/api/v1/claims/${uuid}/layer4/calculate$`),
  new RegExp(`^/api/v1/claims/${uuid}/layer5/(preview|generate)$`),
]
const types = {
  '.html': 'text/html; charset=utf-8', '.js': 'text/javascript; charset=utf-8',
  '.css': 'text/css; charset=utf-8', '.json': 'application/json; charset=utf-8',
  '.svg': 'image/svg+xml', '.png': 'image/png', '.jpg': 'image/jpeg',
  '.ico': 'image/x-icon', '.woff2': 'font/woff2',
}
const docx = 'application/vnd.openxmlformats-officedocument.wordprocessingml.document'
const distDirectory = resolve(fileURLToPath(new URL('.', import.meta.url)), '../dist')

function json(res, status, message) {
  res.writeHead(status, {
    'Content-Type': 'application/json; charset=utf-8',
    'Cache-Control': 'no-store', 'X-Content-Type-Options': 'nosniff',
  })
  res.end(JSON.stringify({ error: message }))
}

async function bodyBytes(req, max = 131072) {
  const chunks = []
  let size = 0
  for await (const chunk of req) {
    size += chunk.length
    if (size > max) {
      const error = new Error('Payload too large')
      error.status = 413
      throw error
    }
    chunks.push(chunk)
  }
  return Buffer.concat(chunks)
}

export function createFrontendServer({
  backendUrl = process.env.BACKEND_URL?.replace(/\/+$/, ''),
  getIdentityToken = async (audience) => {
    const client = await new GoogleAuth().getIdTokenClient(audience)
    const headers = await client.getRequestHeaders(audience)
    return headers.get('Authorization')
  },
  fetchBackend = fetch,
  now = () => Date.now(),
} = {}) {
  const limits = new Map()

  function rateLimited(req, isStart) {
    // Cloud Run normally appends a peer address. This is a provisional,
    // per-instance limiter, not a trusted global client identity mechanism.
    const forwarded = req.headers['x-forwarded-for']
    const parts = typeof forwarded === 'string' ? forwarded.split(',') : []
    const key = parts.length >= 2 ? parts[parts.length - 2].trim() : req.socket.remoteAddress || 'unknown'
    const time = now()
    if (limits.size > 10000) {
      for (const [ip, entry] of limits) if (entry.until <= time) limits.delete(ip)
    }
    const prior = limits.get(key)
    const entry = prior && prior.until > time ? prior : { until: time + 60000, count: 0, starts: 0 }
    entry.count += 1
    if (isStart) entry.starts += 1
    limits.set(key, entry)
    return entry.count > 30 || entry.starts > 5
  }

  async function proxy(req, res, path) {
    if (req.method !== 'POST') return json(res, 405, 'Método no permitido.')
    if (!routes.some((route) => route.test(path))) return json(res, 404, 'Ruta no disponible.')
    const isStart = path === '/api/v1/claims/start'
    const claimToken = req.headers['x-claim-token']
    if (!isStart && (typeof claimToken !== 'string' || !/^[A-Za-z0-9_-]{40,128}$/.test(claimToken))) {
      return json(res, 403, 'Token de sesión requerido.')
    }
    if (rateLimited(req, isStart)) return json(res, 429, 'Demasiadas solicitudes. Intente más tarde.')
    if (!backendUrl || !/^https:\/\/[^/?#]+$/.test(backendUrl)) return json(res, 503, 'Backend no configurado.')
    if (!/^application\/json(?:\s*;|$)/i.test(req.headers['content-type'] || '')) return json(res, 415, 'Se requiere JSON.')
    if (Number(req.headers['content-length']) > 131072) return json(res, 413, 'Solicitud demasiado grande.')

    let body
    try {
      body = await bodyBytes(req)
      JSON.parse(body.toString('utf8'))
    } catch (error) {
      return json(res, error.status || 400, error.status === 413 ? 'Solicitud demasiado grande.' : 'JSON inválido.')
    }

    try {
      const identityToken = await getIdentityToken(backendUrl)
      if (!identityToken || !/^Bearer\s+\S+$/i.test(identityToken)) throw new Error('Identity token unavailable')
      const headers = { 'Content-Type': 'application/json', 'X-Serverless-Authorization': identityToken }
      if (!isStart) headers['X-Claim-Token'] = claimToken
      if (req.headers.accept === docx) headers.Accept = docx
      const upstream = await fetchBackend(`${backendUrl}${path}`, {
        method: 'POST', headers, body, redirect: 'manual', signal: AbortSignal.timeout(290000),
      })
      const responseHeaders = { 'Cache-Control': 'no-store', 'X-Content-Type-Options': 'nosniff' }
      for (const name of ['content-type', 'content-disposition', 'x-claim-token']) {
        const value = upstream.headers.get(name)
        if (value) responseHeaders[name] = value
      }
      res.writeHead(upstream.status, responseHeaders)
      if (upstream.body) for await (const chunk of upstream.body) res.write(chunk)
      res.end()
    } catch {
      if (!res.headersSent) json(res, 502, 'No fue posible comunicarse con el servicio.')
      else res.destroy()
    }
  }

  return http.createServer(async (req, res) => {
    const url = req.url || ''
    const rawPath = url.split('?')[0]
    if (!rawPath.startsWith('/') || rawPath.includes('%') || rawPath.includes('\\') ||
        rawPath.includes('//') || rawPath.split('/').includes('..')) {
      return json(res, 400, 'Ruta no válida.')
    }
    if (rawPath.startsWith('/api/')) {
      if (url.includes('?')) return json(res, 400, 'No se permiten parámetros en la API.')
      return proxy(req, res, rawPath)
    }
    if (rawPath === '/health') return json(res, 200, 'ok')
    if (req.method !== 'GET' && req.method !== 'HEAD') return json(res, 405, 'Método no permitido.')

    const requested = resolve(distDirectory, `.${rawPath}`)
    if (requested !== distDirectory && !requested.startsWith(distDirectory + sep)) {
      return json(res, 403, 'Acceso denegado.')
    }
    const exists = existsSync(requested) && statSync(requested).isFile()
    if (!exists && extname(rawPath)) return json(res, 404, 'Archivo no encontrado.')
    const file = exists ? requested : resolve(distDirectory, 'index.html')
    if (!existsSync(file)) return json(res, 503, 'Frontend no compilado.')
    res.writeHead(200, {
      'Content-Type': types[extname(file)] || 'application/octet-stream',
      'Cache-Control': file.endsWith('.html') ? 'no-store' : 'public, max-age=3600',
      'X-Content-Type-Options': 'nosniff',
    })
    if (req.method === 'HEAD') return res.end()
    createReadStream(file).on('error', () => res.destroy()).pipe(res)
  })
}

if (process.argv[1] && import.meta.url === pathToFileURL(resolve(process.argv[1])).href) {
  const port = Number(process.env.PORT || 8080)
  const server = createFrontendServer()
  server.on('error', (error) => {
    console.error(`No se pudo iniciar el servidor: ${error.code}`)
    process.exitCode = 1
  })
  server.listen(port, '0.0.0.0', () => console.log(`SEM2026 frontend escuchando en el puerto ${port}`))
}
