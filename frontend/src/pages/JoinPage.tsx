import { useEffect, useState } from 'react'
import { useNavigate, useParams } from 'react-router-dom'
import { api } from '../api/client'
import { useCircle } from '../state/CircleContext'

export default function JoinPage() {
  const { token } = useParams()
  const navigate = useNavigate()
  const { refresh } = useCircle()
  const [error, setError] = useState<string | null>(null)

  useEffect(() => {
    api
      .post(`/circles/join/${token}`)
      .then(refresh)
      .then(() => navigate('/', { replace: true }))
      .catch((err: Error) => setError(err.message))
  }, [token, refresh, navigate])

  return (
    <div className="app centered">
      {error ? <p className="error">{error}</p> : <p>Trete dem Pflegekreis bei…</p>}
    </div>
  )
}
