import { Navigate, Outlet, Route, Routes, useLocation } from 'react-router-dom'
import { useAuth } from './state/AuthContext'
import { CircleProvider, useCircle } from './state/CircleContext'
import BottomNav from './components/BottomNav'
import LoginPage from './pages/LoginPage'
import OnboardingPage from './pages/OnboardingPage'
import TodayPage from './pages/TodayPage'
import MedicationsPage from './pages/MedicationsPage'
import CalendarPage from './pages/CalendarPage'
import CirclePage from './pages/CirclePage'
import JoinPage from './pages/JoinPage'

function RequireAuth() {
  const { user } = useAuth()
  const location = useLocation()
  if (!user) {
    return <Navigate to="/login" state={{ from: location.pathname }} replace />
  }
  return (
    <CircleProvider>
      <Outlet />
    </CircleProvider>
  )
}

function TabLayout() {
  const { circle, loading, error, refresh } = useCircle()
  if (loading) {
    return <div className="app centered">Lade…</div>
  }
  if (error) {
    return (
      <div className="app centered">
        <p className="error">{error}</p>
        <button className="btn btn-primary" onClick={() => refresh()}>
          Erneut versuchen
        </button>
      </div>
    )
  }
  if (!circle) {
    return <Navigate to="/onboarding" replace />
  }
  return (
    <div className="app">
      <Outlet />
      <BottomNav />
    </div>
  )
}

export default function App() {
  return (
    <Routes>
      <Route path="/login" element={<LoginPage />} />
      <Route element={<RequireAuth />}>
        <Route path="/onboarding" element={<OnboardingPage />} />
        <Route path="/join/:token" element={<JoinPage />} />
        <Route element={<TabLayout />}>
          <Route path="/" element={<TodayPage />} />
          <Route path="/plan" element={<MedicationsPage />} />
          <Route path="/kalender" element={<CalendarPage />} />
          <Route path="/kreis" element={<CirclePage />} />
        </Route>
      </Route>
      <Route path="*" element={<Navigate to="/" replace />} />
    </Routes>
  )
}
