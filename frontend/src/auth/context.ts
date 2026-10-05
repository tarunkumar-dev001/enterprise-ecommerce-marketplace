import { createContext } from 'react'
import type { UserProfileResponse } from '../types/api'

export type AuthContextValue = {
  user: UserProfileResponse | null
  isLoading: boolean
  signIn: (username: string, password: string) => Promise<UserProfileResponse>
  signOut: () => void
}

export const AuthContext = createContext<AuthContextValue | null>(null)