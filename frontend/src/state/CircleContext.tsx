import { createContext, useCallback, useContext, useEffect, useState, type ReactNode } from 'react'
import { api } from '../api/client'
import type { CircleSummary } from '../api/types'

interface CircleContextValue {
  circle: CircleSummary | null
  isAdmin: boolean
  loading: boolean
  refresh: () => Promise<void>
}

const CircleContext = createContext<CircleContextValue | null>(null)

export function CircleProvider({ children }: { children: ReactNode }) {
  const [circle, setCircle] = useState<CircleSummary | null>(null)
  const [loading, setLoading] = useState(true)

  const refresh = useCallback(async () => {
    const circles = await api.get<CircleSummary[]>('/circles')
    setCircle(circles[0] ?? null)
    setLoading(false)
  }, [])

  useEffect(() => {
    refresh().catch(() => setLoading(false))
  }, [refresh])

  return (
    <CircleContext.Provider value={{ circle, isAdmin: circle?.role === 'ADMIN', loading, refresh }}>
      {children}
    </CircleContext.Provider>
  )
}

export function useCircle(): CircleContextValue {
  const value = useContext(CircleContext)
  if (!value) throw new Error('useCircle braucht einen CircleProvider')
  return value
}
