const tokens = new Map<string, string>()

export function rememberSession(id: string, token: string | null): void {
  if (token && /^[A-Za-z0-9_-]{40,128}$/.test(token)) {
    tokens.set(id, token)
  }
}

export function sessionHeaders(id: string): Record<string, string> {
  const token = tokens.get(id)
  return token ? { 'X-Claim-Token': token } : {}
}

export function headersForPath(route: string): Record<string, string> {
  const id = route.match(/[a-fA-F0-9-]{36}/)?.[0]
  return id ? sessionHeaders(id) : {}
}

export function forgetSessions(): void {
  tokens.clear()
}
