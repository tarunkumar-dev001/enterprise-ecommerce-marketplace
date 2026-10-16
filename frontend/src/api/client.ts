import axios from 'axios'

export const AUTH_TOKEN_KEY = 'marketplace_access_token'

// Normalize URL: Ensure no trailing slashes before appending /api
const rawBaseUrl = import.meta.env.VITE_API_BASE_URL || ''
const cleanBaseUrl = rawBaseUrl.replace(/\/+$/, '')
const API_BASE_URL = cleanBaseUrl ? `${cleanBaseUrl}/api` : '/api'

export const apiClient = axios.create({
  baseURL: API_BASE_URL,
  headers: { 'Content-Type': 'application/json' },
})

apiClient.interceptors.request.use((config) => {
  const token = window.localStorage.getItem(AUTH_TOKEN_KEY)
  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
})

// Interceptor to reject HTML responses returned as HTTP 200 (Netlify SPA fallback)
apiClient.interceptors.response.use((response) => {
  if (
    typeof response.data === 'string' &&
    (response.data.trim().startsWith('<!DOCTYPE html>') || response.data.trim().startsWith('<html'))
  ) {
    return Promise.reject(
      new Error('Received HTML instead of JSON. Check your API base URL configuration.')
    )
  }
  return response
})