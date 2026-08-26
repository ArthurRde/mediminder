import { createContext, useContext, useState, type ReactNode } from 'react'
import { api, setToken, USER_KEY } from '../api/client'
import type { AuthResponse, UserDto } from '../api/types'

interface AuthContextValue {
  user: UserDto | null
  login: (email: string, password: string) => Promise<void>
  register: (name: string, email: string, password: string) => Promise<void>
  logout: () => void
}

const AuthContext = createContext<AuthContextValue | null>(null)

function loadStoredUser(): UserDto | null {
  const raw = localStorage.getItem(USER_KEY)
  return raw ? (JSON.parse(raw) as UserDto) : null
}

export function AuthProvider({ children }: { children: ReactNode }) {
  const [user, setUser] = useState<UserDto | null>(loadStoredUser)

  const applyAuth = (response: AuthResponse) => {
    setToken(response.token)
    localStorage.setItem(USER_KEY, JSON.stringify(response.user))
    setUser(response.user)
  }

  const login = async (email: string, password: string) => {
    applyAuth(await api.post<AuthResponse>('/auth/login', { email, password }))
  }

  const register = async (name: string, email: string, password: string) => {
    applyAuth(await api.post<AuthResponse>('/auth/register', { name, email, password }))
  }

  const logout = () => {
    setToken(null)
    localStorage.removeItem(USER_KEY)
    setUser(null)
  }

  return (
    <AuthContext.Provider value={{ user, login, register, logout }}>
      {children}
    </AuthContext.Provider>
  )
}

export function useAuth(): AuthContextValue {
  const value = useContext(AuthContext)
  if (!value) throw new Error('useAuth braucht einen AuthProvider')
  return value
}
