const TOKEN_KEY = 'scada.accessToken'
const REFRESH_TOKEN_KEY = 'scada.refreshToken'

export function getAccessToken() {
  return localStorage.getItem(TOKEN_KEY)
}

export function saveTokens(response) {
  if (response.accessToken) {
    localStorage.setItem(TOKEN_KEY, response.accessToken)
  }
  if (response.refreshToken) {
    localStorage.setItem(REFRESH_TOKEN_KEY, response.refreshToken)
  }
}

export function clearTokens() {
  localStorage.removeItem(TOKEN_KEY)
  localStorage.removeItem(REFRESH_TOKEN_KEY)
}

function toQuery(params = {}) {
  const search = new URLSearchParams()
  Object.entries(params).forEach(([key, value]) => {
    if (value !== undefined && value !== null && value !== '') {
      search.set(key, value)
    }
  })
  const query = search.toString()
  return query ? `?${query}` : ''
}

export async function apiFetch(path, options = {}) {
  const headers = {
    'Content-Type': 'application/json',
    ...(options.headers || {}),
  }
  const token = getAccessToken()
  if (token) {
    headers.Authorization = `Bearer ${token}`
  }

  const response = await fetch(path, {
    ...options,
    headers,
  })

  if (response.status === 204) {
    return null
  }

  const contentType = response.headers.get('content-type') || ''
  const body = contentType.includes('application/json') ? await response.json() : await response.text()

  if (!response.ok) {
    const message = typeof body === 'string' ? body : body.message || body.error || 'API 요청에 실패했습니다.'
    throw new Error(message)
  }

  return body
}

export const api = {
  login: (payload) =>
    apiFetch('/api/auth/login', {
      method: 'POST',
      body: JSON.stringify(payload),
    }),
  logout: () => apiFetch('/api/auth/logout', { method: 'POST' }),
  me: () => apiFetch('/api/users/me'),
  plants: () => apiFetch('/api/plants'),
  facilities: (plantId) => apiFetch(`/api/plants/${plantId}/facilities`),
  dashboard: (plantId) => apiFetch(`/api/dashboard/overview${toQuery({ plantId })}`),
  energySummaries: (params) => apiFetch(`/api/energy/summaries${toQuery(params)}`),
  energyMeasurements: (params) => apiFetch(`/api/energy/measurements${toQuery(params)}`),
  latestEnergy: (plantId, facilityId) => apiFetch(`/api/energy/latest/plants/${plantId}/facilities/${facilityId}`),
  alarms: (params) => apiFetch(`/api/alarms${toQuery(params)}`),
  resolveAlarm: (alarmId) => apiFetch(`/api/alarms/${alarmId}/resolve`, { method: 'PATCH' }),
  esgScores: (params) => apiFetch(`/api/esg/scores${toQuery(params)}`),
  users: (params) => apiFetch(`/api/users${toQuery(params)}`),
}
