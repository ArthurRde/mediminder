import { createContext, useCallback, useContext, useEffect, useState, type ReactNode } from 'react'
import { api } from '../api/client'
import type { CircleSummary } from '../api/types'

interface CircleContextValue {
  circle: CircleSummary | null
  isAdmin: boolean
  loading: boolean
  error: string | null
  refresh: () => Promise<void>
}

const CircleContext = createContext<CircleContextValue | null>(null)

export function CircleProvider({ children }: { children: ReactNode }) {
  const [circle, setCircle] = useState<CircleSummary | null>(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)

  // Fehler nicht schlucken, sonst leitet circle=null fälschlich ins Onboarding
  const refresh = useCallback(async () => {
    setLoading(true)
    try {
      const circles = await api.get<CircleSummary[]>('/circles')
      setCircle(circles[0] ?? null)
      setError(null)
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Pflegekreis konnte nicht geladen werden.')
    } finally {
      setLoading(false)
    }
  }, [])

  useEffect(() => {
    refresh()
  }, [refresh])

  return (
    <CircleContext.Provider value={{ circle, isAdmin: circle?.role === 'ADMIN', loading, error, refresh }}>
      {children}
    </CircleContext.Provider>
  )
}

export function useCircle(): CircleContextValue {
  const value = useContext(CircleContext)
  if (!value) throw new Error('useCircle braucht einen CircleProvider')
  return value
}
