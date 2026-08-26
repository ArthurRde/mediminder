import { useCallback, useEffect, useState } from 'react'
import { api } from '../api/client'
import type { CircleDetail } from '../api/types'
import { useAuth } from '../state/AuthContext'
import { useCircle } from '../state/CircleContext'
import Avatar from '../components/Avatar'

export default function CirclePage() {
  const { circle, isAdmin } = useCircle()
  const { user, logout } = useAuth()
  const [detail, setDetail] = useState<CircleDetail | null>(null)
  const [copied, setCopied] = useState(false)
  const [error, setError] = useState<string | null>(null)
  const [confirmingId, setConfirmingId] = useState<number | null>(null)

  const load = useCallback(async () => {
    setDetail(await api.get<CircleDetail>(`/circles/${circle!.id}`))
  }, [circle])

  useEffect(() => {
    load().catch(() => undefined)
  }, [load])

  const copyInviteLink = async () => {
    setError(null)
    try {
      const invite = await api.post<{ inviteToken: string }>(`/circles/${circle!.id}/invite`)
      await navigator.clipboard.writeText(`${window.location.origin}/join/${invite.inviteToken}`)
      setCopied(true)
      setTimeout(() => setCopied(false), 3000)
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Konnte Link nicht kopieren')
    }
  }

  // Erster Klick fragt nach, erst der zweite entfernt wirklich
  const removeMember = async (userId: number) => {
    if (confirmingId !== userId) {
      setConfirmingId(userId)
      return
    }
    setConfirmingId(null)
    setError(null)
    try {
      await api.delete(`/circles/${circle!.id}/members/${userId}`)
      await load()
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Entfernen fehlgeschlagen')
    }
  }

  if (!detail) return <p>Lade Pflegekreis…</p>

  return (
    <>
      <header className="page-header">
        <h1>{detail.name}</h1>
        {detail.patient && (
          <p className="muted">
            Pflege für {detail.patient.name}
            {detail.patient.birthYear ? ` (Jahrgang ${detail.patient.birthYear})` : ''}
          </p>
        )}
      </header>

      {error && <p className="error">{error}</p>}

      <h2>Mitglieder</h2>
      {detail.members.map((member) => (
        <div className="card row" key={member.userId}>
          <Avatar name={member.name} />
          <div className="grow">
            <strong>{member.name}</strong>
            <span className="muted small">{member.email}</span>
          </div>
          <span className={`badge ${member.role === 'ADMIN' ? 'badge-admin' : ''}`}>
            {member.role === 'ADMIN' ? 'Admin' : 'Mitglied'}
          </span>
          {isAdmin && member.userId !== user?.id && (
            <button className="btn btn-ghost danger" onClick={() => removeMember(member.userId)}>
              {confirmingId === member.userId ? 'Wirklich entfernen?' : 'Entfernen'}
            </button>
          )}
        </div>
      ))}

      {isAdmin && (
        <button className="btn btn-primary wide" onClick={copyInviteLink}>
          {copied ? 'Einladungslink kopiert ✓' : 'Einladungslink kopieren'}
        </button>
      )}

      <button className="btn btn-ghost wide" onClick={logout}>
        Abmelden ({user?.name})
      </button>
    </>
  )
}
