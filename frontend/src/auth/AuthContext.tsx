import { useCallback, useEffect, useMemo, useState, type ReactNode } from 'react'
import { useQueryClient } from '@tanstack/react-query'
import { api, setUnauthorizedHandler, tokenStore } from '../api/client'
import type { AuthResponse, User } from '../api/types'
import { AuthContext, type AuthState } from './useAuth'

export function AuthProvider({ children }: { children: ReactNode }) {
  const queryClient = useQueryClient()
  const [user, setUser] = useState<User | null>(null)
  const [loading, setLoading] = useState(() => tokenStore.get() !== null)

  const logout = useCallback(() => {
    tokenStore.clear()
    setUser(null)
    // nothing cached for one user may be shown to the next
    queryClient.clear()
  }, [queryClient])

  useEffect(() => {
    setUnauthorizedHandler(logout)
  }, [logout])

  // A token from an earlier visit may have expired: check it before trusting it.
  useEffect(() => {
    if (!tokenStore.get()) return
    api
      .me()
      .then(setUser)
      .catch(() => tokenStore.clear())
      .finally(() => setLoading(false))
  }, [])

  const signedIn = useCallback((response: AuthResponse) => {
    tokenStore.set(response.token)
    setUser(response.user)
  }, [])

  const value = useMemo<AuthState>(
    () => ({
      user,
      loading,
      login: async (email, password) => signedIn(await api.login(email, password)),
      register: async (email, password, displayName) => signedIn(await api.register(email, password, displayName)),
      logout,
    }),
    [user, loading, signedIn, logout],
  )

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>
}
