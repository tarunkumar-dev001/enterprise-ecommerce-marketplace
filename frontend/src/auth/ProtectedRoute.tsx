import type { ReactNode } from 'react'
import { Navigate, useLocation } from 'react-router-dom'
import { useAuth } from './useAuth'

type ProtectedRouteProps = {
  allowedRoles: string[]
  children: ReactNode
}

export function ProtectedRoute({ allowedRoles, children }: ProtectedRouteProps) {
  const { user, isLoading } = useAuth()
  const location = useLocation()

  if (isLoading) {
    return <div className="py-16 text-sm text-stone-600" role="status">Restoring your session…</div>
  }

  if (!user) {
    return <Navigate to="/login" replace state={{ from: location }} />
  }

  if (!allowedRoles.some((role) => user.roles.includes(role))) {
    return <Navigate to="/" replace />
  }

  return children
}