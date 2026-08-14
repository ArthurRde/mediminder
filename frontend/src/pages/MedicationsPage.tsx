import { useCallback, useEffect, useState, type FormEvent } from 'react'
import { api } from '../api/client'
import type { Medication } from '../api/types'
import { fmtTime } from '../format'
import { useCircle } from '../state/CircleContext'

const ALL_DAYS = ['MONDAY', 'TUESDAY', 'WEDNESDAY', 'THURSDAY', 'FRIDAY', 'SATURDAY', 'SUNDAY']

interface TimeEntry {
  id: number | null
  time: string
  daysOfWeek: string[]
}

export default function MedicationsPage() {
  const { circle, isAdmin } = useCircle()
  const [medications, setMedications] = useState<Medication[]>([])
  const [editing, setEditing] = useState<Medication | 'new' | null>(null)
  const [error, setError] = useState<string | null>(null)

  const load = useCallback(async () => {
    setMedications(await api.get<Medication[]>(`/circles/${circle!.id}/medications`))
  }, [circle])

  useEffect(() => {
    load().catch(() => undefined)
  }, [load])

  const save = async (medication: Medication | 'new', payload: unknown) => {
    setError(null)
    try {
      if (medication === 'new') {
        await api.post(`/circles/${circle!.id}/medications`, payload)
      } else {
        await api.put(`/circles/${circle!.id}/medications/${medication.id}`, payload)
      }
      setEditing(null)
      await load()
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Speichern fehlgeschlagen')
    }
  }

  const deactivate = async (medication: Medication) => {
    await api.delete(`/circles/${circle!.id}/medications/${medication.id}`)
    await load()
  }

  return (
    <>
      <header className="page-header">
        <h1>Medikationsplan</h1>
        <p className="muted">{circle?.patientName ? `für ${circle.patientName}` : ''}</p>
      </header>

      {error && <p className="error">{error}</p>}

      {medications.map((medication) => (
        <MedicationCard
          key={medication.id}
          medication={medication}
          isAdmin={isAdmin}
          onEdit={() => setEditing(medication)}
          onDeactivate={() => deactivate(medication)}
        />
      ))}
      {medications.length === 0 && <p className="muted">Noch keine Medikamente angelegt.</p>}

      {isAdmin && editing === null && (
        <button className="btn btn-primary wide" onClick={() => setEditing('new')}>
          + Medikament hinzufügen
        </button>
      )}
      {editing !== null && (
        <MedicationForm
          medication={editing === 'new' ? null : editing}
          onSave={(payload) => save(editing, payload)}
          onCancel={() => setEditing(null)}
        />
      )}
    </>
  )
}

function MedicationCard({
  medication,
  isAdmin,
  onEdit,
  onDeactivate,
}: {
  medication: Medication
  isAdmin: boolean
  onEdit: () => void
  onDeactivate: () => void
}) {
  const dosesPerDay = medication.schedules.length
  const daysLeft = dosesPerDay > 0 ? Math.floor(medication.stockCount / dosesPerDay) : null

  return (
    <div className="card stack">
      <div className="row">
        <div className="grow">
          <strong>{medication.name}</strong> <span className="muted">{medication.dosage}</span>
        </div>
        {isAdmin && (
          <>
            <button className="btn btn-ghost" onClick={onEdit}>
              Bearbeiten
            </button>
            <button className="btn btn-ghost danger" onClick={onDeactivate}>
              Deaktivieren
            </button>
          </>
        )}
      </div>
      <div className="chips">
        {medication.schedules.map((schedule) => (
          <span className="chip" key={schedule.id}>
            {fmtTime(schedule.timeOfDay)}
          </span>
        ))}
      </div>
      <div>
        <span className="muted small">
          Bestand: {medication.stockCount}
          {daysLeft !== null && ` · reicht ca. ${daysLeft} Tage`}
        </span>
        {daysLeft !== null && (
          <div className="stock-bar">
            <div
              className={`stock-bar-fill ${daysLeft <= 3 ? 'low' : ''}`}
              style={{ width: `${Math.min(100, (daysLeft / 14) * 100)}%` }}
            />
          </div>
        )}
      </div>
    </div>
  )
}

function MedicationForm({
  medication,
  onSave,
  onCancel,
}: {
  medication: Medication | null
  onSave: (payload: unknown) => void
  onCancel: () => void
}) {
  const [name, setName] = useState(medication?.name ?? '')
  const [dosage, setDosage] = useState(medication?.dosage ?? '')
  const [stock, setStock] = useState(String(medication?.stockCount ?? 0))
  const [times, setTimes] = useState<TimeEntry[]>(
    medication?.schedules.map((s) => ({ id: s.id, time: fmtTime(s.timeOfDay), daysOfWeek: s.daysOfWeek })) ?? [
      { id: null, time: '08:00', daysOfWeek: ALL_DAYS },
    ],
  )

  const submit = (event: FormEvent) => {
    event.preventDefault()
    onSave({
      name,
      dosage,
      stockCount: Number(stock),
      schedules: times.map((entry) => ({
        id: entry.id,
        timeOfDay: entry.time,
        daysOfWeek: entry.daysOfWeek,
      })),
    })
  }

  return (
    <form className="card stack" onSubmit={submit}>
      <h2>{medication ? 'Medikament bearbeiten' : 'Neues Medikament'}</h2>
      <label>
        Name
        <input value={name} onChange={(e) => setName(e.target.value)} required />
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
        {times.map((entry, index) => (
          <div className="row" key={index}>
            <input
              type="time"
              value={entry.time}
              onChange={(e) =>
                setTimes(times.map((t, i) => (i === index ? { ...t, time: e.target.value } : t)))
              }
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
        <button
          type="button"
          className="btn btn-ghost"
          onClick={() => setTimes([...times, { id: null, time: '12:00', daysOfWeek: ALL_DAYS }])}
        >
          + Zeit hinzufügen
        </button>
      </fieldset>
      <div className="row">
        <button className="btn btn-primary grow">Speichern</button>
        <button type="button" className="btn btn-ghost" onClick={onCancel}>
          Abbrechen
        </button>
      </div>
    </form>
  )
}
