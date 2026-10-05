import { useEffect, useState } from 'react'
import type { PropsWithChildren } from 'react'
import { AUTH_TOKEN_KEY } from '../api/client'
import { authApi, profileApi, sessionApi } from '../api/services'
import type { UserProfileResponse } from '../types/api'
import { AuthContext } from './context'

function normalizeUser(user: UserProfileResponse): UserProfileResponse {
  const roles = Array.isArray(user.roles)
    ? [...new Set(user.roles.map((role) => {
        const normalized = role.trim().toUpperCase()
        return normalized.startsWith('ROLE_') ? normalized : `ROLE_${normalized}`
      }))]
    : []

  return { ...user, roles }
}

export function AuthProvider({ children }: PropsWithChildren) {
  const [user, setUser] = useState<UserProfileResponse | null>(null)
  const [isLoading, setIsLoading] = useState(() => Boolean(window.localStorage.getItem(AUTH_TOKEN_KEY)))

  useEffect(() => {
    let active = true
    if (!window.localStorage.getItem(AUTH_TOKEN_KEY)) {
      return () => { active = false }
    }

    profileApi.get()
      .then((profile) => {
        if (active) setUser(normalizeUser(profile))
      })
      .catch(() => {
        sessionApi.logout()
        if (active) setUser(null)
      })
      .finally(() => {
        if (active) setIsLoading(false)
      })

    return () => { active = false }
  }, [])

  async function signIn(username: string, password: string) {
    const response = await authApi.login(username, password)
    const authenticatedUser = normalizeUser(response.user)
    setUser(authenticatedUser)
    return authenticatedUser
  }

  function signOut() {
    sessionApi.logout()
    setUser(null)
  }

  return (
    <AuthContext.Provider value={{ user, isLoading, signIn, signOut }}>
      {children}
    </AuthContext.Provider>
  )
}
