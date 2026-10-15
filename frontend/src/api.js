const configuredApiUrl = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080/api'
export const API_BASE_URL = configuredApiUrl.replace(/\/+$/, '')

export async function apiRequest(path, { token, body, headers = {}, ...options } = {}) {
  const requestHeaders = new Headers(headers)
  if (body !== undefined) requestHeaders.set('Content-Type', 'application/json')
  if (token) requestHeaders.set('Authorization', `Bearer ${token}`)

  const response = await fetch(`${API_BASE_URL}${path}`, {
    ...options,
    headers: requestHeaders,
    body: body === undefined || typeof body === 'string' ? body : JSON.stringify(body),
  })
  const text = await response.text()
  let data = null
  if (text) {
    try {
      data = JSON.parse(text)
    } catch {
      throw new Error('The server returned an invalid response.')
    }
  }

  if (!response.ok) {
    const validationMessages = data && typeof data === 'object'
      ? Object.entries(data).map(([key, value]) => `${key}: ${value}`).join(' ')
      : ''
    throw new Error(data?.error || validationMessages || `Request failed (${response.status}).`)
  }
  return data
}