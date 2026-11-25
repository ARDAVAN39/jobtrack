import type {
  ApplicationDetail,
  ApplicationFields,
  ApplicationStatus,
  ApplicationSummary,
  AuthResponse,
  Page,
  Stats,
  User,
} from './types'

const TOKEN_KEY = 'jobtrack.token'

/** An error answer from the API, with the field errors when the server sent them. */
export class ApiError extends Error {
  readonly status: number
  readonly fieldErrors: Record<string, string>

  constructor(status: number, message: string, fieldErrors: Record<string, string> = {}) {
    super(message)
    this.name = 'ApiError'
    this.status = status
    this.fieldErrors = fieldErrors
  }
}

export const tokenStore = {
  get: (): string | null => localStorage.getItem(TOKEN_KEY),
  set: (token: string) => localStorage.setItem(TOKEN_KEY, token),
  clear: () => localStorage.removeItem(TOKEN_KEY),
}

/** Called when the server rejects the token, so the app can send the user back to the login page. */
let onUnauthorized: () => void = () => {}

export function setUnauthorizedHandler(handler: () => void) {
  onUnauthorized = handler
}

export async function request<T>(path: string, init: RequestInit = {}): Promise<T> {
  const headers = new Headers(init.headers)
  if (init.body !== undefined) {
    headers.set('Content-Type', 'application/json')
  }
  const token = tokenStore.get()
  if (token) {
    headers.set('Authorization', `Bearer ${token}`)
  }

  const response = await fetch(path, { ...init, headers })

  if (response.status === 401 && token) {
    tokenStore.clear()
    onUnauthorized()
  }
  if (!response.ok) {
    throw await toApiError(response)
  }
  if (response.status === 204) {
    return undefined as T
  }
  return (await response.json()) as T
}

async function toApiError(response: Response): Promise<ApiError> {
  try {
    // Spring answers with RFC 9457 problem details: { title, detail, errors? }
    const problem = (await response.json()) as { detail?: string; title?: string; errors?: Record<string, string> }
    return new ApiError(response.status, problem.detail ?? problem.title ?? response.statusText, problem.errors ?? {})
  } catch {
    return new ApiError(response.status, response.statusText || 'Request failed')
  }
}

/** Empty form fields are sent as null, so the API stores "not set" rather than "". */
function toPayload(fields: ApplicationFields) {
  const orNull = (value: string) => (value.trim() === '' ? null : value.trim())
  return {
    company: fields.company.trim(),
    roleTitle: fields.roleTitle.trim(),
    location: orNull(fields.location),
    jobUrl: orNull(fields.jobUrl),
    appliedOn: orNull(fields.appliedOn),
    notes: orNull(fields.notes),
  }
}

export const api = {
  register: (email: string, password: string, displayName: string) =>
    request<AuthResponse>('/api/auth/register', {
      method: 'POST',
      body: JSON.stringify({ email, password, displayName }),
    }),

  login: (email: string, password: string) =>
    request<AuthResponse>('/api/auth/login', { method: 'POST', body: JSON.stringify({ email, password }) }),

  me: () => request<User>('/api/me'),

  listApplications: (params: { status?: ApplicationStatus | ''; q?: string; page?: number }) => {
    const query = new URLSearchParams()
    if (params.status) query.set('status', params.status)
    if (params.q?.trim()) query.set('q', params.q.trim())
    query.set('page', String(params.page ?? 0))
    return request<Page<ApplicationSummary>>(`/api/applications?${query}`)
  },

  getApplication: (id: number) => request<ApplicationDetail>(`/api/applications/${id}`),

  createApplication: (fields: ApplicationFields, status: ApplicationStatus) =>
    request<ApplicationDetail>('/api/applications', {
      method: 'POST',
      body: JSON.stringify({ ...toPayload(fields), status }),
    }),

  updateApplication: (id: number, fields: ApplicationFields, version: number) =>
    request<ApplicationDetail>(`/api/applications/${id}`, {
      method: 'PUT',
      body: JSON.stringify({ ...toPayload(fields), version }),
    }),

  changeStatus: (id: number, status: ApplicationStatus, note: string) =>
    request<ApplicationDetail>(`/api/applications/${id}/status`, {
      method: 'PATCH',
      body: JSON.stringify({ status, note: note.trim() || null }),
    }),

  deleteApplication: (id: number) => request<void>(`/api/applications/${id}`, { method: 'DELETE' }),

  stats: () => request<Stats>('/api/applications/stats'),
}
