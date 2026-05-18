const TOKEN_KEY = 'scada.accessToken'
const REFRESH_TOKEN_KEY = 'scada.refreshToken'
const REFRESH_PATH = '/api/auth/refresh'
const AUTH_REFRESH_EXCLUDED_PATHS = new Set(['/api/auth/login', '/api/auth/signup', REFRESH_PATH])

let refreshTokenRequest = null

export function getAccessToken() {
  return localStorage.getItem(TOKEN_KEY)
}

function getRefreshToken() {
  return localStorage.getItem(REFRESH_TOKEN_KEY)
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

async function parseResponseBody(response) {
  const contentType = response.headers.get('content-type') || ''
  return contentType.includes('application/json') ? await response.json() : await response.text()
}

function createApiError(response, body) {
  const message = typeof body === 'string' ? body : body.message || body.error || 'API 요청에 실패했습니다.'
  const error = new Error(message)
  error.status = response.status
  return error
}

async function refreshTokens() {
  const accessToken = getAccessToken()
  const refreshToken = getRefreshToken()

  if (!accessToken || !refreshToken) {
    throw createApiError({ status: 401 }, { message: 'Authentication required.' })
  }

  const response = await fetch(REFRESH_PATH, {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
      Authorization: `Bearer ${accessToken}`,
      'X-Refresh-Token': refreshToken,
    },
  })
  const body = response.status === 204 ? null : await parseResponseBody(response)

  if (!response.ok) {
    clearTokens()
    throw createApiError(response, body)
  }

  saveTokens(body)
  return body
}

function getSharedRefreshTokenRequest() {
  if (!refreshTokenRequest) {
    refreshTokenRequest = refreshTokens().finally(() => {
      refreshTokenRequest = null
    })
  }
  return refreshTokenRequest
}

function shouldRefresh(path, response, retryOnUnauthorized, tokenUsed) {
  return (
    retryOnUnauthorized &&
    response.status === 401 &&
    tokenUsed &&
    getRefreshToken() &&
    !AUTH_REFRESH_EXCLUDED_PATHS.has(path)
  )
}

export async function apiFetch(path, options = {}, retryOnUnauthorized = true) {
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

  if (shouldRefresh(path, response, retryOnUnauthorized, token)) {
    const latestToken = getAccessToken()
    if (latestToken && latestToken !== token) {
      return apiFetch(path, options, false)
    }

    await getSharedRefreshTokenRequest()
    return apiFetch(path, options, false)
  }

  if (response.status === 204) {
    return null
  }

  const body = await parseResponseBody(response)

  if (!response.ok) {
    throw createApiError(response, body)
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
  energyFacilityDetail: (params) => apiFetch(`/api/energy/facility-detail${toQuery(params)}`),
  energyFacilityLine: (params) => apiFetch(`/api/energy/facility-line${toQuery(params)}`),
  peakDashboard: (params) => apiFetch(`/api/energy/peak-dashboard${toQuery(params)}`),
  utilityDashboard: (params) => apiFetch(`/api/energy/utility-dashboard${toQuery(params)}`),
  latestEnergy: (plantId, facilityId) => apiFetch(`/api/energy/latest/plants/${plantId}/facilities/${facilityId}`),
  alarms: (params) => apiFetch(`/api/alarms${toQuery(params)}`),
  resolveAlarm: (alarmId) => apiFetch(`/api/alarms/${alarmId}/resolve`, { method: 'PATCH' }),
  esgScores: (params) => apiFetch(`/api/esg/scores${toQuery(params)}`),
  esgEnvironmentDashboard: (params) => apiFetch(`/api/esg/environment-dashboard${toQuery(params)}`),
  users: (params) => apiFetch(`/api/users${toQuery(params)}`),
  chatbotMessages: (params) => apiFetch(`/api/chatbot/messages${toQuery(params)}`),
  askChatbot: (payload) =>
    apiFetch('/api/chatbot/messages', {
      method: 'POST',
      body: JSON.stringify(payload),
    }),
}
