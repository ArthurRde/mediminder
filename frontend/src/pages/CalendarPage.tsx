import { useCallback, useEffect, useState, type FormEvent } from 'react'
import { api, ApiError } from '../api/client'
import type { Appointment, Task } from '../api/types'
import { fmtDate, fmtDateTime } from '../format'
import { useCircle } from '../state/CircleContext'
import Avatar from '../components/Avatar'

export default function CalendarPage() {
  const { circle } = useCircle()
  const [appointments, setAppointments] = useState<Appointment[]>([])
  const [tasks, setTasks] = useState<Task[]>([])
  const [notice, setNotice] = useState<string | null>(null)
  const [showAppointmentForm, setShowAppointmentForm] = useState(false)
  const [showTaskForm, setShowTaskForm] = useState(false)

  const load = useCallback(async () => {
    const [nextAppointments, allTasks] = await Promise.all([
      api.get<Appointment[]>(`/circles/${circle!.id}/appointments`),
      api.get<Task[]>(`/circles/${circle!.id}/tasks`),
    ])
    setAppointments(nextAppointments)
    setTasks(allTasks.filter((task) => task.status === 'OPEN'))
  }, [circle])

  useEffect(() => {
    load().catch(() => undefined)
  }, [load])

  const handle = async (action: () => Promise<unknown>) => {
    try {
      await action()
    } catch (err) {
      if (err instanceof ApiError && err.status === 409) {
        setNotice(err.message)
      } else if (err instanceof Error) {
        setNotice(err.message)
      }
    }
    await load().catch(() => undefined)
  }

  return (
    <>
      <header className="page-header">
        <h1>Kalender & Aufgaben</h1>
      </header>

      {notice && (
        <div className="notice" role="status" onClick={() => setNotice(null)}>
          {notice} <span className="muted">(Tippen zum Schließen)</span>
        </div>
      )}

      <h2>Kommende Termine</h2>
      {appointments.map((appointment) => (
        <div className="card row" key={appointment.id}>
          <div className="grow">
            <strong>{appointment.title}</strong>
            <span className="muted small">
              {fmtDateTime(appointment.dateTime)}
              {appointment.location ? ` · ${appointment.location}` : ''}
            </span>
          </div>
          {appointment.assignedToName ? (
            <Avatar name={appointment.assignedToName} />
          ) : (
            <button
              className="btn btn-outline"
              onClick={() => handle(() => api.post(`/appointments/${appointment.id}/claim`))}
            >
              Übernehmen
            </button>
          )}
        </div>
      ))}
      {appointments.length === 0 && <p className="muted">Keine kommenden Termine.</p>}
      {showAppointmentForm ? (
        <AppointmentForm
          onSave={(payload) =>
            handle(async () => {
              await api.post(`/circles/${circle!.id}/appointments`, payload)
              setShowAppointmentForm(false)
            })
          }
          onCancel={() => setShowAppointmentForm(false)}
        />
      ) : (
        <button className="btn btn-ghost" onClick={() => setShowAppointmentForm(true)}>
          + Termin anlegen
        </button>
      )}

      <h2>Offene Aufgaben</h2>
      {tasks.map((task) => (
        <div className="card row" key={task.id}>
          <div className="grow">
            <strong>{task.title}</strong>
            <span className="muted small">fällig {fmtDate(task.dueDate)}</span>
          </div>
          {task.assignedToName ? (
            <Avatar name={task.assignedToName} />
          ) : (
            <button
              className="btn btn-outline"
              onClick={() => handle(() => api.post(`/tasks/${task.id}/claim`))}
            >
              Übernehmen
            </button>
          )}
          <button
            className="check-btn"
            aria-label="Aufgabe abhaken"
            onClick={() => handle(() => api.post(`/tasks/${task.id}/done`))}
          >
            ✓
          </button>
        </div>
      ))}
      {tasks.length === 0 && <p className="muted">Keine offenen Aufgaben.</p>}
      {showTaskForm ? (
        <TaskForm
          onSave={(payload) =>
            handle(async () => {
              await api.post(`/circles/${circle!.id}/tasks`, payload)
              setShowTaskForm(false)
            })
          }
          onCancel={() => setShowTaskForm(false)}
        />
      ) : (
        <button className="btn btn-ghost" onClick={() => setShowTaskForm(true)}>
          + Aufgabe anlegen
        </button>
      )}
    </>
  )
}

function AppointmentForm({
  onSave,
  onCancel,
}: {
  onSave: (payload: unknown) => void
  onCancel: () => void
}) {
  const [title, setTitle] = useState('')
  const [dateTime, setDateTime] = useState('')
  const [location, setLocation] = useState('')

  const submit = (event: FormEvent) => {
    event.preventDefault()
    onSave({ title, dateTime, location: location || null })
  }

  return (
    <form className="card stack" onSubmit={submit}>
      <label>
        Titel
        <input value={title} onChange={(e) => setTitle(e.target.value)} required />
      </label>
      <label>
        Datum & Uhrzeit
        <input type="datetime-local" value={dateTime} onChange={(e) => setDateTime(e.target.value)} required />
      </label>
      <label>
        Ort (optional)
        <input value={location} onChange={(e) => setLocation(e.target.value)} />
      </label>
      <div className="row">
        <button className="btn btn-primary grow">Speichern</button>
        <button type="button" className="btn btn-ghost" onClick={onCancel}>
          Abbrechen
        </button>
      </div>
    </form>
  )
}

function TaskForm({ onSave, onCancel }: { onSave: (payload: unknown) => void; onCancel: () => void }) {
  const [title, setTitle] = useState('')
  const [dueDate, setDueDate] = useState('')

  const submit = (event: FormEvent) => {
    event.preventDefault()
    onSave({ title, dueDate })
  }

  return (
    <form className="card stack" onSubmit={submit}>
      <label>
        Titel
        <input value={title} onChange={(e) => setTitle(e.target.value)} required />
      </label>
      <label>
        Fällig am
        <input type="date" value={dueDate} onChange={(e) => setDueDate(e.target.value)} required />
      </label>
      <div className="row">
        <button className="btn btn-primary grow">Speichern</button>
        <button type="button" className="btn btn-ghost" onClick={onCancel}>
          Abbrechen
        </button>
      </div>
    </form>
  )
}
