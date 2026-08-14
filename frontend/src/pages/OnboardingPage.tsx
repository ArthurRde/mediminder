import { useState, type FormEvent } from 'react'
import { useNavigate } from 'react-router-dom'
import { api } from '../api/client'
import type { CircleSummary, InviteDto } from '../api/types'
import { useCircle } from '../state/CircleContext'

export default function OnboardingPage() {
  const navigate = useNavigate()
  const { refresh } = useCircle()
  const [step, setStep] = useState(1)
  const [circleId, setCircleId] = useState<number | null>(null)
  const [error, setError] = useState<string | null>(null)

  const [circleName, setCircleName] = useState('')
  const [patientName, setPatientName] = useState('')
  const [birthYear, setBirthYear] = useState('')
  const [medName, setMedName] = useState('')
  const [dosage, setDosage] = useState('')
  const [stock, setStock] = useState('0')
  const [times, setTimes] = useState<string[]>(['08:00'])
  const [inviteLink, setInviteLink] = useState<string | null>(null)
  const [copied, setCopied] = useState(false)

  const run = async (action: () => Promise<void>) => {
    setError(null)
    try {
      await action()
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Unbekannter Fehler')
    }
  }

  const createCircle = (event: FormEvent) => {
    event.preventDefault()
    run(async () => {
      const circle = await api.post<CircleSummary>('/circles', { name: circleName })
      setCircleId(circle.id)
      setStep(2)
    })
  }

  const createPatient = (event: FormEvent) => {
    event.preventDefault()
    run(async () => {
      await api.put(`/circles/${circleId}/patient`, {
        name: patientName,
        birthYear: birthYear ? Number(birthYear) : null,
        note: null,
      })
      setStep(3)
    })
  }

  const allDays = ['MONDAY', 'TUESDAY', 'WEDNESDAY', 'THURSDAY', 'FRIDAY', 'SATURDAY', 'SUNDAY']

  const createMedication = (event: FormEvent) => {
    event.preventDefault()
    run(async () => {
      await api.post(`/circles/${circleId}/medications`, {
        name: medName,
        dosage,
        stockCount: Number(stock),
        schedules: times.map((time) => ({ timeOfDay: time, daysOfWeek: allDays })),
      })
      await showInvite()
    })
  }

  const showInvite = async () => {
    const dto = await api.post<InviteDto>(`/circles/${circleId}/invite`)
    setInviteLink(`${window.location.origin}/join/${dto.inviteToken}`)
    setStep(4)
  }

  const finish = () => {
    run(async () => {
      await refresh()
      navigate('/', { replace: true })
    })
  }

  return (
    <div className="app">
      <h1>Willkommen bei MediMinder</h1>
      {step < 4 && <p className="muted">Schritt {step} von 3</p>}
      {error && <p className="error">{error}</p>}

      {step === 1 && (
        <form onSubmit={createCircle} className="card stack">
          <h2>Pflegekreis anlegen</h2>
          <label>
            Name des Pflegekreises
            <input
              value={circleName}
              onChange={(e) => setCircleName(e.target.value)}
              placeholder="z. B. Familie Rode"
              required
            />
          </label>
          <button className="btn btn-primary">Weiter</button>
        </form>
      )}

      {step === 2 && (
        <form onSubmit={createPatient} className="card stack">
          <h2>Wer wird gepflegt?</h2>
          <label>
            Name
            <input value={patientName} onChange={(e) => setPatientName(e.target.value)} required />
          </label>
          <label>
            Geburtsjahr (optional)
            <input
              type="number"
              min={1900}
              max={2100}
              value={birthYear}
              onChange={(e) => setBirthYear(e.target.value)}
            />
          </label>
          <button className="btn btn-primary">Weiter</button>
        </form>
      )}

      {step === 3 && (
        <form onSubmit={createMedication} className="card stack">
          <h2>Erstes Medikament</h2>
          <label>
            Name
            <input value={medName} onChange={(e) => setMedName(e.target.value)} placeholder="z. B. Ramipril" required />
          </label>
          <label>
            Dosierung
            <input value={dosage} onChange={(e) => setDosage(e.target.value)} placeholder="z. B. 5 mg" required />
          </label>
          <label>
            Bestand (Stück)
            <input type="number" min={0} value={stock} onChange={(e) => setStock(e.target.value)} required />
          </label>
          <fieldset className="stack">
            <legend>Einnahmezeiten</legend>
            {times.map((time, index) => (
              <div className="row" key={index}>
                <input
                  type="time"
                  value={time}
                  onChange={(e) => setTimes(times.map((t, i) => (i === index ? e.target.value : t)))}
                  required
                />
                {times.length > 1 && (
                  <button
                    type="button"
                    className="btn btn-ghost"
                    onClick={() => setTimes(times.filter((_, i) => i !== index))}
                  >
                    Entfernen
                  </button>
                )}
              </div>
            ))}
            <button type="button" className="btn btn-ghost" onClick={() => setTimes([...times, '12:00'])}>
              + Zeit hinzufügen
            </button>
          </fieldset>
          <button className="btn btn-primary">Weiter</button>
          <button type="button" className="btn btn-link" onClick={() => run(showInvite)}>
            Später hinzufügen
          </button>
        </form>
      )}

      {step === 4 && (
        <div className="card stack">
          <h2>Familie einladen</h2>
          <p className="muted">Mit diesem Link können weitere Mitglieder beitreten:</p>
          <code className="invite-link">{inviteLink}</code>
          <button
            className="btn btn-primary"
            onClick={() => {
              navigator.clipboard.writeText(inviteLink ?? '')
              setCopied(true)
            }}
          >
            {copied ? 'Kopiert ✓' : 'Link kopieren'}
          </button>
          <button className="btn btn-ghost" onClick={finish}>
            Zur Tagesansicht
          </button>
        </div>
      )}
    </div>
  )
}
