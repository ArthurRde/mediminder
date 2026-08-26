const TOKEN_KEY = 'mediminder.token'
export const USER_KEY = 'mediminder.user'

export function getToken(): string | null {
  return localStorage.getItem(TOKEN_KEY)
}

export function setToken(token: string | null) {
  if (token === null) {
    localStorage.removeItem(TOKEN_KEY)
  } else {
    localStorage.setItem(TOKEN_KEY, token)
  }
}

export class ApiError extends Error {
  status: number
  body: Record<string, unknown>

  constructor(status: number, body: Record<string, unknown> | undefined) {
    super(typeof body?.message === 'string' ? body.message : `Fehler ${status}`)
    this.status = status
    this.body = body ?? {}
  }
}

async function request<T>(method: string, path: string, body?: unknown): Promise<T> {
  const headers: Record<string, string> = {}
  const token = getToken()
  if (token) headers.Authorization = `Bearer ${token}`
  if (body !== undefined) headers['Content-Type'] = 'application/json'

  let response: Response
  try {
    response = await fetch(`/api${path}`, {
      method,
      headers,
      body: body === undefined ? undefined : JSON.stringify(body),
    })
  } catch {
    throw new ApiError(0, { message: 'Server nicht erreichbar. Bitte Verbindung prüfen.' })
  }

  const text = await response.text()
  let data: Record<string, unknown> | undefined
  try {
    data = text ? JSON.parse(text) : undefined
  } catch {
    data = undefined
  }
  if (!response.ok) {
    // Session abgelaufen, zurück zum Login. Bei /auth heißt 401 nur falsche Zugangsdaten.
    if (response.status === 401 && !path.startsWith('/auth')) {
      setToken(null)
      localStorage.removeItem(USER_KEY)
      window.location.assign('/login')
    }
    throw new ApiError(response.status, data)
  }
  return data as T
}

export const api = {
  get: <T>(path: string) => request<T>('GET', path),
  post: <T>(path: string, body?: unknown) => request<T>('POST', path, body),
  put: <T>(path: string, body?: unknown) => request<T>('PUT', path, body),
  delete: <T = void>(path: string) => request<T>('DELETE', path),
}
